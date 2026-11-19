import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import RequireAuth from './RequireAuth'
import * as AuthModule from './AuthContext'

const renderAt = (authValue) => {
    vi.spyOn(AuthModule, 'useAuth').mockReturnValue(authValue)
    render(
        <MemoryRouter initialEntries={['/teams']}>
            <Routes>
                <Route path="/login" element={<p>login page</p>} />
                <Route path="/teams" element={<RequireAuth><p>secret</p></RequireAuth>} />
            </Routes>
        </MemoryRouter>
    )
}

test('χωρίς session ανακατευθύνει στο /login', () => {
    renderAt({ user: null, loading: false })
    expect(screen.getByText('login page')).toBeInTheDocument()
})
test('με session δείχνει τη σελίδα', () => {
    renderAt({ user: { id: 1 }, loading: false })
    expect(screen.getByText('secret')).toBeInTheDocument()
})