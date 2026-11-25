# Changelog

Όλες οι σημαντικές αλλαγές του project καταγράφονται εδώ.
Η μορφή βασίζεται στο [Keep a Changelog](https://keepachangelog.com/el/1.1.0/).

<!-- TODO: βάλε την ημερομηνία παράδοσης του sprint και αφαίρεσε τα issues που δεν ολοκληρώθηκαν. -->

## [Sprint 1] - ΗΗΗΗ-ΜΜ-ΗΗ

### Προστέθηκαν

- Σύμβαση API σε OpenAPI (`docs/openapi.yaml`) για αυθεντικοποίηση και ομάδες (US-01).
- Αρχιτεκτονικές αποφάσεις (ADR): session αντί για JWT, ο ρόλος στο TeamMember, κανόνες invite code και ένταξης (US-02).
- Βάση δεδομένων PostgreSQL με migrations Flyway για χρήστες, ομάδες και μέλη, με περιορισμούς (μοναδικό email χωρίς διάκριση πεζών/κεφαλαίων, μοναδικό username, μοναδικός invite code) (US-04).
- Repositories, Docker Compose για την Postgres και υποδομή integration tests (US-05, US-16).
- Εγγραφή χρήστη (`POST /api/auth/register`) με validation, κωδικό σε BCrypt και χειρισμό διπλού email/username (409) ακόμη και σε ταυτόχρονες εγγραφές (US-06).
- Είσοδος, έξοδος και στοιχεία τρέχοντος χρήστη με session (`/api/auth/login`, `/api/auth/logout`, `/api/auth/me`) (US-07).
- Ρύθμιση ασφαλείας (SecurityConfig), `CurrentUserService` και απάντηση 401 στην κοινή μορφή σφάλματος (US-08).
- Δημιουργία ομάδας με invite code, ο δημιουργός γίνεται OWNER (US-09).
- Λίστα των ομάδων μου και λεπτομέρειες ομάδας (US-10).
- Ένταξη σε ομάδα με invite code (US-11).
- Λίστα μελών ομάδας (US-12).
- Αποχώρηση μέλους από ομάδα, με απαγόρευση για τον OWNER (US-13).
- Αφαίρεση μέλους από τον OWNER (US-14).
- Κοινή μορφή σφαλμάτων (`ErrorResponse`) και `GlobalExceptionHandler` (US-15).
- End-to-end test της ροής (US-17).
- Frontend: σελίδες εγγραφής και εισόδου, API client, route guard, logout (US-18, US-19).
- Frontend: οι ομάδες μου, δημιουργία ομάδας, ένταξη με κωδικό, λεπτομέρειες και μέλη ομάδας (US-20, US-21, US-22).
- README με οδηγίες εγκατάστασης και εκτέλεσης, CHANGELOG και QA checklist (US-03).

### Γνωστά θέματα

- Το CSRF είναι απενεργοποιημένο στο Sprint 1 (απόφαση καταγεγραμμένη στο `SecurityConfig`). Να επανεξεταστεί πριν από πραγματική χρήση.
- Το endpoint των στοιχείων χρήστη υποστηρίζει προσωρινά δύο paths (`/api/auth/me` και `/api/users/me`) μέχρι να αποφασίσει η ομάδα (issue #50).

<!-- TODO: πρόσθεσε εδώ τα σφάλματα που βρέθηκαν στο QA της Ημέρας 6 και δεν διορθώθηκαν. -->