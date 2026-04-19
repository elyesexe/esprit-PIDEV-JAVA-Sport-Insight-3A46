# Sport Insight Knowledge

Sport Insight is a JavaFX desktop application focused on football management, analysis, and user experience.

## Main modules

- Home: central launcher for the user workspace.
- Equipes: manage clubs, coaches, contacts, images, and competition-related team data.
- Joueurs: manage player profiles, team assignment, positions, images, and enriched football metadata.
- Matchs: manage fixtures, statuses, scores, lineups, and competition-linked match details.
- Leagues: browse competitions and standings.
- Annonces: publish updates and user-facing communication.
- Entrainements: manage training sessions, participation, and evaluation flows.
- Sponsors: manage sponsor records, presentation screens, and contract workflows.
- Store: manage products, catalog browsing, and order-related flows.
- Profile: session-aware personal information and user identity.

## Roles

- Guest users can reach authentication screens.
- Authenticated users can access the main user workspace.
- Admin users can access the admin shell, dashboard, moderation, and admin CRUD screens.

## Admin workspace

The admin shell centralizes:

- dashboard KPIs and charts
- user moderation
- CRUD access to major modules
- a return path back to the user interface

## Guidance rules

- Stay focused on Sport Insight and its workflows.
- Do not invent data that is not visible in the current context.
- If a user asks for navigation, open the closest matching module or detail page when the app data makes that possible.
- Prefer opening exact competition and match pages over only describing where they live.
- If a user asks how to do something, explain the relevant module workflow.
- Respect role boundaries when discussing admin-only actions.

## Local AI and voice setup

- The assistant is designed to work with a local Ollama model so there are no paid API fees.
- The recommended smart local model for modest hardware is `qwen2.5:3b`.
- Voice input is offline through Whisper with a downloadable local model, with Vosk kept as a fallback path.
- Voice output on Windows uses Piper with a downloadable local neural voice.
