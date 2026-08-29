package com.booking.dto;

import com.booking.model.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateRequest {

    @NotNull(message = "status is required")
    private ReservationStatus status;
}
