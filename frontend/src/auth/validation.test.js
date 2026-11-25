import { test, expect } from 'vitest'
import { validateRegister } from './validation'

test('δέχεται έγκυρα δεδομένα', () => {
    expect(validateRegister({ email: 'a@b.gr', username: 'abc', password: '12345678' })).toEqual({})
})

test('απορρίπτει άκυρο email, κοντό username και κοντό κωδικό', () => {
    const errors = validateRegister({ email: 'x', username: 'ab', password: '123' })
    expect(Object.keys(errors)).toEqual(['email', 'username', 'password'])
})

test('απορρίπτει username μεγαλύτερο από 30 χαρακτήρες', () => {
    const errors = validateRegister({ email: 'a@b.gr', username: 'a'.repeat(31), password: '12345678' })
    expect(errors.username).toBeDefined()
})