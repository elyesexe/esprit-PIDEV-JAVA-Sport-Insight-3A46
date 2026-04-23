package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.entities.User;
import tn.esprit.face.FaceRecognitionService;
import tn.esprit.face.WebcamService;
import tn.esprit.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Controller for face_register.fxml
 *
 * Opened from {@link AdminUserModerationController} when the admin clicks
 * "Register Face" for a selected user.
 *
 * Call {@link #setTargetUser(User)} before showing the stage.
 *
 * On success it also sets {@code user.faceRegistered = true} and calls
 * {@link UserService#update(User)} so the flag is persisted.
 * (Requires adding the {@code face_registered} column — see below.)
 */
public class FaceRegisterController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(FaceRegisterController.class);

    private static final int  REQUIRED_SAMPLES   = 20;
    private static final long CAPTURE_INTERVAL_MS = 350;

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ImageView         cameraPreview;
    @FXML private Label             statusLabel;
    @FXML private Label             sampleCountLabel;
    @FXML private Label             instructionLabel;
    @FXML private ProgressBar       sampleBar;
    @FXML private ProgressIndicator trainingSpinner;
    @FXML private Button            startBtn;
    @FXML private Button            cancelBtn;

    // ── Services ──────────────────────────────────────────────────────────────
    private WebcamService          webcam;
    private FaceRecognitionService faceService;
    private UserService            userService;
    private User                   targetUser;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<Mat> samples   = new ArrayList<>();
    private volatile boolean capturing = false;
    private long lastCapture           = 0;

    // ── Called before show() ──────────────────────────────────────────────────

    /**
     * Inject the user whose face is being registered.
     * Must be called after loader.load() and before showAndWait().
     * This method enables the Start button and shows the user's name in the UI.
     */
    public void setTargetUser(User user) {
        this.targetUser = user;

        // Update UI on FX thread — setTargetUser is called from the FX thread
        // (inside handleRegisterFace) so Platform.runLater is not needed here.
        if (user == null) {
            startBtn.setDisable(true);
            setStatus("No user selected — close and try again", "status-error");
        } else {
            startBtn.setDisable(false);
            setStatus("Ready — registering face for: " + user.getDisplayName(), "status-muted");
            setInstruction("Press Start, then slowly move your head");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try { userService = new UserService(); }
        catch (Exception e) { log.error("UserService init failed", e); }

        faceService = new FaceRecognitionService();
        trainingSpinner.setVisible(false);
        sampleBar.setProgress(0);
        sampleCountLabel.setText("0 / " + REQUIRED_SAMPLES);

        // Start button stays DISABLED until setTargetUser() is called with a valid user.
        // This prevents clicking Start before the controller knows who to register.
        startBtn.setDisable(true);
        setStatus("Waiting for user selection…", "status-muted");
        setInstruction("This window must be opened from the user table");

        webcam = new WebcamService(cameraPreview);
        webcam.onFrame(this::processFrame);
        webcam.start();
    }

    // ── Per-frame logic ───────────────────────────────────────────────────────

    private void processFrame(Mat bgr) {
        Rect[] faces = faceService.detectFaces(bgr);
        faceService.drawFaceBoxes(bgr, faces);

        if (capturing && faces.length > 0) {
            long now = System.currentTimeMillis();
            if (now - lastCapture >= CAPTURE_INTERVAL_MS) {
                lastCapture = now;
                Rect best  = largest(faces);
                Mat  clone = new Mat(bgr, best).clone();
                samples.add(clone);

                int count = samples.size();
                Platform.runLater(() -> {
                    sampleCountLabel.setText(count + " / " + REQUIRED_SAMPLES);
                    sampleBar.setProgress((double) count / REQUIRED_SAMPLES);
                    setInstruction(instructionForCount(count));
                });

                if (count >= REQUIRED_SAMPLES) {
                    capturing = false;
                    Platform.runLater(this::beginTraining);
                }
            }
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void onStart() {
        if (targetUser == null) {
            setStatus("No user selected — close and try again", "status-error");
            return;
        }
        samples.clear();
        capturing = true;
        startBtn.setDisable(true);
        setStatus("Capturing… slowly move your head", "status-info");
        setInstruction("Look straight at the camera");
    }

    @FXML
    private void onCancel() {
        capturing = false;
        webcam.stop();
        closeWindow();
    }

    // ── Training ──────────────────────────────────────────────────────────────

    private void beginTraining() {
        webcam.stop();
        trainingSpinner.setVisible(true);
        startBtn.setDisable(true);
        setStatus("Training face model…", "status-info");

        Thread t = new Thread(() -> {
            boolean ok = faceService.registerFace(
                    targetUser.getId(),
                    targetUser.getEmail(),
                    samples);

            Platform.runLater(() -> {
                trainingSpinner.setVisible(false);
                if (ok) {
                    // Persist face_registered flag if your schema has it
                    if (userService != null) {
                        try {
                            // Uncomment after adding face_registered column:
                            // targetUser.setFaceRegistered(true);
                            // userService.update(targetUser);
                        } catch (Exception ignored) {}
                    }
                    sampleBar.setProgress(1.0);
                    setStatus("Face registered! " + targetUser.getDisplayName()
                            + " can now log in with their face.", "status-success");
                    scheduleClose(2200);
                } else {
                    setStatus("Registration failed — not enough clear face samples. Try again.", "status-error");
                    startBtn.setDisable(false);
                    samples.clear();
                    webcam.start();
                }
            });
        }, "face-training");
        t.setDaemon(true);
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void scheduleClose(long ms) {
        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        s.schedule(() -> Platform.runLater(this::closeWindow), ms, TimeUnit.MILLISECONDS);
        s.shutdown();
    }

    private void closeWindow() {
        cameraPreview.getScene().getWindow().hide();
    }

    private void setStatus(String msg, String styleClass) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll(
                "status-muted","status-info","status-success","status-warning","status-error");
        statusLabel.getStyleClass().add(styleClass);
    }

    private void setInstruction(String text) { instructionLabel.setText(text); }

    private String instructionForCount(int n) {
        if (n < 5)       return "Look straight at the camera";
        if (n < 10)      return "Turn slightly to the left";
        if (n < 15)      return "Turn slightly to the right";
        return "Look up and down — almost done!";
    }

    private Rect largest(Rect[] rects) {
        Rect best = rects[0];
        for (Rect r : rects) if (r.width() * r.height() > best.width() * best.height()) best = r;
        return best;
    }
}