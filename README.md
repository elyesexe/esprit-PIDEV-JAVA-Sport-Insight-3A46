# Sport Insight

Sport Insight is a Java 17 desktop application backed by JDBC and MySQL.

The project now includes:
- JavaFX homepage dashboard
- JavaFX CRUD for `equipe`
- JavaFX CRUD for `joueur`
- JavaFX CRUD for `matchs`

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

## JavaFX Modules

- `HomeMain`: homepage with navigation to the main JavaFX modules
- `EquipeCrudMain`: team management with image selection from Windows Explorer
- `JoueurCrudMain`: player management with image selection from Windows Explorer
- `MatchCrudMain`: basic match management with team selectors and scores

## Notes

- Date input format: `yyyy-mm-dd`
- If your MySQL schema uses different table or column names, update the SQL in the service classes.

## Branches

- `main`: current integration branch
- `gestion-match`: match-focused branch used for the match module flow
