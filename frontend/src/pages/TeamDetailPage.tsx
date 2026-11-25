import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, TeamDetails, teamsApi } from '../api/teamsApi'

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [team, setTeam] = useState<TeamDetails | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let active = true
    setLoading(true)
    teamsApi
      .getTeamById(id)
      .then(data => {
        if (active) {
          setTeam(data)
          setError(null)
        }
      })
      .catch(err => {
        if (!active) return
        setError(err instanceof ApiError ? err.message : 'Αδυναμία φόρτωσης ομάδας')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [id])

  if (loading) return <p>Φόρτωση στοιχείων ομάδας...</p>
  if (error) {
    return (
      <div>
        <p role="alert">{error}</p>
        <Link to="/teams">Επιστροφή στις ομάδες μου</Link>
      </div>
    )
  }
  if (!team) return null

  return (
    <main>
      <Link to="/teams">← Πίσω στις ομάδες μου</Link>
      <h1>{team.name}</h1>
      <p>
        Κωδικός πρόσκλησης: <strong>{team.inviteCode}</strong>
      </p>
      {team.role && <p>Ρόλος: {team.role}</p>}
    </main>
  )
}
