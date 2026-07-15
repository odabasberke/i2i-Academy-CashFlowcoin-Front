package com.cryptopal.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the {@code users} table created in {@code db/init.sql}.
 * {@code @Setter} is on the whole class (not just individual fields)
 * because, unlike the append-only entities from Module 2, a user row is
 * legitimately mutated later (e.g. {@code lastLoginAt} on every login).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Generated in application code rather than left to the DB default, so
    // it's always available on the entity right after save() with no need
    // to re-select the row.
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Maps to the Postgres native ENUM `user_role`. NAMED_ENUM binds this as
    // the actual enum type (not varchar) - if a Hibernate/driver combo ever
    // rejects it, the documented fallback is storing role as VARCHAR with a
    // CHECK constraint instead of a native Postgres enum.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
