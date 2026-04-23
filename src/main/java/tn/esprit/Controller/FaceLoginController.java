package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.entities.User;
import tn.esprit.face.FaceRecognitionService;
import tn.esprit.face.WebcamService;
import tn.esprit.security.AuthSession;
import tn.esprit.services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FaceLoginController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(FaceLoginController.class);

    private static final int CONFIRM_FRAMES  = 8;
    private static final int AUTO_RESET_SECS = 4;

    @FXML private ImageView         cameraPreview;
    @FXML private Label             statusLabel;
    @FXML private Label             instructionLabel;
    @FXML private ProgressBar       confirmBar;
    @FXML private ProgressIndicator spinner;
    @FXML private Button            retryBtn;
    @FXML private Button            passwordBtn;

    private WebcamService          webcam;
    private FaceRecognitionService faceService;
    private UserService            userService;

    private final AtomicInteger confirmCount    = new AtomicInteger(0);
    private volatile String     lastEmail       = null;
    private volatile boolean    loginInProgress = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try { userService = new UserService(); }
        catch (Exception e) { setStatus("Database unavailable.", "status-error"); return; }

        faceService = new FaceRecognitionService();

        spinner.setVisible(false);
        retryBtn.setVisible(false);
        confirmBar.setProgress(0);
        setStatus("Position your face in the camera", "status-muted");
        setInstruction("Make sure the room is well lit");

        webcam = new WebcamService(cameraPreview);
        webcam.onFrame(this::processFrame);
        webcam.start();
    }

    // ── Per-frame logic ───────────────────────────────────────────────────────

    private void processFrame(Mat bgr) {
        if (loginInProgress) return;

        // ── Lighting check — show warning before anything else ────────────────
        if (faceService.isTooDark(bgr)) {
            Platform.runLater(() -> {
                confirmBar.setProgress(0);
                setStatus("Too dark — turn on a light and face the camera", "status-warning");
                setInstruction("Lighting is the most important factor for face recognition");
            });
            resetCount();
            return;
        }

        Rect[] faces = faceService.detectFaces(bgr);
        faceService.drawFaceBoxes(bgr, faces);

        if (faces.length == 0) {
            resetCount();
            Platform.runLater(() -> {
                confirmBar.setProgress(0);
                setStatus("No face detected — look straight at the camera", "status-muted");
                setInstruction("Make sure your face is fully visible and well lit");
            });
            return;
        }

        Rect   best  = largest(faces);
        Mat    roi   = new Mat(bgr, best);
        String email = faceService.recognizeEmail(roi);

        if (email == null) {
            resetCount();
            Platform.runLater(() -> {
                confirmBar.setProgress(0);
                setStatus("Face not recognised — try better lighting or re-register", "status-warning");
                setInstruction("If this keeps failing, register your face again from the admin panel");
            });
            return;
        }

        if (email.equals(lastEmail)) {
            int n = confirmCount.incrementAndGet();
            double progress = (double) n / CONFIRM_FRAMES;
            Platform.runLater(() -> {
                confirmBar.setProgress(progress);
                setStatus("Recognising " + email + "…", "status-info");
            });
            if (n >= CONFIRM_FRAMES) triggerLogin(email);
        } else {
            lastEmail = email;
            confirmCount.set(1);
        }
    }

    // ── Login trigger ─────────────────────────────────────────────────────────

    private void triggerLogin(String email) {
        loginInProgress = true;
        webcam.stop();

        Platform.runLater(() -> {
            spinner.setVisible(true);
            confirmBar.setProgress(1.0);
            setStatus("Checking account…", "status-info");
        });

        CompletableFuture.runAsync(() -> {
            try {
                User user = userService.findByEmail(email);
                if (user == null) { fail("Face recognised but no account found for " + email); return; }
                if (!user.isActiveAccount()) { fail("Account is inactive or blocked"); return; }

                Platform.runLater(() -> {
                    AuthSession.setCurrentUser(user);
                    spinner.setVisible(false);
                    setStatus("Welcome, " + user.getDisplayName() + "!", "status-success");
                    confirmBar.setProgress(1.0);
                });
                Thread.sleep(700);
                Platform.runLater(this::openDashboard);

            } catch (SQLException e) {
                fail("Database error — try password login");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML private void onRetry() {
        loginInProgress = false; lastEmail = null; confirmCount.set(0);
        retryBtn.setVisible(false); spinner.setVisible(false); confirmBar.setProgress(0);
        setStatus("Position your face in the camera", "status-muted");
        setInstruction("Make sure the room is well lit");
        webcam.start();
    }

    @FXML private void onPasswordLogin() { webcam.stop(); navigateTo("/fxml/login.fxml"); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openDashboard() { webcam.stop(); navigateTo("/fxml/AdminDashboard.fxml"); }

    private void navigateTo(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage  stage = (Stage) cameraPreview.getScene().getWindow();
            stage.setScene(new Scene(root)); stage.show();
        } catch (Exception e) { setStatus("Navigation error: " + e.getMessage(), "status-error"); }
    }

    private void fail(String message) {
        loginInProgress = false;
        Platform.runLater(() -> {
            spinner.setVisible(false);
            setStatus(message, "status-error");
            retryBtn.setVisible(true);
        });
        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        s.schedule(() -> Platform.runLater(this::onRetry), AUTO_RESET_SECS, TimeUnit.SECONDS);
        s.shutdown();
    }

    private void resetCount() { lastEmail = null; confirmCount.set(0); }

    private void setStatus(String msg, String styleClass) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll(
                "status-muted","status-info","status-success","status-warning","status-error");
        statusLabel.getStyleClass().add(styleClass);
    }

    private void setInstruction(String text) { instructionLabel.setText(text); }

    private Rect largest(Rect[] rects) {
        Rect best = rects[0];
        for (Rect r : rects) if (r.width() * r.height() > best.width() * best.height()) best = r;
        return best;
    }

    public void shutdown() { webcam.stop(); }
}
