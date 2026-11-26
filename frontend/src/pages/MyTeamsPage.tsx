import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, teamsApi } from '../api/teamsApi'
import { CreateTeamForm } from '../components/CreateTeamForm'
import { EmptyTeamsState } from '../components/EmptyTeamsState'

const ROLE_LABELS = {
  OWNER: 'Διαχειριστής (Owner)',
  MEMBER: 'Μέλος (Member)',
}

export function MyTeamsPage() {
  const [teams, setTeams] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const navigate = useNavigate()
  const formWrapperRef = useRef(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    teamsApi
      .getMyTeams()
      .then(data => {
        if (active) {
          setTeams(data)
          setError(null)
        }
      })
      .catch(err => {
        if (!active) return
        setError(err instanceof ApiError ? err.message : 'Σφάλμα φόρτωσης ομάδων')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  function handleCreated(created) {
    navigate(`/teams/${created.id}`)
  }

  function focusCreateInput() {
    const input = formWrapperRef.current?.querySelector('input[name="name"]')
    input?.focus()
  }

  return (
    <main>
      <h1>Οι ομάδες μου</h1>

      <div ref={formWrapperRef}>
        <CreateTeamForm onCreated={handleCreated} />
      </div>

      {loading && <p>Φόρτωση ομάδων...</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && teams.length === 0 && (
        <EmptyTeamsState onFocusCreate={focusCreateInput} />
      )}

      {!loading && !error && teams.length > 0 && (
        <ul>
          {teams.map(team => (
            <li key={team.id}>
              <Link to={`/teams/${team.id}`}>{team.name}</Link>
              <span> — Ρόλος: {ROLE_LABELS[team.role] ?? team.role}</span>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
