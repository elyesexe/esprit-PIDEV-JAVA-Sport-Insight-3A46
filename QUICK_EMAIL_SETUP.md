# ⚡ Quick Email Setup (5 minutes)

## You're seeing: "SMTP configuration missing"

### Fix it in 3 steps:

## 1️⃣ Get Gmail App Password

1. Go to: https://myaccount.google.com/apppasswords
2. Click "Generate" 
3. Copy the 16-character password (example: `abcd efgh ijkl mnop`)

**Note:** You need 2-Step Verification enabled first: https://myaccount.google.com/security

## 2️⃣ Edit Configuration File

Open: **`evaluation-mail.local.properties`** (in project root)

Change these 2 lines:

```properties
mail.sender=YOUR_EMAIL@gmail.com
mail.password=YOUR_APP_PASSWORD_HERE
```

**Example:**
```properties
mail.sender=coach@gmail.com
mail.password=abcdefghijklmnop
```

## 3️⃣ Restart & Test

1. Save the file
2. Restart your application
3. Create/update an evaluation
4. ✅ Email sent!

---

## Need Help?

See full guide: **EMAIL_CONFIGURATION_GUIDE.md**

## Common Issues

**"Authentication failed"** → Use App Password, not regular password
**"Timeout"** → Check firewall, try port 465
**Still not working?** → Check file is named exactly: `evaluation-mail.local.properties`
