package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    // Same "yyyy-MM-dd" shape on create and on read (a DATE column comes back as java.sql.Date).
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @JsonIgnore
    private Room room;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "reservation_equipment",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    @JsonIgnore
    private Set<Equipment> reservedEquipments = new HashSet<>();

    // Reading the id of a lazy association does not load it, so these are safe outside a session.
    @JsonProperty(value = "userId", access = JsonProperty.Access.READ_ONLY)
    public Long getUserId() {
        return compte == null ? null : compte.getId();
    }

    @JsonProperty(value = "coworkingSpaceId", access = JsonProperty.Access.READ_ONLY)
    public Long getCoworkingSpaceId() {
        return coworkingSpace == null ? null : coworkingSpace.getId();
    }

    @JsonProperty(value = "roomId", access = JsonProperty.Access.READ_ONLY)
    public Long getRoomId() {
        return room == null ? null : room.getId();
    }
}
