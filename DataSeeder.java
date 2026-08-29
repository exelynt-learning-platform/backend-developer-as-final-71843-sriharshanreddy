package com.booking.config;

import com.booking.model.ResourceEntity;
import com.booking.model.Role;
import com.booking.model.User;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build());
        }
        if (!userRepository.existsByUsername("user")) {
            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("User@123"))
                    .role(Role.USER)
                    .build());
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            resourceRepository.save(ResourceEntity.builder()
                    .name("Conference Room A")
                    .type("ROOM")
                    .description("Large conference room with projector, seats 12")
                    .available(true)
                    .build());
            resourceRepository.save(ResourceEntity.builder()
                    .name("Toyota Camry - Fleet Car 1")
                    .type("VEHICLE")
                    .description("Company sedan for local trips")
                    .available(true)
                    .build());
            resourceRepository.save(ResourceEntity.builder()
                    .name("Projector Kit")
                    .type("EQUIPMENT")
                    .description("Portable HD projector with screen and cables")
                    .available(true)
                    .build());
        }
    }
}
