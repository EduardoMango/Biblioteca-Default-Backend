package com.EduardoMango.Biblioteca.feature.auth.domain;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Permits permit;

    public PermitEntity(Permits permit) {
        this.permit = permit;
    }
}

