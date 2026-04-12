# 🚀 Démarrage rapide - Sport Insight

## ⚡ 3 étapes pour lancer l'application

### 1️⃣ Vérifier Maven
```bash
mvn -v
```
Si cette commande ne fonctionne pas → Installez Maven (voir SETUP.md)

### 2️⃣ Compiler le projet
```bash
mvn clean compile
```

### 3️⃣ Lancer l'application
```bash
mvn javafx:run
```

---

## 🆘 Si ça ne fonctionne pas

### "mvn: command not found"
→ Installez Maven et ajoutez-le au PATH

### "JavaFX runtime components are missing"
→ Relancez: `mvn clean compile`

### "Cannot find FXML file"
→ Vérifiez que les fichiers sont dans `src/main/resources/views/`

---

## 💡 Alternative (Windows)
Double-cliquez sur `run.bat` pour lancer automatiquement l'application

---

## 📚 Besoin d'aide détaillée?
Consultez `SETUP.md` pour les instructions complètes

