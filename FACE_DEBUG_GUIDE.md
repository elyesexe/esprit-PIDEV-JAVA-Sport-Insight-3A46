# 🔍 Face Recognition Debug Guide

## Current Issue
Face is detected but shows "Unknown" - not recognized correctly

## Diagnostic Steps

### Step 1: Check Database User
The face data exists for userId=1 in `face_data/1/`

**You need to verify:**
- What user has ID=1 in your database?
- Is that user's email correct?
- Is that user marked as ACTIVE?

**To check:**
```sql
SELECT id, email, is_active FROM users WHERE id = 1;
```

### Step 2: Check Console Logs
When starting the app and trying face login, look for these messages:

**GOOD SIGNS:**
```
[INFO] Loaded X user labels for face recognition  
[INFO] Recognized face with confidence 45.5: user@email.com
```

**BAD SIGNS:**
```
[INFO] Loaded 0 user labels for face recognition  ← DB connection failed
[DEBUG] Model not trained yet  ← No model file
[DEBUG] Recognition failed  ← Face not matching confidence
```

### Step 3: Verify face_data Directory
```
face_data/
├── 1/  ← User ID 1 folder
│   ├── sample_0.jpg
│   ├── sample_1.jpg
│   ...
│   └── sample_19.jpg (at least some samples)
└── model.yml  ← Trained model
```

Current status: ✅ FOUND (face data for user 1 exists)

### Step 4: Recompile and Restart

```bash
# 1. Clean old compilation
mvn clean

# 2. Compile with latest changes
mvn compile

# 3. Run application
java -jar target/untitled-1.0-SNAPSHOT.jar
# OR
mvn spring-boot:run

# 4. Watch console for logs
```

### Step 5: Test Face Login

1. Go to login page
2. Click "Face login"
3. **Watch the console carefully** for:
   - `Loaded X user labels for face recognition`
   - If you see `Loaded 0 user labels` → DATABASE ISSUE
   - If you see nothing → LABELS NOT LOADED

### Step 6: Enable Debug Logging

Add to `application.properties` or `application.yml`:
```properties
logging.level.tn.esprit.face=DEBUG
logging.level.tn.esprit.Controller.FaceLoginController=DEBUG
logging.level.tn.esprit.services.UserService=DEBUG
```

Then restart and check output for detailed information.

## Common Issues & Solutions

### Issue 1: "Loaded 0 user labels"
**Cause:** Database connection failed or no users in database
**Solution:**
1. Check database is running
2. Check database connection credentials
3. Verify users table has data: `SELECT * FROM users LIMIT 5;`
4. Verify user with ID=1 exists

### Issue 2: "Model not trained yet"
**Cause:** No model.yml file or corruption
**Solution:**
1. Verify file exists: `face_data/model.yml`
2. If missing, re-register a face via admin panel
3. Check file size is > 10KB

### Issue 3: "Unknown" displayed (detected but not recognized)
**Cause:** Face doesn't match trained data or confidence too high
**Solution:**
1. Check console logs for confidence score
2. Try with better lighting
3. Re-register face with admin panel
4. Ensure 20+ samples captured

### Issue 4: "No face detected"
**Cause:** Camera issue or poor lighting
**Solution:**
1. Improve lighting
2. Center face in frame
3. Check webcam is working: other apps can access it
4. Restart application

## What the Fix Does

✅ **Loads real user emails from database** (instead of placeholders)
✅ **Maps recognized face ID to actual email** (for DB lookup)
✅ **Provides detailed logging** (for debugging)
✅ **Handles errors gracefully** (with clear messages)

## Verification Checklist

After applying the fix:

- [ ] Code compiled successfully
- [ ] Application started without errors
- [ ] Console shows "Loaded X user labels"
- [ ] Face detected (red box appears)
- [ ] After 8 frames, login succeeds
- [ ] Dashboard appears

## Expected Console Output

```
[INFO] Initializing face login...
[INFO] Loading users from database...
[INFO] Loaded 5 user labels for face recognition
[INFO] LBPH model loaded from face_data/model.yml
[DEBUG] Webcam started
[DEBUG] detect → detecting faces
[DEBUG] predict → label=1 conf=45.5
[INFO] Recognized face with confidence 45.5: john@example.com
[INFO] User found: john@example.com
[INFO] Account is ACTIVE
[INFO] AuthSession set for user: john@example.com
[INFO] Redirecting to dashboard
```

## Quick Checklist

1. Is the code compiled with the latest changes?
2. Is the application actually running with the compiled code?
3. Is the database running and accessible?
4. Does user ID=1 exist in the database?
5. Is the console showing detailed logs?

If you check all these and it's still not working, provide:
- The console logs when trying to login
- The database query result: `SELECT id, email, is_active FROM users WHERE id = 1;`
- Whether you see "Loaded X user labels" message

---

**Status:** Fix applied, needs recompilation and restart
**Next Step:** Follow diagnostic steps above

