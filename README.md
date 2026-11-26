# EscapeHub

Διαδικτυακή πλατφόρμα συνεργατικών αποστολών μυστηρίου (escape-room challenges) με ανοιχτά δεδομένα από δημόσια APIs.

Εργασία εξαμήνου για το μάθημα Εργαστήριο Τεχνολογίας Λογισμικού, Χειμερινό Εξάμηνο 2026–2027.

## Τεχνολογίες
- Backend: Java, Spring Boot
- Βάση δεδομένων: PostgreSQL
- Έλεγχοι: JUnit, Mockito, Spring Boot Test, PIT
- CI/CD: GitHub Actions, Docker

## Κατάσταση
Sprint 1: εγγραφή και είσοδος χρηστών, δημιουργία ομάδων παικτών, ένταξη με invite code, λίστα μελών και αποχώρηση. Η αφαίρεση μέλους από τον OWNER δεν έχει υλοποιηθεί ακόμα. Δες το [CHANGELOG](CHANGELOG.md) και το [QA checklist](docs/qa-checklist.md).

## Ομάδα και διαδικασία
- [Σύνθεση ομάδας και εναλλαγή Scrum Master](docs/team.md)
- [Definition of Done](docs/definition-of-done.md)
- [AI usage log](docs/ai-usage-log.md)
- [Πρότυπο ανάλυσης απαιτήσεων](docs/requirements/_template.md)

## Οδηγίες εγκατάστασης και εκτέλεσης

### Προαπαιτούμενα
- JDK 21
- Docker Desktop (για τη βάση με Docker Compose)
- Git

Εναλλακτικά, αντί για Docker μπορεί να εγκατασταθεί τοπικά PostgreSQL 16 με βάση `escapehub`, χρήστη `escapehub` και κωδικό `escapehub_dev` (port 5432). Δεν μπορούν να τρέχουν και οι δύο τρόποι ταυτόχρονα, γιατί και οι δύο χρησιμοποιούν το port 5432.

### Τοπική εκτέλεση

1. Λήψη του project: `git clone https://github.com/AfroPistik/EscapeHub_26.git`
2. Εκκίνηση βάσης: `docker compose up -d`
3. Εκκίνηση εφαρμογής: `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
4. Έλεγχος: http://localhost:8080/actuator/health. Πρέπει να δεις `{"status":"UP"}`.

Οι πίνακες της βάσης δημιουργούνται αυτόματα από τα migrations του Flyway όταν ξεκινά η εφαρμογή.

Η σύνδεση με τη βάση ρυθμίζεται με τις μεταβλητές περιβάλλοντος `DB_URL`, `DB_USER`, `DB_PASSWORD`.
Οι προεπιλεγμένες τιμές είναι μόνο για τοπική ανάπτυξη.

### Tests

`./mvnw verify` (με τη βάση να τρέχει).

## API

Η περιγραφή της σύμβασης (αιτήματα, απαντήσεις, κωδικοί σφαλμάτων) βρίσκεται στο [`docs/openapi.yaml`](docs/openapi.yaml). Ο παρακάτω πίνακας δείχνει τα endpoints που υπάρχουν στον κώδικα. Οι διαφορές από το openapi καταγράφονται στα «Γνωστά θέματα» του [CHANGELOG](CHANGELOG.md).

Η ταυτοποίηση γίνεται με session cookie (`JSESSIONID`). Τα σφάλματα επιστρέφονται σε κοινή μορφή `{ code, message, timestamp, fieldErrors }`.

| Μέθοδος | Διαδρομή | Πρόσβαση | Περιγραφή |
|---------|----------|----------|-----------|
| POST | `/api/auth/register` | Δημόσιο | Εγγραφή χρήστη |
| POST | `/api/auth/login` | Δημόσιο | Είσοδος και δημιουργία session |
| POST | `/api/auth/logout` | Συνδεδεμένος | Έξοδος |
| GET | `/api/auth/me` | Συνδεδεμένος | Στοιχεία τρέχοντος χρήστη (προσωρινά και ως `/api/users/me`) |
| POST | `/api/teams` | Συνδεδεμένος | Δημιουργία ομάδας (ο δημιουργός γίνεται OWNER) |
| GET | `/api/teams/mine` | Συνδεδεμένος | Οι ομάδες μου |
| POST | `/api/teams/join` | Συνδεδεμένος | Ένταξη με invite code |
| GET | `/api/teams/{id}` | Μέλος ομάδας | Λεπτομέρειες ομάδας |
| GET | `/api/teams/{id}/members` | Μέλος ομάδας | Λίστα μελών |
| DELETE | `/api/teams/{id}/members/me` | Μέλος (όχι OWNER) | Αποχώρηση από ομάδα |

## Συνηθισμένα προβλήματα

| Πρόβλημα | Λύση |
|----------|------|
| `Connection refused` ή η εφαρμογή δεν συνδέεται στη βάση | Η βάση δεν τρέχει. Ξεκίνα το container (`docker compose up -d`) ή την υπηρεσία `postgresql-x64-16` |
| Το port 5432 είναι πιασμένο | Τρέχει ήδη άλλη Postgres ή container. Σταμάτα το (`docker compose stop`) |
| `Migration checksum mismatch` | Σβήσε και ξαναφτιάξε τη βάση `escapehub` |
| Σφάλμα Java έκδοσης ή `JAVA_HOME is not defined correctly` | Χρειάζεται Java 21. Έλεγξε με `java -version` και το `JAVA_HOME` |
