// src/main/java/com/yourcompany/homeofficecalendar/repository/GroupRepository.java
package de.marlon.homeoffice.repository;

import de.marlon.homeoffice.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    Optional<Group> findByName(String name);
}