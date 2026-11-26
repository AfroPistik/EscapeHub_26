import { api } from './client'

export const teamsApi = {
  getMyTeams: () => api.get('/teams/mine'),
  createTeam: (name) => api.post('/teams', { name }),
  getTeamById: (id) => api.get(`/teams/${id}`),
}
