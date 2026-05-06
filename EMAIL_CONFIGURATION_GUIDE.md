# Email Configuration Guide for Evaluation Notifications

## Problem
You're seeing: **"SMTP configuration missing"** when trying to send evaluation emails.

## Solution

### Step 1: Configure Gmail App Password

1. **Go to your Google Account**: https://myaccount.google.com/security

2. **Enable 2-Step Verification** (if not already enabled):
   - Click on "2-Step Verification"
   - Follow the setup process

3. **Create an App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" as the app
   - Select "Windows Computer" (or your device)
   - Click "Generate"
   - **Copy the 16-character password** (it looks like: `abcd efgh ijkl mnop`)

### Step 2: Edit the Configuration File

Open the file: **`evaluation-mail.local.properties`** (already created at project root)

Replace these values:

```properties
# Replace with YOUR Gmail address
mail.sender=your-email@gmail.com

# Replace with YOUR App Password (remove spaces)
mail.password=abcdefghijklmnop
```

**Example:**
```properties
mail.sender=john.doe@gmail.com
mail.password=xyzw1234abcd5678
```

### Step 3: Save and Test

1. Save the file
2. Restart your application
3. Try creating or updating an evaluation
4. The email should be sent successfully!

## Configuration File Locations

The system checks these locations (in order):
1. `evaluation-mail.local.properties` (project root) ✅ **Recommended**
2. `src/main/resources/evaluation-mail.local.properties`
3. `~/.sport-insight/evaluation-mail.local.properties` (user home directory)

## Alternative: Environment Variables

Instead of using a file, you can set environment variables:

**Windows (PowerShell):**
```powershell
$env:SPORT_INSIGHT_SMTP_USERNAME="your-email@gmail.com"
$env:SPORT_INSIGHT_SMTP_PASSWORD="your-app-password"
$env:SPORT_INSIGHT_SMTP_FROM="your-email@gmail.com"
```

**Windows (CMD):**
```cmd
set SPORT_INSIGHT_SMTP_USERNAME=your-email@gmail.com
set SPORT_INSIGHT_SMTP_PASSWORD=your-app-password
set SPORT_INSIGHT_SMTP_FROM=your-email@gmail.com
```

**Linux/Mac:**
```bash
export SPORT_INSIGHT_SMTP_USERNAME="your-email@gmail.com"
export SPORT_INSIGHT_SMTP_PASSWORD="your-app-password"
export SPORT_INSIGHT_SMTP_FROM="your-email@gmail.com"
```

## Troubleshooting

### Error: "Authentication failed (535)"
- **Cause**: Wrong password or App Password not created
- **Solution**: 
  1. Make sure you're using an App Password, NOT your regular Gmail password
  2. Verify 2-Step Verification is enabled
  3. Generate a new App Password

### Error: "Connection timeout"
- **Cause**: Firewall or network blocking SMTP port 587
- **Solution**: 
  1. Check your firewall settings
  2. Try using port 465 with SSL instead:
     ```properties
     mail.smtp.port=465
     smtp.ssl=true
     ```

### Email not sending but no error
- **Cause**: Configuration file not found
- **Solution**: 
  1. Verify the file is named exactly: `evaluation-mail.local.properties`
  2. Check it's in the project root directory
  3. Restart the application

## Security Notes

⚠️ **IMPORTANT:**
- Never commit `evaluation-mail.local.properties` to Git (it's in .gitignore)
- Never share your App Password
- Use App Passwords, not your main Gmail password
- Revoke App Passwords you're not using

## Full Configuration Options

```properties
# SMTP Server
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587

# Authentication
mail.smtp.auth=true
mail.smtp.starttls.enable=true

# Credentials
mail.sender=your-email@gmail.com
mail.password=your-app-password

# Optional: Use SSL instead of TLS
# smtp.ssl=true
# mail.smtp.port=465
```

## Testing

After configuration, test by:
1. Creating a new evaluation for a player with a valid email
2. Check the success message
3. Verify the email was received

The email will contain:
- Training session details
- Physical, Technical, and Tactical scores
- Average score
- Coach comments
