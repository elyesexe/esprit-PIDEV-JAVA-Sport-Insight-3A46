# Sport Insight

Java console application for managing `equipe`, `joueur`, and `matchs` data with JDBC and MySQL.

## Features

- CRUD for `equipe`
- CRUD for `joueur`
- CRUD for `matchs`
- Interactive console flow
- MySQL connection through JDBC

## Tech Stack

- Java 17
- Maven
- MySQL
- JDBC

## Project Structure

```text
src/main/java/tn/esprit/
├── entities/
├── mains/
├── services/
└── tools/
```

## Database Configuration

The database connection is configured in [src/main/java/tn/esprit/tools/MyConnection.java](src/main/java/tn/esprit/tools/MyConnection.java).

Current settings:

```java
private static final String URL = "jdbc:mysql://127.0.0.1:3306/sport_insight?useSSL=false&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "";
```

Update these values if your local MySQL configuration is different.

## Supported Tables

### `equipe`

Handled fields:

- `id`
- `nom`
- `coach`
- `adresse`
- `telephone`
- `email`
- `image`

### `joueur`

Handled fields:

- `id`
- `nom`
- `prenom`
- `date_naissance`
- `numero`
- `image`
- `equipe_id`

### `matchs`

Handled fields:

- `id`
- `id_match`
- `date_match`
- `heure_debut`
- `lieu`
- `type`
- `statut`
- `lineup_domicile`
- `lineup_exterieur`
- `score_equipe_domicile`
- `score_equipe_exterieur`
- `equipe_domicile_id`
- `equipe_exterieur_id`

## Requirements

Before running the project, make sure you have:

- Java installed
- Maven installed
- MySQL running
- A database named `sport_insight`
- The required tables created in MySQL

## Installation

Clone the project:

```bash
git clone https://github.com/your-username/your-repository.git
cd your-repository
```

Compile the project:

```bash
mvn compile
```

## Run

Run the main class from IntelliJ or from the terminal.

If you use IntelliJ:

- Open the project
- Run `tn.esprit.mains.Main`

If you use Maven from terminal:

```bash
mvn exec:java -Dexec.mainClass="tn.esprit.mains.Main"
```

## Console Usage

When the application starts, it asks:

```text
Choose a table to manipulate (equipe, joueur, matchs) or type exit:
```

You can type:

- `equipe`
- `joueur`
- `matchs`
- `exit`

Then the application asks whether you want to:

- add
- display all
- display one by id
- update
- delete

## Notes

- Date input format for `joueur` and `matchs`: `yyyy-mm-dd`
- Time input format for `matchs`: `HH:mm:ss`
- Some SQL column names are assumed from the entity naming. If your MySQL schema uses different names, update the SQL in the service classes.

## Author

Project developed as a Java CRUD console application for database practice.
