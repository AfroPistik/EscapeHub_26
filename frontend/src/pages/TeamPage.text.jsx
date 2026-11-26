import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import TeamPage from './TeamPage';

const mockTeamData = {
  id: 'team-1',
  name: 'Alpha Developers',
  inviteCode: 'ALPHA-2026',
  members: [
    { id: 'user-owner', name: 'Γιώργος (Owner)', role: 'OWNER' },
    { id: 'user-member-1', name: 'Μαρία', role: 'MEMBER' },
    { id: 'user-member-2', name: 'Κώστας', role: 'MEMBER' },
  ],
};

describe('TeamPage Component', () => {
  let mockApiClient;
  let mockOnRedirect;

  beforeEach(() => {
    mockApiClient = {
      getTeam: jest.fn().mockResolvedValue(mockTeamData),
      removeMember: jest.fn().mockResolvedValue({}),
      leaveTeam: jest.fn().mockResolvedValue({}),
    };
    mockOnRedirect = jest.fn();

    // Mock clipboard API
    Object.assign(navigator, {
      clipboard: {
        writeText: jest.fn().mockResolvedValue(),
      },
    });
  });

  test('εμφανίζει όνομα ομάδας, invite code (με κουμπί αντιγραφής) και μέλη με ρόλους', async () => {
    render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-member-1"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    expect(await screen.findByText('Alpha Developers')).toBeInTheDocument();
    expect(screen.getByText('ALPHA-2026')).toBeInTheDocument();

    // Έλεγχος κουμπιού αντιγραφής
    const copyButton = screen.getByRole('button', { name: /Αντιγραφή/i });
    fireEvent.click(copyButton);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('ALPHA-2026');

    // Έλεγχος εμφάνισης μελών και ρόλων
    expect(screen.getByText('Γιώργος (Owner)')).toBeInTheDocument();
    expect(screen.getByText('OWNER')).toBeInTheDocument();
    expect(screen.getByText('Μαρία')).toBeInTheDocument();
    expect(screen.getAllByText('MEMBER')).toHaveLength(2);
  });

  test('ως OWNER: εμφανίζει κουμπί «Αφαίρεση» ΜΟΝΟ για τα άλλα μέλη και ΟΧΙ κουμπί «Αποχώρηση»', async () => {
    render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-owner"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    await screen.findByText('Alpha Developers');

    // Υπάρχουν 2 άλλα μέλη -> πρέπει να υπάρχουν ακριβώς 2 κουμπιά «Αφαίρεση»
    const removeButtons = screen.getAllByRole('button', { name: 'Αφαίρεση' });
    expect(removeButtons).toHaveLength(2);

    // Ο owner ΔΕΝ πρέπει να βλέπει κουμπί «Αποχώρηση»
    expect(screen.queryByRole('button', { name: 'Αποχώρηση' })).not.toBeInTheDocument();
  });

  test('ως MEMBER: εμφανίζει κουμπί «Αποχώρηση» και ΚΑΝΕΝΑ κουμπί «Αφαίρεση»', async () => {
    render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-member-1"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    await screen.findByText('Alpha Developers');

    // Το MEMBER βλέπει το κουμπί «Αποχώρηση»
    expect(screen.getByRole('button', { name: 'Αποχώρηση' })).toBeInTheDocument();

    // Το MEMBER ΔΕΝ βλέπει κανένα κουμπί «Αφαίρεση»
    expect(screen.queryByRole('button', { name: 'Αφαίρεση' })).not.toBeInTheDocument();
  });

  test('μετά από αφαίρεση μέλους ενημερώνεται η λίστα', async () => {
    render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-owner"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    await screen.findByText('Μαρία');
    const removeButtons = screen.getAllByRole('button', { name: 'Αφαίρεση' });

    // Πατάμε αφαίρεση στο πρώτο member (Μαρία)
    fireEvent.click(removeButtons[0]);

    await waitFor(() => {
      expect(mockApiClient.removeMember).toHaveBeenCalledWith('team-1', 'user-member-1');
      expect(screen.queryByText('Μαρία')).not.toBeInTheDocument();
    });
  });

  test('μετά από αποχώρηση μέλους γίνεται ανακατεύθυνση', async () => {
    render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-member-1"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    const leaveButton = await screen.findByRole('button', { name: 'Αποχώρηση' });
    fireEvent.click(leaveButton);

    await waitFor(() => {
      expect(mockApiClient.leaveTeam).toHaveBeenCalledWith('team-1');
      expect(mockOnRedirect).toHaveBeenCalledTimes(1);
    });
  });

  test('εμφανίζει κατάλληλο μήνυμα σε σφάλμα 403 και 409 από το backend', async () => {
    // 1. Έλεγχος 403 κατά την αφαίρεση
    mockApiClient.removeMember.mockRejectedValueOnce({ status: 403 });

    const { unmount } = render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-owner"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    const removeButtons = await screen.findAllByRole('button', { name: 'Αφαίρεση' });
    fireEvent.click(removeButtons[0]);

    expect(await screen.findByRole('alert')).toHaveTextContent(/Σφάλμα 403/);
    unmount();

    // 2. Έλεγχος 409 κατά την αποχώρηση
    mockApiClient.leaveTeam.mockRejectedValueOnce({ status: 409 });

    render(
      <TeamPage
        teamId="team-1"
        currentUserId="user-member-1"
        apiClient={mockApiClient}
        onRedirect={mockOnRedirect}
      />
    );

    const leaveButton = await screen.findByRole('button', { name: 'Αποχώρηση' });
    fireEvent.click(leaveButton);

    expect(await screen.findByRole('alert')).toHaveTextContent(/Σφάλμα 409/);
  });
});
