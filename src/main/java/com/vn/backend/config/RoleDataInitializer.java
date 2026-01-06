package com.vn.backend.config;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.vn.backend.model.Role;
import com.vn.backend.repository.RoleRepository;
import com.vn.backend.util.enums.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting role initialization...");


        Arrays.stream(role.values()).forEach(roleEnum -> {
            String roleName = roleEnum.name();

            roleRepository.findByName(roleName).ifPresentOrElse(
                existingRole -> {
                    log.debug("Role {} already exists with ID: {}", roleName, existingRole.getId());
                },
                () -> {
                    Role newRole = Role.builder()
                            .name(roleName)
                            .description("Default " + roleName.replace("_", " ").toLowerCase() + " role")
                            .build();

                    Role savedRole = roleRepository.save(newRole);
                    log.info("Created new role: {} with ID: {}", roleName, savedRole.getId());
                }
            );
        });

        log.info("Role initialization completed. Total roles in database: {}", roleRepository.count());
    }
}
