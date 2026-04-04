# Sport Insight

Java console application for managing `match`, `produit`, and `entrainement` features with JDBC and MySQL.

## Features

- CRUD for `equipe`
- CRUD for `joueur`
- CRUD for `matchs`
- CRUD for `product`
- CRUD for `order`
- CRUD for `entrainement`
- CRUD for `evaluation`
- CRUD for `participation`
- Separate entry points for each module
- MySQL connection through JDBC

## Tech Stack

- Java 17
- Maven
- MySQL
- JDBC

## Project Structure

```text
src/main/java/tn/esprit/
|-- entities/
|-- mains/
|-- services/
`-- tools/
```

## Main Classes

- `tn.esprit.mains.Main`: database connection check only
- `tn.esprit.mains.MatchMain`: `equipe`, `joueur`, `matchs`
- `tn.esprit.mains.ProductMain`: `product`, `order`
- `tn.esprit.mains.EntrainementMain`: `entrainement`, `evaluation`, `participation`

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

From IntelliJ or the terminal, run the main class you need.

Examples:

```bash
mvn exec:java -Dexec.mainClass="tn.esprit.mains.Main"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.MatchMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.ProductMain"
mvn exec:java -Dexec.mainClass="tn.esprit.mains.EntrainementMain"
```

## Console Usage

### MatchMain

```text
--- MATCH MODULE ---
1. Manage equipes
2. Manage joueurs
3. Manage matchs
0. Exit
Choice:
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

Inside each module, CRUD actions now follow the same numbered style, for example:

```text
--- EVALUATIONS ---
1. Add evaluation
2. Display all evaluations
3. Update evaluation
4. Delete evaluation
5. Search evaluation
6. Sort evaluations
Choice:
```

## Notes

- Date input format: `yyyy-mm-dd`
- Time input format for matches: `HH:mm:ss`
- Time input format for entrainements: `HH:mm`
- If your MySQL schema uses different table or column names, update the SQL in the service classes.

## Branch

This merged version is available on branch `match+prod+entrainement`.
