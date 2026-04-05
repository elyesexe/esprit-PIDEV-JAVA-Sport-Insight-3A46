# Quick Start Guide - Démarrage Rapide

## 🚀 En 5 Minutes

### Étape 1 : Vérifier les prérequis
```bash
# Vérifier Java
java -version
# Doit afficher : java version 17 ou supérieur

# Vérifier javac
javac -version
# Doit afficher : javac 17 ou supérieur
```

### Étape 2 : Compiler le projet
```bash
# Sur Windows avec le script fourni
compile.bat

# Ou manuellement avec javac
javac -d target/classes -cp "C:\Users\hamou\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;C:\Users\hamou\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar" src/main/java/tn/esprit/entities/*.java src/main/java/tn/esprit/tools/*.java src/main/java/tn/esprit/services/*.java src/main/java/tn/esprit/mains/*.java
```

### Étape 3 : Exécuter l'application
```bash
# Sur Windows avec le script fourni
run.bat

# Ou manuellement
java -cp "target/classes;C:\Users\hamou\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;C:\Users\hamou\.m2\repository\com\google\protobuf\protobuf-java\3.25.1\protobuf-java-3.25.1.jar" tn.esprit.mains.Main
```

### Étape 4 : Utiliser l'application
```
Menu Principal s'affiche automatiquement
Sélectionner une option (1, 2, ou 0)
Suivre les instructions

1. Sponsor Management       → Gérer les sponsors
2. Contract Sponsorship     → Gérer les contrats
0. Exit                     → Quitter
```

---

## 📝 Exemple : Ajouter un Sponsor

```
1                           ← Choisir Sponsor Management
1                           ← Choisir Add Sponsor
Apple                       ← Nom
contact@apple.com           ← Email
+33612345678                ← Téléphone
100000                      ← Budget
apple_logo.png              ← Logo
Cupertino, California       ← Adresse

✓ Message de succès
```

---

## 🔍 Exemple : Rechercher un Sponsor

```
1                           ← Sponsor Management
4                           ← Search Sponsor
apple                       ← Mot-clé de recherche

Résultat:
Sponsor{id=1, nom='Apple', email='contact@apple.com', ...}
```

---

## ✏️ Exemple : Mettre à jour un Sponsor

```
1                           ← Sponsor Management
5                           ← Update Sponsor
1                           ← ID du sponsor
                            ← Nom (appuyez sur Entrée pour ignorer)
new@email.com               ← Nouvel email
                            ← Téléphone (appuyez sur Entrée pour ignorer)
120000                      ← Nouveau budget
                            ← Adresse (appuyez sur Entrée pour ignorer)

✓ Message de succès
```

---

## 🗑️ Exemple : Supprimer un Sponsor

```
1                           ← Sponsor Management
6                           ← Delete Sponsor
1                           ← ID du sponsor à supprimer

✓ Message de succès
```

---

## 📋 Exemple : Ajouter un Contrat

```
2                           ← Contract Sponsor Management
1                           ← Add Contract
2024-01-01                  ← Date de début
2024-12-31                  ← Date de fin
50000                       ← Montant
Sponsorship for football    ← Description
ACTIVE                      ← Statut
1                           ← Sponsor ID
5                           ← Team ID

✓ Message de succès
```

---

## ✅ Checklist de Mise en Route

- [ ] Java 17+ installé
- [ ] Compiler le projet (compile.bat)
- [ ] Base de données configurée
- [ ] Exécuter l'application (run.bat)
- [ ] Menu affichant correctement
- [ ] Ajouter un sponsor pour tester
- [ ] Rechercher le sponsor
- [ ] Voir tous les sponsors
- [ ] Ajouter un contrat
- [ ] Voir tous les contrats
- [ ] Quitter proprement

---

## 🐛 Dépannage Rapide

### Problème : "Cannot find javac"
**Solution :** Installer le JDK (pas juste le JRE)

### Problème : "Connection refused"
**Solution :** Vérifier que MySQL est démarré et la base de données existe

### Problème : "No such file or directory"
**Solution :** S'assurer d'être dans le répertoire du projet

### Problème : Erreur de compilation
**Solution :** Vérifier que les fichiers JAR sont au bon chemin

### Problème : "Invalid input!"
**Solution :** Entrer un nombre valide ou appuyer sur Entrée

---

## 📚 Documentation Complète

Pour plus de détails, consulter :
- **README.md** - Guide complet
- **CRUD_GUIDE_FR.md** - Guide des opérations
- **TEST_GUIDE.md** - Cas de test
- **DATABASE_SETUP.md** - Configuration SQL
- **CHANGELOG.md** - Résumé des modifications

---

## 🎯 Cas d'Usage Courants

### Cas 1 : Gestion Simple d'un Sponsor
```
1 → 1 (Add) → Entrer les données
1 → 2 (View All) → Voir le résultat
```

### Cas 2 : Trouver un Sponsor
```
1 → 4 (Search) → Taper un mot-clé
```

### Cas 3 : Modifier un Sponsor
```
1 → 3 (View by ID) → Entrer l'ID
1 → 5 (Update) → Modifier les données
```

### Cas 4 : Créer un Contrat
```
2 → 1 (Add) → Entrer les données du contrat
```

### Cas 5 : Voir les Contrats d'un Sponsor
```
2 → 7 (View by Sponsor) → Entrer l'ID du sponsor
```

---

## ⌨️ Raccourcis Clavier

| Touche | Action |
|--------|--------|
| Entrée | Valider/Ignorer (dans les mises à jour) |
| Ctrl+C | Quitter l'application |
| 0      | Retour au menu précédent |

---

## 💡 Conseils d'Utilisation

✅ Toujours vérifier l'ID avant de supprimer
✅ Utiliser les recherches pour vérifier l'existence
✅ Entrer les dates au format YYYY-MM-DD
✅ Garder un terminal ouvert pour voir les messages
✅ Consulter la documentation en cas de doute

---

## 🎓 Apprendre Plus

1. Lire le code source dans `src/main/java`
2. Comprendre l'architecture dans README.md
3. Tester tous les cas dans TEST_GUIDE.md
4. Explorer les requêtes SQL dans DATABASE_SETUP.md

---

**Prêt à démarrer ? → Lancez compile.bat puis run.bat !**

Version: 1.0.0
Status: ✅ Prêt pour utilisation

