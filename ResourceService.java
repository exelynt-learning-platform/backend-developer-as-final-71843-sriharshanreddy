package com.booking.service;

import com.booking.dto.ResourceRequest;
import com.booking.dto.ResourceResponse;
import com.booking.exception.ResourceNotFoundException;
import com.booking.model.ResourceEntity;
import com.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        ResourceEntity entity = ResourceEntity.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .available(request.getAvailable() == null || request.getAvailable())
                .build();
        return toResponse(resourceRepository.save(entity));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        ResourceEntity entity = findEntity(id);
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setDescription(request.getDescription());
        if (request.getAvailable() != null) {
            entity.setAvailable(request.getAvailable());
        }
        return toResponse(resourceRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ResourceEntity entity = findEntity(id);
        resourceRepository.delete(entity);
    }

    public ResourceEntity findEntity(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private ResourceResponse toResponse(ResourceEntity entity) {
        return ResourceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .description(entity.getDescription())
                .available(entity.isAvailable())
                .build();
    }
}
