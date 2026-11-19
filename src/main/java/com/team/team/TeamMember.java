package com.team.team;

import com.team.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "team_members")
public class TeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "team_id")
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TeamRole role;
    @Column(name = "joined_at", nullable = false, insertable = false, updatable = false)
    private Instant joinedAt;
    public Long getId() { return id; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
}