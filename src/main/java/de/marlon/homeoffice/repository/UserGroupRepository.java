// src/main/java/com/yourcompany/homeofficecalendar/repository/UserGroupRepository.java
package de.marlon.homeoffice.repository;

import de.marlon.homeoffice.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroup.UserGroupId> {
    List<UserGroup> findByUserId(UUID userId);
    List<UserGroup> findByGroupId(UUID groupId);
    void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}