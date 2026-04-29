# Documentation Technique - Reconnaissance Faciale Sport Insight

## Architecture Système

### Composants Principaux

```
┌─────────────────────────────────────────────────────────┐
│                   Application JavaFX                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │ LoginController  │      │ AdminUserMode    │        │
│  │                  │      │ rationController │        │
│  └────────┬─────────┘      └────────┬─────────┘        │
│           │                         │                   │
│      ┌────▼─────────────────────────▼──────┐           │
│      │   FaceRecognitionService (LBPH)     │           │
│      │   - detectFaces()                   │           │
│      │   - recognizeEmail()                │           │
│      │   - registerFace()                  │           │
│      │   - deleteFace()                    │           │
│      └────┬──────────────────────────┬─────┘           │
│           │                          │                  │
│      ┌────▼──────┐            ┌──────▼──────┐         │
│      │ WebcamService        │ OpenCV Core  │         │
│      │ (JavaCV)             │ LBPH Recognizer│       │
│      └───────────┘            └──────────────┘        │
│                                                          │
│      ┌──────────────────────────────────────────┐      │
│      │    Haarcascade (Face Detection)          │      │
│      │    haarcascade_frontalface_default.xml   │      │
│      └──────────────────────────────────────────┘      │
│                                                          │
└─────────────────────────────────────────────────────────┘
         │                                  │
         ▼                                  ▼
    ┌─────────────┐              ┌──────────────────┐
    │ User Service│              │ Face Data Store  │
    │ (JDBC)      │              │ (File System)    │
    └──────┬──────┘              └────────┬─────────┘
           │                             │
           ▼                             ▼
    ┌──────────────────────┐    ┌──────────────────┐
    │  MySQL Database      │    │ face_data/       │
    │  - users table       │    │ - [userID]/      │
    │  - roles table       │    │   - samples/     │
    └──────────────────────┘    │   - model.xml    │
                                 └──────────────────┘
```

## Flux de Données - Enregistrement du Visage

```
User clicks "Register Face"
        │
        ▼
AdminUserModerationController.handleRegisterFace()
        │
        ├─ Load face_register.fxml
        ├─ Create FaceRegisterController
        ├─ Call setTargetUser(selectedUser)
        │
        ▼
FaceRegisterController.initialize()
        │
        ├─ Initialize UserService
        ├─ Initialize FaceRecognitionService
        ├─ Start WebcamService
        │
        ▼
WebcamService continuously:
        │
        ├─ Capture frame from camera
        ├─ Pass to processFrame()
        │
        ▼
FaceRecognitionService.detectFaces()
        │
        ├─ Load cascade classifier
        ├─ Detect faces in frame
        ├─ Draw rectangles on Mat
        │
        ▼
User moves head slowly
        │
        ├─ Largest face ROI extracted
        ├─ Cloned and stored in samples list
        ├─ Update UI: "Samples: N / 20"
        │
        ▼
Once 20 samples collected:
        │
        ├─ Stop webcam
        ├─ Show training spinner
        │
        ▼
FaceRecognitionService.registerFace()
        │
        ├─ Train LBPH model with 20 samples
        ├─ Save model to face_data/[userID]/model.xml
        ├─ Save samples to face_data/[userID]/samples/
        ├─ Persist to database (optional)
        │
        ▼
Update UI: "Face registered successfully!"
        │
        └─ Auto-close modal after 2.2 seconds
```

## Flux de Données - Connexion par Visage

```
User clicks "Face login" on Login page
        │
        ▼
LoginController.onFaceLogin()
        │
        ├─ Load face_login.fxml
        │
        ▼
FaceLoginController.initialize()
        │
        ├─ Initialize UserService
        ├─ Initialize FaceRecognitionService
        ├─ Pre-load all user labels from DB
        ├─ Start WebcamService
        ├─ Set status: "Position your face..."
        │
        ▼
WebcamService continuously (30-60 FPS):
        │
        ├─ Capture frame
        ├─ Pass to processFrame()
        │
        ▼
FaceRecognitionService.detectFaces()
        │
        ├─ Detect faces in current frame
        │
        ▼
If face(s) detected:
        │
        ├─ Extract largest face ROI
        ├─ Call recognizeEmail(roi)
        │
        ▼
FaceRecognitionService.recognizeEmail()
        │
        ├─ Load all trained LBPH models
        ├─ Predict label for ROI
        ├─ Get confidence score
        ├─ Map to user email
        │
        ▼
Email recognized:
        │
        ├─ Compare with lastEmail
        ├─ If same: increment confirmCount
        ├─ If different: reset, set new lastEmail
        ├─ Update UI progress bar: confirmCount / 8
        │
        ▼
Once 8 consecutive matches:
        │
        ├─ Stop webcam
        ├─ Show spinner
        ├─ Call triggerLogin(email)
        │
        ▼
triggerLogin(email):
        │
        ├─ Call UserService.findByEmail(email)
        ├─ Check user.isActiveAccount()
        │
        ├─ If active:
        │  ├─ AuthSession.setCurrentUser(user)
        │  ├─ Show "Welcome, [Name]!"
        │  └─ Navigate to home-view.fxml
        │
        └─ If inactive:
           └─ Show error, allow retry
```

## Structure de Fichiers - Face Data

```
face_data/
├── [userID1]/
│   ├── model.xml              # LBPH model entraîné
│   ├── labels.txt             # Mapping ID → Email
│   └── samples/
│       ├── 0.jpg              # Sample 1
│       ├── 1.jpg              # Sample 2
│       ├── ...
│       └── 19.jpg             # Sample 20
│
├── [userID2]/
│   ├── model.xml
│   ├── labels.txt
│   └── samples/
│       └── ...
│
└── ...
```

## Implémentation du Service de Reconnaissance

### FaceRecognitionService

**Classe Principale:** `tn.esprit.face.FaceRecognitionService`

#### Méthodes Clés:

```java
// Détection
Rect[] detectFaces(Mat bgr)
void drawFaceBoxes(Mat bgr, Rect[] faces)

// Reconnaissance
String recognizeEmail(Mat faceRoi)
double getConfidence()

// Enregistrement
boolean registerFace(int userId, String email, List<Mat> samples)

// Suppression
boolean deleteFace(int userId)

// Utilitaires
void refreshLabels(Map<Integer, String> idToEmailMap)
boolean isFaceRegistered(int userId)
```

#### Paramètres LBPH:

```java
LBPH recognizer = LBPHFaceRecognizer.create(
    radius = 1,           // LBP pattern radius
    neighbors = 8,        // Number of neighbors
    gridX = 8,           // Histograms in X direction
    gridY = 8,           // Histograms in Y direction
    threshold = 70.0     // Confidence threshold
);
```

### WebcamService

**Classe:** `tn.esprit.face.WebcamService`

```java
public class WebcamService {
    // Capture video stream from default camera
    // Convert to JavaFX Image in real-time
    // Callback function for each frame
    
    void start()                        // Start capturing
    void stop()                         // Stop capturing
    void onFrame(Consumer<Mat> handler) // Register frame handler
}
```

### FaceRegisterController & FaceLoginController

Voir: `AdminUserModerationController` pour l'intégration

## Configuration Maven

### Dépendances Requises (pom.xml)

```xml
<!-- JavaCV (OpenCV binding for Java) -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.5.10</version>
</dependency>

<!-- OpenCV Native Libraries -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>opencv-platform</artifactId>
    <version>4.9.0-1.5.10</version>
</dependency>
```

### Build Configuration

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <includes>
                <include>**/*</include>
            </includes>
        </resource>
    </resources>
</build>
```

## Fichiers Critiques

| Chemin | Description |
|--------|-------------|
| `AdminUserModerationController.java` | Gestionnaire admin pour les utilisateurs + reconnaissance faciale |
| `FaceRecognitionService.java` | Service de reconnaissance basé OpenCV LBPH |
| `FaceRegisterController.java` | Contrôleur pour enregistrer un visage |
| `FaceLoginController.java` | Contrôleur pour connexion par visage |
| `WebcamService.java` | Service de capture vidéo temps réel |
| `admin-users-view.fxml` | Interface admin (avec boutons face) |
| `face_register.fxml` | Interface d'enregistrement du visage |
| `face_login.fxml` | Interface de connexion par visage |
| `haarcascade_frontalface_default.xml` | Classifier cascade Haar pour détection |

## Performance & Optimisation

### Détection de Visage
- **Résolution Cascade**: 24x24 pixels (par défaut)
- **Facteur d'Échelle**: 1.1 (augmente graduellement)
- **Min Neighbors**: 5 (pour réduire faux positifs)
- **Performance**: ~30-60 FPS sur hardware standard

### Reconnaissance LBPH
- **Apprentissage**: 0.5-2 secondes (20 samples)
- **Reconnaissance**: ~5-10ms par frame
- **Seuil de Confiance**: 70.0 (ajustable)
- **Performance**: Temps réel sur desktop moderne

### Optimisation Recommandée
1. **Multithread**: Frame processing sur thread séparé
2. **Frame Skipping**: Process 1 frame sur 2-3 pour réduire CPU
3. **Resolution**: Réduire à 480x360 pour streaming
4. **GPU Acceleration**: Utiliser CUDA si disponible

## Déploiement

### Compilation
```bash
mvn clean compile
```

### Package
```bash
mvn package
```

### Exécution
```bash
# Via IDE (IntelliJ/Eclipse)
Run → Run 'HomeMain' (ou autre main class)

# Via JAR
java -jar target/untitled-1.0-SNAPSHOT.jar
```

### Permissions Requises
- **Accès Caméra**: Demandé à l'OS au premier accès
- **Accès Système de Fichiers**: `face_data/` doit être accessible en lecture/écriture
- **Base de Données**: Connection string dans `football-data.local.properties`

## Troubleshooting Technique

### Erreur: "Cannot find cascade classifier"
**Cause:** `haarcascade_frontalface_default.xml` manquant du classpath
**Solution:** Vérifier que le fichier est dans `target/classes/` après compilation

### Erreur: "No camera available"
**Cause:** Caméra non initialisée ou accès refusé
**Solution:**
```java
// Dans WebcamService.java, ajouter:
try {
    camera = Webcam.getDefault();
    if (camera == null) throw new Exception("No default webcam found");
    camera.open();
} catch (Exception e) {
    log.error("Camera init failed", e);
}
```

### Erreur: "LBPH model training failed"
**Cause:** Insufficient samples ou mauvaise qualité
**Solution:**
- Vérifier que 20 samples sont capturés
- Vérifier que les samples ne sont pas vides
- Augmenter l'éclairage

### Erreur: "Recognition always fails"
**Cause:** Modèle mal entraîné ou conditions différentes
**Solution:**
- Réenregistrer avec un meilleur éclairage
- Enregistrer avec variations (angles, expressions)
- Augmenter le seuil de confiance

### Erreur: "Database error during login"
**Cause:** Connection timeout ou DB indisponible
**Solution:** Voir logs détaillés dans `AdminUserModerationController.initialize()`

## Sécurité

### Considérations

1. **Stockage des Données Faciales**
   - Les données brutes (Mat) ne sont pas persistées
   - Seul le modèle LBPH entraîné est sauvegardé
   - Modèles stockés localement en `face_data/`

2. **Confidentialité**
   - Pas d'envoi de visages à des serveurs externes
   - Tout traitement local sur machine
   - Conforme RGPD (stockage local)

3. **Anti-Spoofing**
   - 8 confirmations consécutives requises
   - Confidence threshold (70.0) pour rejeter les faux positifs
   - Validation d'état du compte (ACTIVE/INACTIVE)

4. **Audit Trail**
   - Tous les logins loggés dans AuthSession
   - Tentatives échouées loggées (level WARN)

## Limitations Connues

1. **Éclairage**: Performance réduite en faible lumière
2. **Occlusion**: Lunettes/masques peuvent réduire reconnaissance
3. **Fatigue**: Reconnaissances multiples peuvent être lentes
4. **Mono-visage**: Une face par utilisateur (multi-face non supporté)

## Roadmap Futures Améliorations

- [ ] Support du 3D face recognition (plus sécurisé)
- [ ] Face anti-spoofing (liveness detection)
- [ ] Support des masques/lunettes
- [ ] Synchronisation cloud (optionnel)
- [ ] Administration UI pour tuning LBPH parameters
- [ ] Analytics: Historique logins par face

## Documentation Externe

- **OpenCV**: https://docs.opencv.org/
- **JavaCV**: https://github.com/bytedeco/javacv
- **LBPH**: https://docs.opencv.org/master/df/d25/classcv_1_1face_1_1LBPHFaceRecognizer.html

---

**Document Version**: 1.0  
**Last Updated**: 2026-04-18  
**Maintainer**: Development Team - Sport Insight Project

