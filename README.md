# Sport Insight

Sport Insight is a Java 17 desktop application backed by JDBC and MySQL.

The project now includes:
- JavaFX homepage dashboard
- JavaFX CRUD for `equipe`
- JavaFX CRUD for `joueur`
- JavaFX CRUD for `matchs`
- Console entry points for the other modules already present in the project

## Team

- Director: `Elyes Chaouch`
- Gestion match: `Elyes Chaouch`
- Gestion produit: `Amine Bouchnak`
- Gestion user: `Sirine Saidaoui`
- Gestion entrainement: `Tesnim Fekih`
- Gestion annonce: `Sayda Guennichi`
- Gestion sponsor: `Rym Hamouda`

## Features

- Homepage dashboard as the default app entry screen
- JavaFX CRUD for `equipe`
- JavaFX CRUD for `joueur`
- JavaFX CRUD for `matchs`
- Console CRUD for `product`
- Console CRUD for `order`
- Console CRUD for `entrainement`
- Console CRUD for `evaluation`
- Console CRUD for `participation`
- Console CRUD for `annonce`
- Console CRUD for `commentaire`
- Console CRUD for `sponsor`
- Console CRUD for `contrat_sponsor`
- Console CRUD for `user`
- Shared MySQL connection through JDBC

## Latest UI Updates (April 2026)

- Homepage (`home-view.fxml` + `home-theme.css`) fully rebuilt with a custom green dashboard layout.
- Homepage sidebar now opens by default when the app starts.
- Equipe sidebar now opens by default when the Equipe module starts.
- Core module cards on the homepage were resized to medium and standardized to the same size.

## Tech Stack

- Java 17
- Maven
- JavaFX 21
- MySQL
- JDBC

## Project Structure

```text
src/main/java/tn/esprit/
|-- Controller/
|-- entities/
|-- gui/
|-- mains/
|-- services/
`-- tools/
```

```text
src/main/resources/tn/esprit/
|-- images/
|-- styles/
`-- views/
```

## Main Classes

- `tn.esprit.gui.HomeMain`: JavaFX homepage
- `tn.esprit.gui.EquipeCrudMain`: JavaFX `equipe` screen
- `tn.esprit.gui.JoueurCrudMain`: JavaFX `joueur` screen
- `tn.esprit.gui.MatchCrudMain`: JavaFX `matchs` screen
- `tn.esprit.mains.Main`: database connection check only
- `tn.esprit.mains.LauncherMain`: console launcher menu for all modules
- `tn.esprit.mains.MatchMain`: `equipe`, `joueur`, `matchs`
- `tn.esprit.mains.ProductMain`: `product`, `order`
- `tn.esprit.mains.EntrainementMain`: `entrainement`, `evaluation`, `participation`
- `tn.esprit.mains.AnnonceMain`: `annonce`, `commentaire`
- `tn.esprit.mains.SponsorMain`: `sponsor`, `contrat_sponsor`
- `tn.esprit.mains.UserMain`: `user`

## Database Configuration

The database connection is configured in [src/main/java/tn/esprit/tools/MyConnection.java](src/main/java/tn/esprit/tools/MyConnection.java).

Current settings:

```java
private static final String URL = "jdbc:mysql://127.0.0.1:3306/sport_insight?useSSL=false&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "";
```

Update these values if your local MySQL configuration is different.

## Managed Tables

- `equipe`
- `joueur`
- `matchs`
- `product`
- `order`
- `entrainement`
- `evaluation`
- `participation`
- `annonce`
- `commentaire`
- `sponsor`
- `contrat_sponsor`
- `user`

## Requirements

Before running the project, make sure you have:

- Java installed
- Maven installed
- MySQL running
- A database named `sport_insight`
- The required tables created in MySQL

## Installation

```bash
git clone https://github.com/elyesexe/esprit-PIDEV-JAVA-Sport-Insight-3A46.git
cd "PI Java"
mvn compile
```

## Run

### Default app

Run the JavaFX homepage:

```bash
mvn javafx:run
```

### Direct JavaFX screens

```bash
mvn javafx:run "-Djavafx.mainClass=tn.esprit.gui.EquipeCrudMain"
mvn javafx:run "-Djavafx.mainClass=tn.esprit.gui.JoueurCrudMain"
mvn javafx:run "-Djavafx.mainClass=tn.esprit.gui.MatchCrudMain"
```

### Console entry points

From IntelliJ or the terminal, run the main class you need.

Examples:

```bash
mvn exec:java -Dexec.mainClass="tn.esprit.mains.Main"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.LauncherMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.MatchMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.ProductMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.EntrainementMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.AnnonceMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.SponsorMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.UserMain"
```

To start from the console launcher menu, run `tn.esprit.mains.LauncherMain`.

## JavaFX Modules

- `HomeMain`: homepage with navigation to the main JavaFX modules
- `EquipeCrudMain`: team management with image selection from Windows Explorer
- `JoueurCrudMain`: player management with image selection from Windows Explorer
- `MatchCrudMain`: basic match management with team selectors and scores

## Console Usage

### LauncherMain

```text
========================================
      WELCOME TO SPORT INSIGHT
========================================
1. Match module
2. Product module
3. Entrainement module
4. Annonce module
5. Sponsor module
6. User module
0. Exit
Choice:
```

### MatchMain

```text
========================================
 MATCH MODULE
========================================
1. Manage equipes
2. Manage joueurs
3. Manage matchs
4. Statistics
0. Exit
Select an option:
```

### ProductMain

```text
--- PRODUCT MODULE ---
1. Manage products
2. Manage orders
0. Exit
Choice:
```

### EntrainementMain

```text
SPORT INSIGHT MENU
1. Manage entrainements
2. Manage evaluations
3. Manage participations
0. Exit
Choice:
```

### AnnonceMain

```text
--- ANNOUNCE MANAGEMENT ---
1. Add annonce
2. Show all annonces
3. Update annonce
4. Delete annonce
5. Search annonces by title
6. Sort annonces by publication date
7. Add commentaire to annonce
8. Show commentaires for annonce
9. Search commentaires by content
0. Exit
Choice:
```

### SponsorMain

```text
============================================
      SPONSOR & CONTRACT MANAGEMENT
============================================
1. Sponsor Management
2. Contract Sponsor Management
0. Exit
Choose an option:
```

### UserMain

```text
--- USER MODULE ---
1. Add user
2. Display all users
3. View user by id
4. Search users
5. Update user
6. Delete user
0. Exit
Choice:
```

Inside each module, CRUD actions now follow the same numbered style, for example:

```text
========================================
 MATCHS
========================================
1. Add match
2. Display all matchs
3. Update match
4. Delete match
5. Search match
6. Sort matchs
Select an action:

========================================
 SORT MATCHS
========================================
1. By date
2. By location
3. By type
Select a sort mode:

========================================
 MATCH LIST
========================================
[1] MATCH-69A76521395FF689676675
  Date/Time        : 2026-02-11 21:00
  Location         : Riyadh
  Type             : final supercopa
  Status           : ended
  Score            : 4 - 0
  Home team ID     : 1
  Away team ID     : 2
  Home lineup      : 4-3-3
  Away lineup      : 4-3-3
```

## Notes

- Date input format: `yyyy-mm-dd`
- Time input format for matches: `HH:mm:ss`
- Time input format for entrainements: `HH:mm`
- If your MySQL schema uses different table or column names, update the SQL in the service classes.
- `Gestion-sponsor` was merged into this project and its sponsor entrypoint is available as `SponsorMain`.
- `gestion_user` was merged locally into this branch and its entrypoint is available as `UserMain`.

## Branches

- `main`: current integration branch
- `gestion-match`: match-focused branch used for the match module flow
