package com.banking.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false, unique = true)
    private RoleType name;

    public Role() {}

    public Role(RoleType name) {
        this.name = name;
    }

    public Role(Long id, RoleType name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RoleType getName() { return name; }
    public void setName(RoleType name) { this.name = name; }
}
