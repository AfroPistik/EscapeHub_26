import React, { useState, useEffect } from 'react';

export default function TeamPage({
  teamId,
  currentUserId,
  apiClient,       // Αντικείμενο/service για τα API calls (π.χ. axios ή custom fetch wrapper)
  onRedirect       // Callback για ανακατεύθυνση μετά από αποχώρηση (π.χ. navigate('/teams'))
}) {
  const [team, setTeam] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [copied, setCopied] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // Φόρτωση δεδομένων ομάδας
  useEffect(() => {
    let isMounted = true;

    async function fetchTeam() {
      setLoading(true);
      setErrorMessage('');
      try {
        const data = await apiClient.getTeam(teamId);
        if (isMounted) {
          setTeam(data);
        }
      } catch (err) {
        if (isMounted) {
          handleApiError(err, 'Αποτυχία φόρτωσης της ομάδας.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    fetchTeam();
    return () => {
      isMounted = false;
    };
  }, [teamId, apiClient]);

  // Κεντρικός χειρισμός σφαλμάτων (με ειδική μέριμνα για 403 & 409)
  const handleApiError = (err, fallbackMessage) => {
    const status = err?.status || err?.response?.status;
    if (status === 403) {
      setErrorMessage('Σφάλμα 403: Δεν έχετε δικαίωμα να εκτελέσετε αυτή την ενέργεια.');
    } else if (status === 409) {
      setErrorMessage('Σφάλμα 409: Σύγκρουση κατάστασης (π.χ. ο owner δεν μπορεί να αποχωρήσει ή η ενέργεια έχει ήδη γίνει).');
    } else {
      setErrorMessage(err?.message || fallbackMessage);
    }
  };

  // Αντιγραφή Invite Code στο clipboard
  const handleCopyInviteCode = async () => {
    if (!team?.inviteCode) return;
    try {
      await navigator.clipboard.writeText(team.inviteCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setErrorMessage('Αδυναμία αντιγραφής του κωδικού πρόσκλησης.');
    }
  };

  // Αφαίρεση μέλους (μόνο για OWNER)
  const handleRemoveMember = async (memberId) => {
    setErrorMessage('');
    setActionLoading(true);
    try {
      await apiClient.removeMember(teamId, memberId);
      // Ενημέρωση της λίστας μελών τοπικά
      setTeam((prevTeam) => ({
        ...prevTeam,
        members: prevTeam.members.filter((m) => m.id !== memberId),
      }));
    } catch (err) {
      handleApiError(err, 'Αποτυχία αφαίρεσης του μέλους.');
    } finally {
      setActionLoading(false);
    }
  };

  // Αποχώρηση από την ομάδα (μόνο για MEMBER)
  const handleLeaveTeam = async () => {
    setErrorMessage('');
    setActionLoading(true);
    try {
      await apiClient.leaveTeam(teamId);
      // Ανακατεύθυνση μετά την επιτυχή αποχώρηση
      if (onRedirect) {
        onRedirect();
      }
    } catch (err) {
      handleApiError(err, 'Αποτυχία αποχώρησης από την ομάδα.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return <div role="status">Φόρτωση στοιχείων ομάδας...</div>;
  }

  if (!team) {
    return (
      <div>
        {errorMessage && <div role="alert" style={{ color: 'red' }}>{errorMessage}</div>}
        <p>Η ομάδα δεν βρέθηκε.</p>
      </div>
    );
  }

  // Εύρεση του τρέχοντος χρήστη και του ρόλου του
  const currentMember = team.members.find((m) => m.id === currentUserId);
  const currentUserRole = currentMember?.role; // 'OWNER' ή 'MEMBER'
  const isOwner = currentUserRole === 'OWNER';
  const isMember = currentUserRole === 'MEMBER';

  return (
    <div className="team-page">
      {/* Banner Σφαλμάτων (403, 409 κ.λπ.) */}
      {errorMessage && (
        <div
          role="alert"
          style={{
            padding: '12px',
            marginBottom: '16px',
            backgroundColor: '#fee2e2',
            color: '#991b1b',
            border: '1px solid #f87171',
            borderRadius: '4px',
          }}
        >
          {errorMessage}
        </div>
      )}

      {/* Header Ομάδας & Invite Code */}
      <header style={{ marginBottom: '24px' }}>
        <h1>{team.name}</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>
            Invite Code: <strong>{team.inviteCode}</strong>
          </span>
          <button type="button" onClick={handleCopyInviteCode}>
            {copied ? 'Αντιγράφηκε!' : 'Αντιγραφή'}
          </button>
        </div>
      </header>

      {/* Λίστα Μελών */}
      <section>
        <h2>Μέλη Ομάδας ({team.members.length})</h2>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {team.members.map((member) => {
            const isSelf = member.id === currentUserId;
            // Κανόνας: Το κουμπί «Αφαίρεση» εμφανίζεται ΜΟΝΟ στον owner και ΜΟΝΟ για άλλα μέλη
            const canRemoveThisMember = isOwner && !isSelf;

            return (
              <li
                key={member.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '8px 0',
                  borderBottom: '1px solid #e5e7eb',
                }}
              >
                <div>
                  <span>{member.name}</span>{' '}
                  <span
                    style={{
                      fontSize: '0.85em',
                      padding: '2px 6px',
                      backgroundColor: '#f3f4f6',
                      borderRadius: '4px',
                    }}
                  >
                    {member.role}
                  </span>
                </div>

                {canRemoveThisMember && (
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={() => handleRemoveMember(member.id)}
                  >
                    Αφαίρεση
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      </section>

      {/* Κουμπί Αποχώρησης (ΜΟΝΟ για MEMBER, ποτέ για OWNER) */}
      {isMember && (
        <div style={{ marginTop: '24px' }}>
          <button
            type="button"
            disabled={actionLoading}
            onClick={handleLeaveTeam}
            style={{ backgroundColor: '#dc2626', color: 'white', padding: '8px 16px' }}
          >
            Αποχώρηση
          </button>
        </div>
      )}
    </div>
  );
}
