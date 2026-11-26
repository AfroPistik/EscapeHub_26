# Changelog

Όλες οι σημαντικές αλλαγές του project καταγράφονται εδώ.
Η μορφή βασίζεται στο [Keep a Changelog](https://keepachangelog.com/el/1.1.0/).

## [Sprint 1] - 2026-11-26

### Προστέθηκαν

- Σύμβαση API σε OpenAPI (`docs/openapi.yaml`) για αυθεντικοποίηση και ομάδες (US-01).
- Αρχιτεκτονικές αποφάσεις (ADR): session αντί για JWT, ο ρόλος στο TeamMember, κανόνες invite code και ένταξης (US-02).
- Βάση δεδομένων PostgreSQL με migrations Flyway για χρήστες, ομάδες και μέλη, με περιορισμούς (μοναδικό email χωρίς διάκριση πεζών/κεφαλαίων, μοναδικό username, μοναδικός invite code) (US-04).
- Repositories, Docker Compose για την Postgres και υποδομή integration tests (US-05, US-16).
- Εγγραφή χρήστη (`POST /api/auth/register`) με validation, κωδικό σε BCrypt και χειρισμό διπλού email/username (409) ακόμη και σε ταυτόχρονες εγγραφές (US-06).
- Είσοδος, έξοδος και στοιχεία τρέχοντος χρήστη με session (`/api/auth/login`, `/api/auth/logout`, `/api/auth/me`) (US-07).
- Ρύθμιση ασφαλείας (SecurityConfig), `CurrentUserService` και απάντηση 401 στην κοινή μορφή σφάλματος (US-08).
- Δημιουργία ομάδας με invite code, ο δημιουργός γίνεται OWNER (US-09).
- Λίστα των ομάδων μου (`GET /api/teams/mine`) και λεπτομέρειες ομάδας (US-10).
- Ένταξη σε ομάδα με invite code (US-11).
- Λίστα μελών ομάδας (`GET /api/teams/{id}/members`) (US-12).
- Αποχώρηση μέλους από ομάδα, με απαγόρευση για τον OWNER (`DELETE /api/teams/{id}/members/me`) (US-13).
- Κοινή μορφή σφαλμάτων (`ErrorResponse`) και `GlobalExceptionHandler` (US-15).
- Integration test για την ένταξη σε ομάδα.
- Frontend: σελίδες εγγραφής και εισόδου, API client, route guard, logout (US-18, US-19).
- README με οδηγίες εγκατάστασης και εκτέλεσης, CHANGELOG και QA checklist (US-03).

### Δεν ολοκληρώθηκαν στο Sprint 1

- Αφαίρεση μέλους από τον OWNER (US-14): το endpoint `DELETE /api/teams/{id}/members/{userId}` δεν έχει υλοποιηθεί.
- Frontend για ομάδες: οι ομάδες μου, δημιουργία ομάδας, ένταξη με κωδικό, λεπτομέρειες και μέλη (US-20, US-21, US-22).

### Γνωστά θέματα

Τα παρακάτω βρέθηκαν στο QA της Ημέρας 6 (`docs/qa-checklist.md`, issue #NN):

- Το `GET /api/teams` δίνει 405. Η λίστα των ομάδων του χρήστη είναι το `GET /api/teams/mine`, ενώ το `docs/openapi.yaml` ορίζει `GET /api/teams`.
- Η αποχώρηση είναι `DELETE /api/teams/{id}/members/me`, ενώ το openapi ορίζει `POST /api/teams/{id}/leave`.
- Το πεδίο `members` είναι κενό στις απαντήσεις του `POST /api/teams` και του `GET /api/teams/{id}`. Τα μέλη επιστρέφονται από το `GET /api/teams/{id}/members`.
- Το openapi δεν περιέχει τα `/api/teams/mine`, `/api/teams/{id}/members` και `/api/users/me`.
- Μικρά: `createdAt` είναι `null` στην απάντηση του join και ο κωδικός σφάλματος 403 διαφέρει ανάμεσα στα endpoints (`NOT_A_MEMBER` και `FORBIDDEN`).
- Το endpoint των στοιχείων χρήστη υποστηρίζει προσωρινά δύο paths (`/api/auth/me` και `/api/users/me`) μέχρι να αποφασίσει η ομάδα (issue #50).
- Το CSRF είναι απενεργοποιημένο στο Sprint 1 (απόφαση καταγεγραμμένη στο `SecurityConfig`). Να επανεξεταστεί πριν από πραγματική χρήση.