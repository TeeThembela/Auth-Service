package com.teetech.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = "token"),
        indexes = @Index(name = "idx_refresh_token", columnList = "token")
)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false, columnDefinition = "Text")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
