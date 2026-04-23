# Fix: Register Face - Path Error

## Problem Identified 🔴

**Error:** "Register Face" button was not working - the face registration modal would not open.

**Root Cause:** Incorrect FXML resource path in `AdminUserModerationController.java` line 314

```java
// ❌ INCORRECT (line 314)
getClass().getResource("/fxml/face_register.fxml")
```

The code was looking for the FXML file at:
- Expected: `/fxml/face_register.fxml`
- Actual location: `/tn/esprit/views/face_register.fxml`

## Solution Applied ✅

**Fixed Path:** Updated to correct resource location

```java
// ✅ CORRECT (line 314)
getClass().getResource("/tn/esprit/views/face_register.fxml")
```

## Changes Made

| File | Line | Change |
|------|------|--------|
| `AdminUserModerationController.java` | 314 | `/fxml/face_register.fxml` → `/tn/esprit/views/face_register.fxml` |

## How It Works Now

### User Flow:
1. Admin selects a user in the user moderation table
2. Admin clicks **"Register Face"** button
3. ✅ Modal window opens with face registration interface
4. User captures 20 facial samples
5. LBPH model is trained and saved
6. User can now log in with facial recognition

### File Structure Verified:
```
src/main/resources/
├── tn/esprit/views/
│   ├── face_register.fxml        ✅ FOUND
│   ├── face_login.fxml           ✅ FOUND
│   ├── admin-users-view.fxml     ✅ FOUND
│   └── ... (other views)
```

## Testing

To verify the fix works:

1. **Compile the project**
   ```bash
   mvn clean compile
   ```

2. **Run the application**
   - Navigate to Admin Dashboard → User Moderation
   - Select a user from the table
   - Click the **"Register Face"** button
   - ✅ Modal should open without errors

3. **Expected Behavior:**
   - Modal title: "Register Face — [User Name]"
   - Camera preview displays
   - Status shows: "Waiting for user selection…"
   - "Start Capture" button is enabled

## Additional Resource Paths

All other resource paths are correctly configured:

| Resource | Path | Status |
|----------|------|--------|
| face_register.fxml | `/tn/esprit/views/face_register.fxml` | ✅ Fixed |
| face_login.fxml | `/tn/esprit/views/face_login.fxml` | ✅ OK |
| admin-users-view.fxml | `/tn/esprit/views/admin-users-view.fxml` | ✅ OK |
| haarcascade_frontalface_default.xml | classpath | ✅ OK |

## Prevention

For future FXML files, always use the correct resource path:

```java
// Template for FXML loading
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/tn/esprit/views/YOUR_FILE_NAME.fxml")
);
```

The path structure is:
```
/tn/esprit/views/[filename].fxml
```

---

**Status:** ✅ FIXED  
**Date:** 2026-04-18  
**Impact:** Critical - Face registration now functional

