package tn.esprit.face;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_imgproc.CLAHE;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * LBPH face detection + recognition with DB integration and low-light handling.
 *
 * Key improvements over previous version:
 *  - CLAHE preprocessing (handles dark/uneven lighting far better than plain equalizeHist)
 *  - isTooD ark() helper so the login screen can warn the user
 *  - persistToDatabase() now correctly sets face_registered = 1
 *  - CONFIDENCE_THRESHOLD raised to 90 (more lenient — needed for real-world variation)
 */
public class FaceRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(FaceRecognitionService.class);

    public  static final String FACE_DATA_DIR        = "face_data/";
    private static final String MODEL_FILE           = FACE_DATA_DIR + "model.yml";

    /**
     * Higher = more lenient matching.
     * LBPH confidence is a distance: 0 = perfect match, higher = worse.
     * Values above this threshold are rejected as "Unknown".
     * Start at 90 — lower toward 70 only if strangers are being accepted.
     */
    public  static final double CONFIDENCE_THRESHOLD = 90.0;

    private static final Size   FACE_SIZE            = new Size(100, 100);

    /** Pixel brightness below this → warn user about dark room (0–255 scale). */
    public  static final double MIN_BRIGHTNESS       = 60.0;

    private final CascadeClassifier    detector;
    private final LBPHFaceRecognizer   recognizer;
    private final Map<Integer, String> labelToEmail = new HashMap<>();
    private boolean modelTrained = false;
    private UserService userService;

    // ── Constructor ───────────────────────────────────────────────────────────

    public FaceRecognitionService() {
        new File(FACE_DATA_DIR).mkdirs();
        detector   = new CascadeClassifier(extractCascade());
        recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, CONFIDENCE_THRESHOLD);
        try { userService = new UserService(); }
        catch (Exception e) { log.warn("UserService unavailable: {}", e.getMessage()); }
        loadModelAndLabels();
    }

    // ── Lighting check ────────────────────────────────────────────────────────

    /**
     * Returns true if the frame is too dark to reliably detect or recognise.
     * Call this every frame and show a warning in the UI before trying detection.
     */
    public boolean isTooDark(Mat bgrFrame) {
        Mat gray = toGray(bgrFrame);
        Scalar mean = mean(gray);
        double brightness = mean.get(0);
        log.debug("Frame brightness: {:.1f}", brightness);
        return brightness < MIN_BRIGHTNESS;
    }

    // ── Detection ─────────────────────────────────────────────────────────────

    public Rect[] detectFaces(Mat bgrFrame) {
        Mat gray = clahePreprocess(bgrFrame);   // CLAHE instead of plain equalizeHist
        RectVector rv = new RectVector();
        detector.detectMultiScale(gray, rv, 1.1, 5, 0, new Size(60, 60), new Size(0, 0));
        Rect[] out = new Rect[(int) rv.size()];
        for (int i = 0; i < out.length; i++) out[i] = rv.get(i);
        return out;
    }

    // ── Recognition ───────────────────────────────────────────────────────────

    public String recognizeEmail(Mat bgrFaceRoi) {
        if (!modelTrained) { log.debug("No model — register a face first"); return null; }
        Mat proc = clahePreprocess(bgrFaceRoi);   // CLAHE for recognition too
        Mat resized = new Mat();
        resize(proc, resized, FACE_SIZE);
        int[]    label = {-1};
        double[] conf  = {Double.MAX_VALUE};
        try { recognizer.predict(resized, label, conf); }
        catch (Exception e) { log.debug("predict threw: {}", e.getMessage()); return null; }
        log.debug("predict → label={} conf={}", label[0], String.format("%.1f", conf[0]));
        if (conf[0] <= CONFIDENCE_THRESHOLD && labelToEmail.containsKey(label[0]))
            return labelToEmail.get(label[0]);
        return null;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    public boolean registerFace(int userId, String email, List<Mat> samples) {
        String userDir = FACE_DATA_DIR + userId + "/";
        new File(userDir).mkdirs();

        int saved = 0;
        for (int i = 0; i < samples.size(); i++) {
            Mat face = extractLargestFace(samples.get(i));
            if (face == null || face.empty()) continue;
            if (imwrite(userDir + "sample_" + i + ".jpg", face)) saved++;
        }

        if (saved < 5) {
            log.warn("Only {} usable samples for userId={} — need >=5. Check lighting!", saved, userId);
            return false;
        }

        log.info("Saved {} samples for userId={} ({})", saved, userId, email);
        labelToEmail.put(userId, email);

        try {
            retrainModel();
            persistToDatabase(userId);   // sets face_registered = 1
            return true;
        } catch (IOException e) {
            log.error("Model retrain failed for userId={}", userId, e);
            return false;
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    public void drawFaceBoxes(Mat frame, Rect[] faces) {
        for (Rect r : faces) {
            String who   = recognizeEmail(new Mat(frame, r));
            Scalar color = who != null ? new Scalar(0, 200, 0, 255) : new Scalar(0, 0, 200, 255);
            rectangle(frame, r, color, 2, LINE_8, 0);
            putText(frame, who != null ? who : "Unknown",
                    new Point(r.x(), Math.max(r.y() - 8, 14)),
                    FONT_HERSHEY_SIMPLEX, 0.55, color, 2, LINE_8, false);
        }
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    public boolean deleteFace(int userId) {
        Path dir = Paths.get(FACE_DATA_DIR + userId);
        try {
            if (Files.exists(dir))
                Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            labelToEmail.remove(userId);
            retrainModel();
            // Reset face_registered = 0 in DB
            if (userService != null) {
                try {
                    User u = userService.getById(userId);
                    if (u != null) {
                        u.setFaceRegistered(false);
                        u.setUpdatedAt(java.time.LocalDateTime.now());
                        userService.update(u);
                    }
                } catch (SQLException e) { log.warn("Could not reset face_registered for userId={}", userId); }
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to delete face data for userId={}", userId, e);
            return false;
        }
    }

    public boolean isFaceRegistered(int userId) {
        File dir = new File(FACE_DATA_DIR + userId);
        File[] jpgs = dir.isDirectory() ? dir.listFiles((d, n) -> n.endsWith(".jpg")) : null;
        return jpgs != null && jpgs.length > 0;
    }

    public void refreshLabels(Map<Integer, String> userIdToEmail) {
        labelToEmail.putAll(userIdToEmail);
    }

    // ── DB persistence ────────────────────────────────────────────────────────

    private void persistToDatabase(int userId) {
        if (userService == null) {
            log.warn("UserService null — skipping DB update for userId={}", userId);
            return;
        }
        try {
            User user = userService.getById(userId);
            if (user == null) { log.warn("User {} not found in DB", userId); return; }

            user.setFaceRegistered(true);                        // ← was commented out before
            user.setUpdatedAt(java.time.LocalDateTime.now());
            userService.update(user);
            log.info("DB: face_registered=1 saved for userId={}", userId);

        } catch (SQLException e) {
            log.error("DB update failed for userId={} after registration", userId, e);
        }
    }

    // ── Model training ────────────────────────────────────────────────────────

    private void retrainModel() throws IOException {
        List<Mat>     imgs   = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        File[] dirs = new File(FACE_DATA_DIR).listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) { modelTrained = false; return; }

        for (File dir : dirs) {
            int uid;
            try { uid = Integer.parseInt(dir.getName()); } catch (NumberFormatException e) { continue; }
            File[] jpgs = dir.listFiles((d, n) -> n.endsWith(".jpg"));
            if (jpgs == null) continue;
            for (File jpg : jpgs) {
                Mat img = imread(jpg.getAbsolutePath(), IMREAD_GRAYSCALE);
                if (img != null && !img.empty()) { imgs.add(img); labels.add(uid); }
            }
        }

        if (imgs.isEmpty()) { modelTrained = false; return; }

        MatVector mv  = new MatVector(imgs.toArray(new Mat[0]));
        Mat       lv  = new Mat(labels.size(), 1, CV_32SC1);
        int[]     arr = labels.stream().mapToInt(i -> i).toArray();
        lv.data().put(toBytes(arr), 0, arr.length * 4);

        recognizer.train(mv, lv);
        recognizer.save(MODEL_FILE);
        modelTrained = true;
        log.info("Model retrained: {} images across {} user(s) → {}", imgs.size(), dirs.length, MODEL_FILE);
    }

    private void loadModelAndLabels() {
        File[] dirs = new File(FACE_DATA_DIR).listFiles(File::isDirectory);
        if (dirs != null)
            for (File d : dirs) {
                try { labelToEmail.put(Integer.parseInt(d.getName()), "user-" + d.getName()); }
                catch (NumberFormatException ignored) {}
            }

        File model = new File(MODEL_FILE);
        if (model.exists()) {
            try {
                recognizer.read(MODEL_FILE);
                modelTrained = true;
                log.info("LBPH model loaded — {} known user(s)", labelToEmail.size());
            } catch (Exception e) {
                log.warn("Could not load model.yml: {}", e.getMessage());
            }
        } else {
            log.info("No model.yml in {} — register a face first", FACE_DATA_DIR);
        }

        // Sync real emails from DB
        if (userService != null) {
            try {
                userService.getAll().forEach(u -> labelToEmail.put(u.getId(), u.getEmail()));
                log.info("Label map refreshed from DB: {} entries", labelToEmail.size());
            } catch (SQLException e) {
                log.warn("Could not refresh labels from DB: {}", e.getMessage());
            }
        }
    }

    // ── CLAHE preprocessing (low-light + uneven illumination fix) ─────────────

    /**
     * CLAHE (Contrast Limited Adaptive Histogram Equalization) normalises
     * local contrast across the image.  It handles:
     *   - Dark rooms
     *   - Bright spots / shadows on the face
     *   - Uneven lighting from one side
     * Far superior to plain equalizeHist for real-world conditions.
     */
    private Mat clahePreprocess(Mat bgr) {
        Mat gray  = toGray(bgr);
        CLAHE clahe = createCLAHE(2.0, new Size(8, 8));
        Mat result = new Mat();
        clahe.apply(gray, result);
        return result;
    }

    // ── Image helpers ─────────────────────────────────────────────────────────

    private Mat toGray(Mat bgr) {
        Mat g = new Mat(); cvtColor(bgr, g, COLOR_BGR2GRAY); return g;
    }

    private Mat extractLargestFace(Mat frame) {
        Rect[] faces = detectFaces(frame);
        if (faces.length == 0) return null;
        Rect best = faces[0];
        for (Rect r : faces) if (r.width() * r.height() > best.width() * best.height()) best = r;
        Mat roi     = new Mat(frame, best);
        Mat proc    = clahePreprocess(roi);
        Mat resized = new Mat();
        resize(proc, resized, FACE_SIZE);
        return resized;
    }

    private String extractCascade() {
        try (InputStream in = getClass().getClassLoader()
                                        .getResourceAsStream("haarcascade_frontalface_default.xml")) {
            if (in == null) throw new IllegalStateException(
                "haarcascade_frontalface_default.xml not found — add javacv-platform to pom.xml");
            Path tmp = Files.createTempFile("haar_", ".xml");
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp.toString();
        } catch (IOException e) { throw new RuntimeException("Cannot extract Haar cascade", e); }
    }

    private byte[] toBytes(int[] ints) {
        byte[] b = new byte[ints.length * 4];
        for (int i = 0; i < ints.length; i++) {
            b[i*4]   = (byte)(ints[i]       & 0xff);
            b[i*4+1] = (byte)(ints[i] >>  8 & 0xff);
            b[i*4+2] = (byte)(ints[i] >> 16 & 0xff);
            b[i*4+3] = (byte)(ints[i] >> 24 & 0xff);
        }
        return b;
    }
}
