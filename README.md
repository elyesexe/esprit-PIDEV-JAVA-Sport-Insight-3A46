# Sport Insight

Sport Insight is a JavaFX desktop platform dedicated to football management, analysis, and user experience. The project brings together administration, sports data management, public-facing interfaces, and connected services inside one modern application backed by Java, JDBC, and MySQL.

The main idea behind Sport Insight is simple: create a single environment where teams, players, matches, training sessions, announcements, sponsors, products, and users can be managed through rich graphical interfaces while also benefiting from live football data and visual dashboards.

## Project Vision

Sport Insight is designed as a digital ecosystem for football organizations and communities. It combines:

- sports administration
- operational CRUD management
- visual dashboards and statistics
- competition and standings consultation
- connected APIs for real-world football content
- user-oriented browsing interfaces

The application focuses on usability, structured data, and a polished JavaFX experience for both administrators and regular users.

## Authors

- Elyes Chaouch — Director, Match management
- Amine Bouchnak — Product management
- Sirine Saidaoui — User management
- Tesnim Fekih — Training management
- Sayda Guennichi — Announcement management
- Rym Hamouda — Sponsoring management

## Functional Scope

Sport Insight covers several business areas inside the same application:

- user authentication and profile management
- admin dashboard and moderation
- team management
- player management
- match management
- training management
- announcement and communication management
- sponsor and contract management
- product and store management
- league discovery, competition browsing, and standings consultation

## Main Modules

### 1. Administration

The admin area centralizes the supervision of the platform. It includes:

- an admin shell with side navigation
- a dashboard with KPIs and charts
- quick access to the main data modules
- moderation tools for users
- a visual overview of teams, players, matches, announcements, and training sessions

### 2. Teams

The team module manages football clubs and squads through dedicated interfaces.

Main capabilities:

- create a team
- view team details
- update team information
- delete a team
- browse team lists
- explore linked competition data
- sync teams from football-data sources

Managed information includes:

- name
- coach
- address
- phone
- email
- image
- competition code
- external source metadata

### 3. Players

The player module manages athlete profiles with both local and enriched data.

Main capabilities:

- create a player
- view player details
- update player information
- delete a player
- browse player lists
- assign players to teams
- display player images
- enrich profiles with external football data and Wikidata images

Managed information includes:

- first name
- last name
- date of birth
- jersey number
- image
- team assignment
- position
- nationality
- external source metadata

### 4. Matches

The match module handles fixture planning, scores, and match tracking.

Main capabilities:

- create a match
- view match details
- update match information
- delete a match
- browse scheduled and finished matches
- link home and away teams
- track scores and statuses
- sync matches from external football competitions

Managed information includes:

- match identifier
- date
- kickoff time
- location
- type
- status
- home lineup
- away lineup
- home score
- away score
- home team
- away team
- competition code
- external source metadata

### 5. Training

The training module supports sports preparation and attendance workflows.

Main capabilities:

- create training sessions
- edit and delete sessions
- browse planning data
- manage participation and evaluation-related flows

### 6. Announcements

The announcement module is used to publish content for users and manage communication.

Main capabilities:

- create announcements
- update announcement content
- delete announcements
- browse announcements in admin and user views
- manage status and required level

### 7. Sponsors

The sponsoring module focuses on sponsorship visibility and contract follow-up.

Main capabilities:

- sponsor CRUD
- sponsor presentation screens
- sponsorship contract management
- PDF-related service support
- visual admin and user interfaces

### 8. Products and Store

Sport Insight also includes a product and store area.

Main capabilities:

- product CRUD
- catalog browsing
- order-related entities and services
- shopping-oriented user interface

### 9. Authentication and User Space

The application contains a complete user flow:

- sign up
- login
- session-based access
- role handling
- profile interface
- access-aware navigation

## CRUD Coverage

Sport Insight includes strong CRUD coverage across the project. The platform manages operations such as:

- `Equipe` CRUD
- `Joueur` CRUD
- `Matchs` CRUD
- `Annonce` CRUD
- `Entrainement` CRUD
- `Sponsor` CRUD
- `Product` CRUD
- `User` administration and moderation
- contract, participation, comment, evaluation, and order-related data services

This CRUD logic is implemented through service classes, JavaFX controllers, and dedicated FXML views.

## Interfaces and Screens

The project is interface-driven and built around JavaFX views. Important screens include:

- home interface
- login interface
- signup interface
- profile interface
- admin shell
- admin dashboard
- admin users view
- team CRUD view
- team list, detail, and form views
- player admin, CRUD, list, and detail views
- match admin, CRUD, form, list, and detail views
- training admin and user views
- announcement admin and user views
- sponsor admin and user views
- product/store views
- competition and standings views

Notable FXML files:

- `home-view.fxml`
- `admin-shell.fxml`
- `admin-dashboard.fxml`
- `equipe-crud-view.fxml`
- `joueur-crud-view.fxml`
- `match-crud-view.fxml`
- `entrainement-admin-view.fxml`
- `annonce-crud-view.fxml`
- `sponsor-admin-view.fxml`
- `product-crud-view.fxml`
- `store-view.fxml`
- `league-table-view.fxml`

## Dashboard and Statistics

Sport Insight includes visual dashboards to make data easier to understand.

Current dashboard direction includes:

- administrative KPI cards
- operational summary tables
- team statistics charts
- player distribution charts
- match status charts
- recent activity views

These elements help transform raw data into quick decisions for administrators.

## External APIs and Data Enrichment

One of the strengths of Sport Insight is the integration of external football information sources.

### football-data.org integration

The project integrates football-data services to:

- import teams
- import players
- import matches
- map competition codes
- retrieve standings and league data

Main related classes:

- `FootballDataSyncService`
- `FootballDataApiClient`
- `FootballDataStandingsService`
- `FootballDataCompetitions`
- `LeagueStandingsSnapshot`
- `LeagueStandingEntry`
- `FootballDataConfig`

### Football data providers

The project now combines multiple sources so the free tier remains usable:

- `football-data.org` is the main source for fixtures, teams, players, standings, and official competition top scorers
- `TheSportsDB` is the free fallback for match statistics and starting lineups when deeper per-match detail is needed
- `API-Football` remains optional as an extra provider when it returns richer statistics such as expected goals for supported fixtures

Main related classes:

- `ApiFootballInsightsService`
- `ApiFootballClient`
- `ApiFootballCompetitionMappings`
- `ApiFootballMatchDetails`
- `ApiFootballScorerEntry`
- `ApiFootballConfig`

### Wikidata integration

Wikidata is used to enrich player information, especially media assets.

Main related classes:

- `WikidataPlayerImageService`
- `WikidataApiClient`
- `WikidataSparqlClient`

This enrichment improves the realism and presentation quality of player profiles.

## Technologies Used

- Java 17
- JavaFX 21
- Maven
- JDBC
- MySQL
- Jackson Databind
- jBCrypt
- PDFBox
- WebP ImageIO
- JUnit 5

## Architecture Overview

The project follows a layered desktop application structure.

### `src/main/java/tn/esprit/Controller`

Contains JavaFX controllers responsible for:

- user interaction
- form management
- table refresh logic
- dashboard handling
- navigation between views

### `src/main/java/tn/esprit/entities`

Contains domain entities such as:

- `Equipe`
- `Joueur`
- `Matchs`
- `Entrainement`
- `Annonce`
- `Sponsor`
- `ContratSponsor`
- `Product`
- `Order`
- `User`

### `src/main/java/tn/esprit/services`

Contains business logic and data access services for:

- CRUD operations
- statistics preparation
- synchronization
- enrichment
- PDF generation
- domain workflows

### `src/main/java/tn/esprit/gui`

Contains application launch and UI support classes such as:

- `HomeMain`
- `EquipeCrudMain`
- `JoueurCrudMain`
- `MatchCrudMain`
- navigation and theme helpers

### `src/main/java/tn/esprit/tools`

Contains technical helpers for:

- database connection
- schema migration
- external API configuration
- utility support

## API Keys

Sport Insight can use two football APIs in parallel:

- `football-data.org` for fixtures, teams, players, standings, and scorer leaderboards
- `TheSportsDB` for free match stats and starting lineups
- `API-Football` as an optional richer-match-data provider when available

You can configure them with environment variables or local properties files:

- `FOOTBALL_DATA_API_KEY`
- `API_FOOTBALL_KEY`
- `football-data.local.properties`
- `api-football.local.properties`

Example files:

- `football-data.local.properties.example`
- `api-football.local.properties.example`

### `src/main/resources/tn/esprit/views`

Contains all FXML interfaces.

### `src/main/resources/tn/esprit/styles`

Contains the visual identity of the application, including module-specific themes and admin styling.

### `src/main/resources/tn/esprit/images`

Contains visual assets used across interfaces.

## Database

Sport Insight relies on a MySQL database named `sport_insight`.

The connection is configured in:

- `src/main/java/tn/esprit/tools/MyConnection.java`

The schema supports multiple modules, including:

- `equipe`
- `joueur`
- `matchs`
- `entrainement`
- `participation`
- `evaluation`
- `annonce`
- `commentaire`
- `sponsor`
- `contrat_sponsor`
- `product`
- `order`
- `order_item`
- `user`
- additional supporting tables for the full platform

## Security and Access

The project includes a security layer for authentication and role handling.

Relevant components include:

- `AuthSession`
- `PasswordSupport`
- `UserRoles`
- login and signup controllers

This allows Sport Insight to separate admin usage from standard user access and deliver a more structured platform experience.

## User Experience Approach

The project emphasizes:

- rich graphical interfaces
- module-based navigation
- reusable layouts
- dashboard visibility
- modern football-oriented styling
- dark mode support in admin areas
- detailed screens for teams, players, and matches

The goal is not only to store sports data, but to make that data meaningful, accessible, and attractive through a complete interface experience.

## Why Sport Insight Matters

Sport Insight is more than a standard CRUD project. It combines local management and connected football intelligence inside one application. The platform shows how a sports-oriented system can evolve from simple administration into a full digital hub with interfaces, analytics, synchronization, and multi-module collaboration.

In that sense, Sport Insight represents:

- a football management platform
- a data visualization platform
- an administration tool
- a user-facing sports experience
- a connected system enriched by external APIs

## Summary

Sport Insight is a JavaFX and MySQL football platform that brings together:

- complete graphical interfaces
- multiple business modules
- CRUD operations across the application
- administration and dashboard views
- public and user-oriented screens
- football-data and Wikidata integration
- a clear sports-tech concept centered on football insight and management
