package de.marlon.homeoffice.repository;

import de.marlon.homeoffice.model.User;
import de.marlon.homeoffice.model.UserHomeofficeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserHomeofficeSettingsRepository extends JpaRepository<UserHomeofficeSettings, Long> {
    Optional<UserHomeofficeSettings> findByUser(User user);
}