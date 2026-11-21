package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Compte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String surname;
    private Integer age;
    private String profession;
    private AccountType typeCompte;

    @OneToMany(mappedBy = "compte")
    @JsonIgnore
    private Set<Reservation> reservations;

    // News items submitted by this account (admins)
    @OneToMany(mappedBy = "submittedBy")
    @JsonIgnore
    private Set<News> newsSubmissions;

    // Coworking spaces owned by this account (owners)
    @OneToMany(mappedBy = "owner")
    @JsonIgnore
    private Set<CoworkingSpace> ownedSpaces;
}
