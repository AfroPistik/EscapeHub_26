import { validateRegister } from './validation'

test('δέχεται έγκυρα δεδομένα', () => {
    expect(validateRegister({ email: 'a@b.gr', username: 'abc', password: '12345678' })).toEqual({})
})
test('απορρίπτει κοντό κωδικό, κοντό username και άκυρο email', () => {
    const e = validateRegister({ email: 'x', username: 'ab', password: '123' })
    expect(Object.keys(e)).toEqual(['email', 'username', 'password'])
})