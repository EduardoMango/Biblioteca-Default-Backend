package com.EduardoMango.Biblioteca.model.entity;

import com.EduardoMango.Biblioteca.model.enums.Roles;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Roles role;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permits",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permit_id")
    )
    @Builder.Default
    private Set<PermitEntity> permits = new HashSet<>();

    public RoleEntity(Roles role) {
        this.role = role;
        this.permits = new HashSet<>();
    }

    public void addPermit(PermitEntity permit) {
        if (this.permits == null) {
            this.permits = new HashSet<>();
        }
        this.permits.add(permit);
    }
}

