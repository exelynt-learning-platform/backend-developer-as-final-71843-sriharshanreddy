package com.booking.controller;

import com.booking.dto.ReservationRequest;
import com.booking.dto.ReservationResponse;
import com.booking.dto.ReservationStatusUpdateRequest;
import com.booking.model.ReservationStatus;
import com.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Bookings for resources")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation for the authenticated user (owner is always taken from the JWT)")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                        Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(201).body(reservationService.create(request, username));
    }

    @GetMapping
    @Operation(summary = "List reservations. ADMIN sees all, USER sees only their own. " +
            "Supports filtering by status/minPrice/maxPrice, pagination, and sorting.")
    public ResponseEntity<Page<ReservationResponse>> search(
            @Parameter(description = "Filter by reservation status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            Authentication authentication) {

        boolean isAdmin = isAdmin(authentication);
        String username = authentication.getName();

        return ResponseEntity.ok(reservationService.search(status, minPrice, maxPrice, username, isAdmin, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single reservation. USER may only view their own; ADMIN may view any.")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.getById(id, authentication.getName(), isAdmin));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation. USER may update only their own (status changes ignored); ADMIN may update any including status.")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ReservationRequest request,
                                                        Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.update(id, request, authentication.getName(), isAdmin));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update only the status of a reservation (ADMIN only)")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody ReservationStatusUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation. USER may cancel only their own; ADMIN may cancel any.")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.cancel(id, authentication.getName(), isAdmin));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation. USER may delete only their own; ADMIN may delete any.")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        reservationService.delete(id, authentication.getName(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
