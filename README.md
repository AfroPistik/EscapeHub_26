# EscapeHub

Διαδικτυακή πλατφόρμα συνεργατικών αποστολών μυστηρίου (escape-room challenges) με ανοιχτά δεδομένα από δημόσια APIs.

Εργασία εξαμήνου για το μάθημα Εργαστήριο Τεχνολογίας Λογισμικού, Χειμερινό Εξάμηνο 2026–2027.

## Τεχνολογίες
- Backend: Java, Spring Boot
- Βάση δεδομένων: PostgreSQL
- Έλεγχοι: JUnit, Mockito, Spring Boot Test, PIT
- CI/CD: GitHub Actions, Docker

## Κατάσταση
Sprint 0: ανάλυση απαιτήσεων και στήσιμο διαδικασίας. Το project δεν είναι ακόμα εκτελέσιμο.

## Ομάδα και διαδικασία
- [Σύνθεση ομάδας και εναλλαγή Scrum Master](docs/team.md)
- [Definition of Done](docs/definition-of-done.md)
- [AI usage log](docs/ai-usage-log.md)
- [Πρότυπο ανάλυσης απαιτήσεων](docs/requirements/_template.md)

## Οδηγίες εγκατάστασης και εκτέλεσης
Θα προστεθούν όταν υπάρξει η πρώτη εκτελέσιμη έκδοση.

## Τοπική εκτέλεση

Απαιτούνται: JDK 21, Docker Desktop.

1. Εκκίνηση βάσης: `docker compose up -d`
2. Εκκίνηση εφαρμογής: `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
3. Έλεγχος: http://localhost:8080/actuator/health

Η σύνδεση με τη βάση ρυθμίζεται με τις μεταβλητές περιβάλλοντος `DB_URL`, `DB_USER`, `DB_PASSWORD`.
Οι προεπιλεγμένες τιμές είναι μόνο για τοπική ανάπτυξη.

Tests: `./mvnw verify` (με τη βάση να τρέχει).