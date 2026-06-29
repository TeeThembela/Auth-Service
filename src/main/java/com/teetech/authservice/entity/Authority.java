package com.teetech.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "authorities",
        uniqueConstraints = @UniqueConstraint(columnNames = "name"),
        indexes = {
        @Index(name = "idx_authorities_name", columnList = "name")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Authority authority)) return false;
        return Objects.equals(name, authority.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
