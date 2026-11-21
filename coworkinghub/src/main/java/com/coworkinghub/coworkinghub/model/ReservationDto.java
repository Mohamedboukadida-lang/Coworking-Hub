package com.coworkinghub.coworkinghub.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    // Date and start time are needed to detect overlapping bookings.
    @NotNull
    private LocalDate date;

    @NotNull
    @Pattern(regexp = "([01]?\\d|2[0-3]):[0-5]\\d", message = "must be a time of day in HH:mm format")
    private String time;

    // Both are needed to price the reservation.
    @NotNull
    @Positive
    @Max(24)
    private Integer hours;

    @NotNull
    @Positive
    private Integer numberOfPeople;

    @NotNull
    private Long coworkingSpaceId;

    private Set<Long> equipmentIds;
}
