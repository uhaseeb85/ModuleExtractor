package com.example.common.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Shared entity — intentionally imported by multiple services (cross-repo coupling antipattern).
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

    // Getters / setters omitted for brevity (test fixture)
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
