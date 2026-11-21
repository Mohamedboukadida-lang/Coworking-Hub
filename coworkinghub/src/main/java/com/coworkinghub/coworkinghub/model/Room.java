package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int capacity;
    private double pricePerPersonPerHour;

    @ManyToOne
    @JoinColumn(name = "coworking_space_id")
    @JsonIgnore
    private CoworkingSpace coworkingSpace;
}
