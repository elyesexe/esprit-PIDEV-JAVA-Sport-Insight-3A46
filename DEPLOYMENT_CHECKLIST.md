# Sport Insight Java/Web Validation Checklist

Use this checklist before the PIDEV Java/Web integration validation.

## Local Services

- [ ] XAMPP Control Panel opens from `C:\xampp\xampp-control.exe`.
- [ ] Apache is running and `http://127.0.0.1/` returns a page.
- [ ] MySQL is running on port `3306`.
- [ ] phpMyAdmin opens at `http://127.0.0.1/phpmyadmin/`.

## Shared Database

- [ ] Database `sport_insight` exists.
- [ ] Java and Symfony both point to `127.0.0.1:3306/sport_insight`.
- [ ] Shared tables contain demo data for users, teams, players, matches, training, announcements, sponsors, products, and orders.
- [ ] Only additive schema repairs are applied. Do not run destructive Doctrine schema updates on the shared demo database.

## Java App

- [ ] Run `mvn test` from `C:\PI Java`.
- [ ] Run the JavaFX app with `mvn javafx:run`.
- [ ] Verify login, admin dashboard, teams, players, matches, training, announcements, sponsors, products/store, profile, and notifications.

## Symfony App

- [ ] Run `composer install` from `C:\final\sport_insight_final`.
- [ ] Run `php bin/phpunit`.
- [ ] Run `php bin/console lint:twig templates`.
- [ ] Run `vendor/bin/phpstan analyse`.
- [ ] Run `composer audit`.
- [ ] Start the app with `symfony server:start` or `php -S 127.0.0.1:8000 -t public`.

## Demo Scenario

- [ ] Prepare one short integrated scenario that moves through all modules instead of separate disconnected demos.
- [ ] Show matching database records from both apps where possible.
- [ ] Confirm both READMEs explain setup, modules, database, and integration.
- [ ] Prepare the commercial video required by the rubric.
