package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    private Date date;

    private String time;
    private Integer hours;
    private Integer numberOfPeople;
    private String statusChangeReason;
    private double totalPrice;

    @Enumerated(EnumType.ORDINAL)
    private ReservationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id")
    @JsonIgnore
    private Compte compte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coworking_space_id")
    @JsonIgnore
    private CoworkingSpace coworkingSpace;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "reservation_equipment",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    @JsonIgnore
    private Set<Equipment> reservedEquipments = new HashSet<>();
}
