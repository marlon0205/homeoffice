package de.marlon.homeoffice.service;

import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Für Passworthashing

    @Transactional
    public User createUser(String username, String password, User.UserRole role, String firstName, String lastName) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Benutzername existiert bereits: " + username);
        }
        User newUser = new User(username, passwordEncoder.encode(password), role, firstName, lastName);
        return userRepository.save(newUser);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllUsersByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User updateUserPersonalMaxHoDays(UUID userId, Integer maxDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden: " + userId));

        // Die Validierung des Wertbereichs könnte auch hier oder im DTO erfolgen
        if (maxDays != null && (maxDays < 0 || maxDays > 7)) {
            throw new IllegalArgumentException("Maximale HO-Tage müssen zwischen 0 und 7 liegen.");
        }

        user.setPersonalMaxHoDaysPerWeek(maxDays);
        return userRepository.save(user);
    }
}