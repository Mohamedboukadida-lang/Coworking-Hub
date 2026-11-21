package com.coworkinghub.coworkinghub.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private Date date;
    private String time;

    // Both are needed to price the reservation.
    @NotNull
    @Positive
    private Integer hours;

    @NotNull
    @Positive
    private Integer numberOfPeople;

    @NotNull
    private Long coworkingSpaceId;

    private Set<Long> equipmentIds;
}
