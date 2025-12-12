package com.omis5.authenticationService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class RefreshToken {

    @Id
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @OneToOne()
    @JoinColumn(name = "user_id",table = "authentication_users",referencedColumnName = "id")
    private AuthenticationUser user;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
