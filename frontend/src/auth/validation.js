export function validateRegister({ email, username, password }) {
    const fieldErrors = {}
    if (!/^\S+@\S+\.\S+$/.test(email)) {
        fieldErrors.email = 'Μη έγκυρο email'
    }
    if (username.length < 3 || username.length > 30) {
        fieldErrors.username = 'Το username πρέπει να έχει 3–30 χαρακτήρες'
    }
    if (password.length < 8) {
        fieldErrors.password = 'Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες'
    }
    return fieldErrors
}