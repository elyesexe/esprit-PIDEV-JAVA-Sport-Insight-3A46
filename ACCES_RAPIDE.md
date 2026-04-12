# 📋 Carte d'accès rapide - Sport Insight

## 🎯 Vous cherchez quoi?

### ⚡ Je veux lancer l'application maintenant
👉 **Voir:** `EXECUTION.md` ou `QUICKSTART.md`  
**Commande:** `mvn javafx:run`

### 📖 J'ai besoin d'aide pour installer
👉 **Voir:** `SETUP.md`  
**Durée:** 10 minutes

### 🔍 Je veux connaître les changements
👉 **Voir:** `CHANGES.md`  
**Durée:** 5 minutes

### 🆘 J'ai une erreur/problème
👉 **Voir:** `SETUP.md` → Section "Dépannage"

### 📚 Je veux comprendre le projet
👉 **Voir:** `README.md`  
**Durée:** 15 minutes

---

## 📂 Structure de fichiers

```
esprit-PIDEV-JAVA-Sport-Insight-3A46/
│
├── 📄 Documentation
│   ├── README.md           ← Vue d'ensemble
│   ├── SETUP.md            ← Guide complet d'installation
│   ├── QUICKSTART.md       ← Démarrage rapide (3 étapes)
│   ├── EXECUTION.md        ← Comment lancer
│   ├── CHANGES.md          ← Modifications apportées
│   ├── INDEX.md            ← Guide de navigation
│   └── ACCES_RAPIDE.md     ← Ce fichier
│
├── 🔧 Scripts
│   ├── run.bat             ← Exécution (Windows)
│   ├── run.sh              ← Exécution (Linux/Mac)
│   ├── verify.bat          ← Vérification (Windows)
│   └── verify.sh           ← Vérification (Linux/Mac)
│
├── ⚙️ Configuration
│   ├── pom.xml             ← Configuration Maven (MODIFIÉ)
│   ├── .idea/
│   │   └── runConfigurations.xml
│   └── src/main/java/
│       └── module-info.java ← Configuration modules (MODIFIÉ)
│
├── 🎨 Code source
│   └── src/
│       ├── main/java/tn/esprit/
│       │   ├── mains/
│       │   │   ├── LoginApp.java       ← Point d'entrée (MODIFIÉ)
│       │   │   ├── Main.java
│       │   │   └── UserMain.java
│       │   ├── controllers/
│       │   │   └── LoginController.java ← Contrôleur (MODIFIÉ)
│       │   ├── entities/
│       │   ├── services/
│       │   └── tools/
│       └── resources/
│           ├── views/
│           │   ├── login.fxml         ← Interface (MODIFIÉ)
│           │   └── register.fxml
│           └── images/
│               └── sportinsight.png
```

---

## 🎬 Scénarios courants

### Scénario 1: "Je découvre le projet"
1. Lire `README.md` (5 min)
2. Lire `QUICKSTART.md` (2 min)
3. Exécuter `mvn javafx:run` (1 min)

### Scénario 2: "Je veux comprendre les changements"
1. Lire `CHANGES.md` (5 min)
2. Consulter `SETUP.md` pour les détails techniques
3. Vérifier les fichiers modifiés

### Scénario 3: "J'ai une erreur"
1. Consulter `SETUP.md` → Section "Dépannage"
2. Exécuter `verify.bat` ou `verify.sh`
3. Lire `CHANGES.md` pour comprendre les corrections

### Scénario 4: "Je dois configurer la DB"
1. Ouvrir `tools/MyConnection.java`
2. Entrer les identifiants MySQL
3. Tester la connexion en lançant l'app

---

## ⚡ Commandes essentielles

```bash
# Vérifier que tout est installé
mvn -v
java -version

# Compiler le projet
mvn clean compile

# Lancer l'application
mvn javafx:run

# Nettoyer les fichiers compilés
mvn clean

# Voir les dépendances
mvn dependency:tree
```

---

## 📊 État de chaque fichier

| Fichier | Status | Commentaire |
|---------|--------|------------|
| pom.xml | ✅ Modifié | Dépendances complètes |
| module-info.java | ✅ Modifié | Packages ouverts |
| LoginApp.java | ✅ Modifié | Chemin FXML corrigé |
| LoginController.java | ✅ Modifié | Chemins corrigés |
| login.fxml | ✅ Modifié | Label d'erreur fixée |
| Documentation | ✅ Créée | 6 fichiers |
| Scripts | ✅ Créés | 4 scripts |

---

## 💡 Conseils

### Pour les débutants
- Commencez par `QUICKSTART.md`
- N'installez pas Maven si vous n'êtes pas à l'aise
- Utilisez IntelliJ IDEA qui gère Maven automatiquement

### Pour les développeurs expérimentés
- Utilisez `mvn javafx:run` directement
- Consultez `CHANGES.md` pour comprendre les modifications
- Continuez le développement en partant de la structure existante

### Pour le dépannage
- Toujours exécuter `mvn clean compile` d'abord
- Utiliser les scripts `verify.bat` ou `verify.sh`
- Consulter la section "Dépannage" de `SETUP.md`

---

## 📞 Besoin d'aide rapide?

| Question | Réponse |
|----------|--------|
| Comment lancer? | `mvn javafx:run` ou voir `EXECUTION.md` |
| Maven non trouvé? | Voir `SETUP.md` → "Prérequis" |
| Erreur JavaFX? | Voir `SETUP.md` → "Dépannage" |
| Fichier FXML manquant? | Vérifier `src/main/resources/views/` |
| Besoin de détails? | Lire `CHANGES.md` |

---

## 🎯 Prochaines étapes

### Court terme (aujourd'hui)
1. [ ] Lancer l'application: `mvn javafx:run`
2. [ ] Voir l'écran de connexion
3. [ ] Lire `CHANGES.md` pour comprendre les corrections

### Moyen terme (cette semaine)
1. [ ] Configurer la base de données MySQL
2. [ ] Créer l'écran d'enregistrement
3. [ ] Tester la connexion avec des utilisateurs réels

### Long terme (cette mois)
1. [ ] Implémenter le dashboard
2. [ ] Ajouter la gestion des matchs
3. [ ] Ajouter la gestion des joueurs

---

## ✅ Validations

Avant de commencer le développement, vérifiez:

- [ ] `mvn -v` affiche une version
- [ ] `java -version` affiche Java 17+
- [ ] `mvn clean compile` se termine sans erreur
- [ ] `mvn javafx:run` lance l'application
- [ ] L'écran de connexion s'affiche

---

## 📈 Performance

| Action | Temps |
|--------|-------|
| 1ère compilation | ~2-3 minutes |
| Compilations suivantes | ~10-30 secondes |
| Lancement de l'app | ~5-10 secondes |
| Téléchargement Maven (1ère fois) | ~1-2 minutes |

---

## 🔗 Ressources externes

- **JavaFX Documentation:** https://openjfx.io/
- **Maven Documentation:** https://maven.apache.org/
- **Java 17 Documentation:** https://docs.oracle.com/en/java/javase/17/

---

**Vous êtes prêt ! 🚀**

**Commencez par:** `mvn javafx:run`

---

*Dernière mise à jour: 2026-04-12*  
*Statut: ✅ Projet prêt*

