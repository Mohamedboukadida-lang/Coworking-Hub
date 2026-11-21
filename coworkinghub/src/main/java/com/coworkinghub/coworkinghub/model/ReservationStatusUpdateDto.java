package com.coworkinghub.coworkinghub.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateDto {
    @NotNull
    private ReservationStatus status;
    private String reason;
}
