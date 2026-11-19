# 001. Επιλογή Session-based Authentication έναντι JWT

* Status: accepted
* Date: 2026-11-19
* Context: Στο EscapeHub χρειαζόμαστε έναν μηχανισμό πιστοποίησης χρηστών (authentication) για την ασφάλεια των endpoints και τη διαχείριση των συνεδριών.
* Decision: Επιλέξαμε τη χρήση Session-based authentication (Spring Security sessions) αντί για JSON Web Tokens (JWT).
* Consequences:
  * Θετικά: Απλούστερη διαχείριση της ανάκλησης (revocation) και του logout από την πλευρά του server, ασφαλέστερη αποθήκευση cookies (HttpOnly).
  * Αρνητικά: Ο server διατηρεί την κατάσταση της σύνδεσης (stateful), κάτι που απαιτεί προσοχή σε σενάρια οριζόντιας κλιμάκωσης (scaling), αλλά δεν αποτελεί περιορισμό για το EscapeHub καθώς το σύστημα προβλέπεται να τρέχει σε μία instance (single instance).