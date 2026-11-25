export type TeamRole = 'OWNER' | 'MEMBER'

export interface TeamSummary {
  id: number
  name: string
  role: TeamRole
  inviteCode?: string
}

export interface TeamDetails {
  id: number
  name: string
  inviteCode: string
  role?: TeamRole
  createdAt?: string
}

export interface ApiErrorPayload {
  code?: string
  message: string
  timestamp?: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  status: number
  code?: string
  fieldErrors: Record<string, string>

  constructor(status: number, payload: Partial<ApiErrorPayload> & { message: string }) {
    super(payload.message)
    this.status = status
    this.code = payload.code
    this.fieldErrors = payload.fieldErrors ?? {}
  }
}

const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:4010'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const data = await response.json().catch(() => ({}))

  if (!response.ok) {
    throw new ApiError(response.status, {
      code: data.code,
      message: data.message ?? 'Προέκυψε σφάλμα από τον διακομιστή',
      fieldErrors: data.fieldErrors,
    })
  }

  return data as T
}

export const teamsApi = {
  getMyTeams: () => request<TeamSummary[]>('/api/teams/mine'),
  createTeam: (name: string) =>
    request<TeamDetails>('/api/teams', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
  getTeamById: (id: number | string) => request<TeamDetails>(`/api/teams/${id}`),
}
