# EscapeHub

Διαδικτυακή πλατφόρμα συνεργατικών αποστολών μυστηρίου (escape-room challenges) με ανοιχτά δεδομένα από δημόσια APIs.

Εργασία εξαμήνου για το μάθημα Εργαστήριο Τεχνολογίας Λογισμικού, Χειμερινό Εξάμηνο 2026–2027.

## Τεχνολογίες
- Backend: Java, Spring Boot
- Βάση δεδομένων: PostgreSQL
- Έλεγχοι: JUnit, Mockito, Spring Boot Test, PIT
- CI/CD: GitHub Actions, Docker

## Ομάδα και διαδικασία
- [Σύνθεση ομάδας και εναλλαγή Scrum Master](docs/team.md)
- [Definition of Done](docs/definition-of-done.md)
- [AI usage log](docs/ai-usage-log.md)
- [Πρότυπο ανάλυσης απαιτήσεων](docs/requirements/_template.md)

## Κατάσταση
Sprint 1: εγγραφή και είσοδος χρηστών, δημιουργία ομάδων παικτών, ένταξη με invite code και διαχείριση μελών (backend και frontend).

## Προαπαιτούμενα
- Java 21 (JDK 21)
- Git
- PostgreSQL 16, με έναν από τους δύο τρόπους:
  - **Docker (Docker Compose)**, ο επίσημος τρόπος της ομάδας, ή
  - **Απευθείας εγκατάσταση στα Windows** (δες τον οδηγό «Εγκατάσταση PostgreSQL χωρίς Docker»)

> Δεν μπορούν να τρέχουν και οι δύο τρόποι ταυτόχρονα, γιατί και οι δύο χρησιμοποιούν το port `5432`.

## Εγκατάσταση και εκτέλεση

### 1. Λήψη του project
```bash
git clone https://github.com/AfroPistik/EscapeHub_26.git
cd EscapeHub_26
```

### 2. Εκκίνηση της βάσης

**Με Docker Compose:**
```bash
docker compose up -d
```

**Χωρίς Docker:** ακολούθησε τον οδηγό εγκατάστασης και φτιάξε χρήστη και βάση:
```sql
CREATE USER escapehub WITH PASSWORD 'escapehub_dev';
CREATE DATABASE escapehub OWNER escapehub ENCODING 'UTF8' TEMPLATE template0;
```

Τα στοιχεία σύνδεσης (`escapehub` / `escapehub_dev`, port `5432`, βάση `escapehub`) είναι αυτά που περιμένει το `application.yml`. Μην τα αλλάζεις.  
Η σύνδεση με τη βάση ρυθμίζεται με τις μεταβλητές περιβάλλοντος `DB_URL`, `DB_USER`, `DB_PASSWORD` (οι προεπιλεγμένες τιμές είναι μόνο για τοπική ανάπτυξη).

### 3. Εκκίνηση της εφαρμογής

Με Java 21 ρυθμισμένη στο terminal:
```bash
./mvnw spring-boot:run
```

Στα Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

Περίμενε το μήνυμα `Started Main in ... seconds`. Οι πίνακες της βάσης φτιάχνονται αυτόματα από τα migrations του Flyway.

### 4. Έλεγχος
Άνοιξε http://localhost:8080/actuator/health. Πρέπει να δεις `{"status":"UP"}`.

## Tests
```bash
./mvnw test
```
Για πλήρη έλεγχο (με τη βάση να τρέχει):
```bash
./mvnw verify
```

## API

Η πλήρης περιγραφή (αιτήματα, απαντήσεις, κωδικοί σφαλμάτων) βρίσκεται στο `docs/openapi.yaml`.

Η ταυτοποίηση γίνεται με session cookie (`JSESSIONID`). Τα σφάλματα επιστρέφονται σε κοινή μορφή `{ code, message, timestamp, fieldErrors }`.

| Μέθοδος | Διαδρομή | Πρόσβαση | Περιγραφή |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Δημόσιο | Εγγραφή χρήστη |
| `POST` | `/api/auth/login` | Δημόσιο | Είσοδος και δημιουργία session |
| `POST` | `/api/auth/logout` | Συνδεδεμένος | Έξοδος |
| `GET` | `/api/auth/me` | Συνδεδεμένος | Στοιχεία τρέχοντος χρήστη |
| `POST` | `/api/teams` | Συνδεδεμένος | Δημιουργία ομάδας (ο δημιουργός γίνεται OWNER) |
| `GET` | `/api/teams` | Συνδεδεμένος | Οι ομάδες μου |
| `POST` | `/api/teams/join` | Συνδεδεμένος | Ένταξη με invite code |
| `GET` | `/api/teams/{id}` | Μέλος ομάδας | Λεπτομέρειες ομάδας και μέλη |
| `POST` | `/api/teams/{id}/leave` | Μέλος (όχι OWNER) | Αποχώρηση από ομάδα |
| `DELETE` | `/api/teams/{id}/members/{userId}` | OWNER | Αφαίρεση μέλους |

## Συνηθισμένα προβλήματα
- **Port 5432 already in use:** Βεβαιώσου ότι δεν τρέχουν ταυτόχρονα το Docker container της PostgreSQL και η τοπική υπηρεσία PostgreSQL στα Windows.
- **Σφάλμα έκδοσης Java:** Έλεγξε με `java -version` ότι το `JAVA_HOME` δείχνει σε JDK 21.
- **Αδυναμία σύνδεσης με τη βάση:** Βεβαιώσου ότι το Docker Desktop είναι ανοιχτό και το container τρέχει (`docker compose ps`).
