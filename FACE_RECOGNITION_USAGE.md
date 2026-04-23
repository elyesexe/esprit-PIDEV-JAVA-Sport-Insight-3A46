# Guide d'Utilisation - Reconnaissance Faciale

## Vue d'Ensemble

Le système Sport Insight inclut une intégration complète de reconnaissance faciale permettant aux administrateurs de gérer l'authentification par visage pour les utilisateurs.

## 1. Accéder à la Modération des Utilisateurs

```
Accueil → Admin Dashboard → User Moderation
```

Vous verrez:
- **Tableau gauche**: Liste de tous les utilisateurs
- **Formulaire droit**: Détails de l'utilisateur sélectionné

## 2. Enregistrer un Visage pour un Utilisateur

### Étape 1: Sélectionner l'Utilisateur
1. Cliquez sur un utilisateur dans le tableau des comptes
2. Le formulaire de droite se remplit avec ses détails
3. Remarquez les boutons **"Register Face"** et **"Delete Face"** en bas du formulaire

### Étape 2: Cliquer sur "Register Face"
```
[Register Face]  [Delete Face]
[Save changes]   [Delete user]
```
- Le bouton **"Register Face"** est toujours actif (si aucun visage)
- Le bouton **"Delete Face"** est grisé jusqu'à l'enregistrement d'un visage

### Étape 3: Window de Registration
Une fenêtre modale s'ouvre avec:

```
┌────────────────────────────────────┐
│     Register Face                   │
│ Capture 20 samples — move your     │
│ head slowly for best accuracy      │
├────────────────────────────────────┤
│                                    │
│        [  CAMERA FEED  ]           │
│                                    │
│                                    │
│   Samples: 0 / 20                  │
│   [================== 0%]          │
│                                    │
│   Status: Ready                    │
│                                    │
│   [▶ Start Capture]  [Cancel]      │
└────────────────────────────────────┘
```

### Étape 4: Capture des Échantillons
1. Cliquez **"▶ Start Capture"**
2. Le système capture automatiquement des échantillons (350ms d'intervalle)
3. Bougez lentement votre tête:
   - **Samples 0-5**: Regardez droit devant
   - **Samples 5-10**: Tournez légèrement à gauche
   - **Samples 10-15**: Tournez légèrement à droite
   - **Samples 15-20**: Levez et baissez le menton

### Étape 5: Entraînement du Modèle
```
Status: Training face model…
[Training spinner visible]
```
- Le système entraîne le modèle LBPH (Local Binary Patterns Histograms)
- Durée: quelques secondes

### Étape 6: Succès
```
Status: Face registered! [User Name] can now log in with their face.
Progress: [================== 100%]
```
- La fenêtre se ferme automatiquement après 2.2 secondes
- Les données faciales sont sauvegardées

## 3. Supprimer le Visage d'un Utilisateur

### Étape 1: Sélectionner l'Utilisateur
1. Cliquez sur l'utilisateur ayant déjà un visage enregistré
2. Le bouton **"Delete Face"** devient actif (couleur rouge)

### Étape 2: Cliquer sur "Delete Face"
```
[Register Face]  [Delete Face]  ← Rouge et actif
     (Grisé)
```

### Étape 3: Confirmation
```
┌──────────────────────────────────┐
│  Delete face data                │
├──────────────────────────────────┤
│ Delete face data for [User Name]?│
│                                  │
│ The user will no longer be able  │
│ to log in with their face.       │
│                                  │
│     [OK]      [Cancel]           │
└──────────────────────────────────┘
```

### Étape 4: Suppression
- Les fichiers de données faciales sont supprimés
- Le modèle LBPH est réinitialisé
- L'utilisateur ne peut plus se connecter par reconnaissance faciale

## 4. Connexion par Reconnaissance Faciale (Utilisateur)

### Pour un Utilisateur Enregistré:

**Écran de Login:**
```
┌──────────────────────────────────┐
│    Sport Insight                 │
│    Authentication                │
├──────────────────────────────────┤
│  Email:        [_______________] │
│  Password:     [_______________] │
│                                  │
│  [Sign in]     [Face login]      │
│                                  │
│  New here? [Create account]      │
└──────────────────────────────────┘
```

1. Cliquez **"Face login"**

**Écran de Reconnaissance Faciale:**
```
┌──────────────────────────────────┐
│  Sport Insight                   │
│  Face Authentication             │
├──────────────────────────────────┤
│                                  │
│    [  LIVE CAMERA FEED  ]        │
│    [Rectangle auto-détecté]      │
│                                  │
│                                  │
│  Centre your face within frame   │
│  [Progress bar pour confirmation]│
│                                  │
│  Status: Position your face...   │
│                                  │
│  [Try Again]  [Use Password...]  │
└──────────────────────────────────┘
```

2. Positionnez votre visage dans le rectangle
3. Le système détecte et reconnaît automatiquement
4. **8 confirmations consécutives** requises (sécurité anti-spoofing)
5. Vous êtes automatiquement connecté

### États Possibles:

| État | Message | Action |
|------|---------|--------|
| ✅ Détecté | "Recognising…" | Continue, 8 frames needed |
| ❌ Pas détecté | "No face detected" | Repositionnez votre visage |
| ⚠️ Non reconnu | "Face not recognised" | Vérifiez l'enregistrement |
| 🔒 Compte inactif | "Account is inactive" | Contactez l'admin |
| 🎉 Succès | "Welcome, [Name]!" | Redirection automatique |

## 5. État des Boutons

### Bouton "Register Face":
| Condition | État | Couleur |
|-----------|------|--------|
| Aucun utilisateur sélectionné | Désactivé (grisé) | Gris |
| Utilisateur sans visage | Activé | Bleu |
| Utilisateur avec visage | Activé | Bleu |

### Bouton "Delete Face":
| Condition | État | Couleur |
|-----------|------|---------|
| Aucun utilisateur sélectionné | Désactivé (grisé) | Gris |
| Utilisateur sans visage | Désactivé (grisé) | Gris |
| Utilisateur avec visage | Activé | Rouge |

## 6. Troubleshooting

### Problème: "No faces detected"
**Cause**: Mauvais éclairage ou visage hors de cadre
**Solution**:
- Assurez-vous que votre visage est bien visible
- Améliorez l'éclairage
- Centrez votre visage dans le rectangle

### Problème: "Face not recognised"
**Cause**: Données faciales insuffisantes ou trop différentes
**Solution**:
- Enregistrez à nouveau avec 20 nouveaux échantillons
- Assurez-vous de bouger naturellement
- Utilisez un bon éclairage stable

### Problème: Erreur de caméra
**Cause**: Caméra occupée ou non disponible
**Solution**:
- Fermez les autres applications utilisant la caméra
- Redémarrez l'application
- Vérifiez les permissions d'accès caméra

### Problème: "Database unavailable"
**Cause**: Connexion base de données perdue
**Solution**:
- Vérifiez la connexion réseau
- Redémarrez le serveur de base de données
- Vérifiez les credentials dans `.properties`

## 7. Sécurité

### Mesures de Sécurité Implémentées:

✅ **Anti-spoofing**: 8 confirmations consécutives requises  
✅ **Vérification d'Activité**: Compte doit être ACTIF  
✅ **Chiffrement**: Données faciales stockées de manière sécurisée  
✅ **Suppression Confirmée**: Confirmation requise avant suppression  
✅ **Logs**: Tous les accès sont loggés  

### Recommandations:

1. Enregistrez le visage dans un bon éclairage
2. Changez votre enregistrement facial chaque 6-12 mois
3. Supprimez les données faciales si vous quittez l'organisation
4. Gardez un mot de passe robuste en secours

## 8. Specifications Techniques

- **Détection**: Cascade Haar (haarcascade_frontalface_default.xml)
- **Reconnaissance**: LBPH (Local Binary Patterns Histograms)
- **Échantillons Requis**: 20 par utilisateur
- **Confirmations Requises**: 8 frames consécutifs
- **Format**: OpenCV Mat / BSD license

## Support

Pour des problèmes:
1. Vérifiez les logs de l'application
2. Contactez l'administrateur système
3. Consultez la documentation OpenCV

---

**Version**: 1.0  
**Dernière Mise à Jour**: 2026-04-18  
**Support**: Admin Team - Sport Insight Project

