import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, teamsApi } from '../api/teamsApi'
import { MyTeamsPage } from './MyTeamsPage'
import { TeamDetailPage } from './TeamDetailPage'

vi.mock('../api/teamsApi', async importOriginal => {
  const actual = await importOriginal()
  return {
    ...actual,
    teamsApi: {
      getMyTeams: vi.fn(),
      createTeam: vi.fn(),
      getTeamById: vi.fn(),
    },
  }
})

function renderWithRouter(initialEntries = ['/teams']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route path="/teams" element={<MyTeamsPage />} />
        <Route path="/teams/:id" element={<TeamDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('MyTeamsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('εμφανίζει τις ομάδες και τον ρόλο του χρήστη σε κάθε μία', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([
      { id: 1, name: 'Cipher Hunters', role: 'OWNER' },
      { id: 2, name: 'Clockwork Crew', role: 'MEMBER' },
    ])

    renderWithRouter()

    expect(await screen.findByText('Cipher Hunters')).toBeInTheDocument()
    expect(screen.getByText(/Διαχειριστής \(OWNER\)/i)).toBeInTheDocument()
    expect(screen.getByText('Clockwork Crew')).toBeInTheDocument()
    expect(screen.getByText(/Μέλος \(MEMBER\)/i)).toBeInTheDocument()
  })

  it('εμφανίζει κενή κατάσταση όταν ο χρήστης δεν έχει ομάδες', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])

    renderWithRouter()

    expect(await screen.findByText(/Δεν έχετε ακόμη ομάδες/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Ένταξη με κωδικό πρόσκλησης/i })).toBeInTheDocument()
  })

  it('εμφανίζει μήνυμα σφάλματος όταν αποτύχει η φόρτωση της λίστας ομάδων', async () => {
    vi.mocked(teamsApi.getMyTeams).mockRejectedValue(
      new ApiError(500, {
        code: 'INTERNAL_ERROR',
        message: 'Αποτυχία φόρτωσης ομάδων',
      }),
    )

    renderWithRouter()

    expect(await screen.findByRole('alert')).toHaveTextContent(/Αποτυχία φόρτωσης ομάδων/i)
  })

  it('μπλοκάρει όνομα μικρότερο από 3 χαρακτήρες', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])
    renderWithRouter()

    const input = await screen.findByLabelText(/Όνομα ομάδας/i)
    fireEvent.change(input, { target: { value: 'AB' } })
    fireEvent.click(screen.getByRole('button', { name: /Δημιουργία ομάδας/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/3 έως 50 χαρακτήρες/i)
    expect(teamsApi.createTeam).not.toHaveBeenCalled()
  })

  it('μπλοκάρει όνομα μεγαλύτερο από 50 χαρακτήρες', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])
    renderWithRouter()

    const tooLongName = 'A'.repeat(51)
    const input = await screen.findByLabelText(/Όνομα ομάδας/i)
    fireEvent.change(input, { target: { value: tooLongName } })
    fireEvent.click(screen.getByRole('button', { name: /Δημιουργία ομάδας/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/3 έως 50 χαρακτήρες/i)
    expect(teamsApi.createTeam).not.toHaveBeenCalled()
  })

  it('απορρίπτει όνομα που αποτελείται μόνο από κενά ή γίνεται μικρότερο από 3 χαρακτήρες μετά το trim', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])
    renderWithRouter()

    const input = await screen.findByLabelText(/Όνομα ομάδας/i)
    fireEvent.change(input, { target: { value: '  ab  ' } })
    fireEvent.click(screen.getByRole('button', { name: /Δημιουργία ομάδας/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/3 έως 50 χαρακτήρες/i)
    expect(teamsApi.createTeam).not.toHaveBeenCalled()
  })

  it('εμφανίζει σφάλμα πεδίου από το backend όταν επιστρέφει ApiError με fieldErrors', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])
    vi.mocked(teamsApi.createTeam).mockRejectedValue(
      new ApiError(400, {
        code: 'VALIDATION_ERROR',
        message: 'Σφάλμα εγκυρότητας',
        fieldErrors: { name: 'Team name must be between 3 and 50 characters' },
      }),
    )

    renderWithRouter()

    const input = await screen.findByLabelText(/Όνομα ομάδας/i)
    fireEvent.change(input, { target: { value: 'Escape Room Team' } })
    fireEvent.click(screen.getByRole('button', { name: /Δημιουργία ομάδας/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Team name must be between 3 and 50 characters',
    )
  })

  it('εμφανίζει γενικό σφάλμα backend όταν επιστρέφει ApiError χωρίς fieldErrors', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])
    vi.mocked(teamsApi.createTeam).mockRejectedValue(
      new ApiError(500, {
        code: 'INTERNAL_SERVER_ERROR',
        message: 'Παρουσιάστηκε σφάλμα διακομιστή κατά τη δημιουργία ομάδας',
      }),
    )

    renderWithRouter()

    const input = await screen.findByLabelText(/Όνομα ομάδας/i)
    fireEvent.change(input, { target: { value: 'Escape Room Team' } })
    fireEvent.click(screen.getByRole('button', { name: /Δημιουργία ομάδας/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Παρουσιάστηκε σφάλμα διακομιστή κατά τη δημιουργία ομάδας',
    )
  })

  it('μεταβαίνει στη σελίδα της νέας ομάδας μετά τη δημιουργία', async () => {
    vi.mocked(teamsApi.getMyTeams).mockResolvedValue([])
    vi.mocked(teamsApi.createTeam).mockResolvedValue({
      id: 42,
      name: 'Room 42 Escape',
      inviteCode: 'XK92LP4M',
      role: 'OWNER',
    })
    vi.mocked(teamsApi.getTeamById).mockResolvedValue({
      id: 42,
      name: 'Room 42 Escape',
      inviteCode: 'XK92LP4M',
      role: 'OWNER',
    })

    renderWithRouter()

    const input = await screen.findByLabelText(/Όνομα ομάδας/i)
    fireEvent.change(input, { target: { value: 'Room 42 Escape' } })
    fireEvent.click(screen.getByRole('button', { name: /Δημιουργία ομάδας/i }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Room 42 Escape' })).toBeInTheDocument()
    })
    expect(screen.getByText('XK92LP4M')).toBeInTheDocument()
  })
})
