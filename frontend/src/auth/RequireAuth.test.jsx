import { test, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import RequireAuth from './RequireAuth'
import { useAuth } from './AuthContext'

vi.mock('./AuthContext', () => ({ useAuth: vi.fn() }))

function renderGuard() {
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
    useAuth.mockReturnValue({ user: null, loading: false })
    renderGuard()
    expect(screen.getByText('login page')).toBeInTheDocument()
})

test('με session δείχνει τη σελίδα', () => {
    useAuth.mockReturnValue({ user: { id: 1 }, loading: false })
    renderGuard()
    expect(screen.getByText('secret')).toBeInTheDocument()
})

test('όσο φορτώνει δεν ανακατευθύνει', () => {
    useAuth.mockReturnValue({ user: null, loading: true })
    renderGuard()
    expect(screen.queryByText('login page')).not.toBeInTheDocument()
})