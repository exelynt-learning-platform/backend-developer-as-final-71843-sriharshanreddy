package com.booking.service;

import com.booking.dto.ReservationRequest;
import com.booking.dto.ReservationResponse;
import com.booking.exception.AccessDeniedCustomException;
import com.booking.exception.BadRequestException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.model.ReservationStatus;
import com.booking.model.Reservation;
import com.booking.model.ResourceEntity;
import com.booking.repository.ReservationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;

    @Transactional
    public ReservationResponse create(ReservationRequest request, String currentUsername) {
        ResourceEntity resource = resourceService.findEntity(request.getResourceId());

        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource is not available for booking: " + resource.getName());
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .username(currentUsername) // identity always derived from JWT, never client input
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> search(ReservationStatus status,
                                             BigDecimal minPrice,
                                             BigDecimal maxPrice,
                                             String currentUsername,
                                             boolean isAdmin,
                                             Pageable pageable) {

        Specification<Reservation> spec = buildSpecification(status, minPrice, maxPrice,
                isAdmin ? null : currentUsername);

        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id, String currentUsername, boolean isAdmin) {
        Reservation reservation = findEntity(id);
        enforceOwnershipOrAdmin(reservation, currentUsername, isAdmin);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatus newStatus) {
        // Status changes are an ADMIN-only operation, enforced at controller level via @PreAuthorize
        Reservation reservation = findEntity(id);
        reservation.setStatus(newStatus);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request, String currentUsername, boolean isAdmin) {
        Reservation reservation = findEntity(id);
        enforceOwnershipOrAdmin(reservation, currentUsername, isAdmin);

        ResourceEntity resource = resourceService.findEntity(request.getResourceId());
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());

        // Only an admin may change status directly through the general update endpoint
        if (isAdmin && request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id, String currentUsername, boolean isAdmin) {
        Reservation reservation = findEntity(id);
        enforceOwnershipOrAdmin(reservation, currentUsername, isAdmin);
        reservationRepository.delete(reservation);
    }

    @Transactional
    public ReservationResponse cancel(Long id, String currentUsername, boolean isAdmin) {
        Reservation reservation = findEntity(id);
        enforceOwnershipOrAdmin(reservation, currentUsername, isAdmin);
        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    private void enforceOwnershipOrAdmin(Reservation reservation, String currentUsername, boolean isAdmin) {
        if (!isAdmin && !reservation.getUsername().equals(currentUsername)) {
            throw new AccessDeniedCustomException("You may only access your own reservations");
        }
    }

    private Reservation findEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private Specification<Reservation> buildSpecification(ReservationStatus status,
                                                            BigDecimal minPrice,
                                                            BigDecimal maxPrice,
                                                            String restrictToUsername) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (restrictToUsername != null) {
                predicates.add(cb.equal(root.get("username"), restrictToUsername));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource().getId())
                .resourceName(r.getResource().getName())
                .username(r.getUsername())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .price(r.getPrice())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
