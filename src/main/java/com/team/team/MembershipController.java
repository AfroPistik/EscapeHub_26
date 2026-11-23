
package com.team.team;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/join")
    public ResponseEntity<TeamResponse> join(@Valid @RequestBody JoinTeamRequest request) {
        return ResponseEntity.ok(membershipService.join(request.inviteCode()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(membershipService.getMembers(id));
    }

    @DeleteMapping("/{id}/members/me")
    public ResponseEntity<Void> leave(@PathVariable Long id) {
        membershipService.leave(id);
        return ResponseEntity.noContent().build();
    }
}
