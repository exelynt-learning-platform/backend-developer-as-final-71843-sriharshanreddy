package com.booking.controller;

import com.booking.dto.ResourceRequest;
import com.booking.dto.ResourceResponse;
import com.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Bookable resources: rooms, vehicles, equipment")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "List all resources (paginated) - available to any authenticated user")
    public ResponseEntity<Page<ResourceResponse>> getAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(resourceService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resource by id - available to any authenticated user")
    public ResponseEntity<ResourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(201).body(resourceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
