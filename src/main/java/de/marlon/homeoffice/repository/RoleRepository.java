package de.marlon.homeoffice.repository;

import de.marlon.homeoffice.model.ERole;
import de.marlon.homeoffice.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}