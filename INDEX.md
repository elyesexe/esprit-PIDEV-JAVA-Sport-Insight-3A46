# 📚 Index de documentation - Sport Insight

## 🎯 Par où commencer?

### ⚡ Vous êtes pressé?
👉 **Lisez:** [`QUICKSTART.md`](QUICKSTART.md) (3 étapes, 2 minutes)

### 📖 Vous voulez des détails?
👉 **Lisez:** [`SETUP.md`](SETUP.md) (guide complet, 10 minutes)

### 🔍 Vous voulez connaître les changements?
👉 **Lisez:** [`CHANGES.md`](CHANGES.md) (résumé des modifications)

### 📘 Documentation générale
👉 **Lisez:** [`README.md`](README.md) (aperçu du projet)

---

## 📑 Guide par fichier

| Fichier | Contenu | Public |
|---------|---------|--------|
| **QUICKSTART.md** | 3 étapes pour lancer l'app | Développeurs impatients |
| **SETUP.md** | Guide complet d'installation | Nouveaux développeurs |
| **CHANGES.md** | Résumé des modifications | Auditeurs/Mainteneurs |
| **README.md** | Documentation du projet | Tous |
| **INDEX.md** | Ce fichier | Navigation |

---

## 🛠️ Scripts disponibles

### Windows
- **`run.bat`** - Compiler et lancer l'application
- **`verify.bat`** - Vérifier la configuration

### Linux/Mac
- **`run.sh`** - Compiler et lancer l'application
- **`verify.sh`** - Vérifier la configuration

---

## 🚀 Démarrage rapide

```bash
# Vérifier Maven
mvn -v

# Compiler
mvn clean compile

# Lancer
mvn javafx:run
```

---

## 📊 Fichiers modifiés

| Fichier | Type | Modification |
|---------|------|--------------|
| `pom.xml` | Config | ✅ Ajout des dépendances manquantes |
| `module-info.java` | Config | ✅ Configuration des modules corrects |
| `src/main/java/tn/esprit/mains/LoginApp.java` | Code | ✅ Chemin FXML corrigé |
| `src/main/java/tn/esprit/controllers/LoginController.java` | Code | ✅ Chemins FXML corrigés |
| `src/main/resources/views/login.fxml` | UI | ✅ Label d'erreur mise à jour |

---

## 📁 Nouveaux fichiers créés

### Documentation
- `README.md` - Documentaire complet
- `SETUP.md` - Guide détaillé
- `QUICKSTART.md` - Démarrage rapide
- `CHANGES.md` - Historique des modifications
- `INDEX.md` - Ce fichier

### Scripts
- `run.bat` / `run.sh` - Exécution
- `verify.bat` / `verify.sh` - Vérification
- `.idea/runConfigurations.xml` - Configuration IDE

---

## ❓ Dépannage rapide

### "mvn not found"
→ Installez Maven et ajoutez-le au PATH

### "JavaFX runtime components are missing"
→ Exécutez: `mvn clean compile`

### "Cannot find FXML file"
→ Vérifiez que les fichiers sont dans `src/main/resources/views/`

👉 **Pour plus d'aide:** Consultez la section **Dépannage** dans [`SETUP.md`](SETUP.md)

---

## ✅ Checklist

- [ ] Java JDK 17+ installé
- [ ] Maven 3.8+ installé
- [ ] `mvn -v` fonctionne
- [ ] `mvn clean compile` se termine sans erreur
- [ ] `mvn javafx:run` lance l'application
- [ ] L'écran de connexion s'affiche

---

## 🎯 Prochaines étapes

1. **Configurer MySQL:**
   - Ouvrir `tools/MyConnection.java`
   - Configurer les identifiants

2. **Implémenter les écrans:**
   - Créer `register.fxml`
   - Créer `dashboard.fxml`

3. **Ajouter la sécurité:**
   - Hacher les mots de passe
   - Valider les formulaires

---

## 📞 Support

| Problème | Solution |
|----------|----------|
| Installation | Voir [`SETUP.md`](SETUP.md) section "Prérequis" |
| Exécution | Voir [`QUICKSTART.md`](QUICKSTART.md) |
| Erreurs | Voir [`SETUP.md`](SETUP.md) section "Dépannage" |
| Modifications | Voir [`CHANGES.md`](CHANGES.md) |

---

**Dernière mise à jour:** 2026-04-12

