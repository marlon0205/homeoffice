package de.marlon.homeoffice.service;

import de.marlon.homeoffice.model.*;
import de.marlon.homeoffice.repository.RoleRepository;
import de.marlon.homeoffice.repository.UserHomeofficeSettingsRepository;
import de.marlon.homeoffice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder; // Brauchen wir hier nicht mehr für Auth, aber vllt. für Dummypasswörter
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserHomeofficeSettingsRepository userHomeofficeSettingsRepository;
    private final RoleRepository roleRepository;
    // PasswordEncoder wird hier nicht für das Hashing von Passwörtern beim Login verwendet,
    // sondern nur, falls du ein Dummy-Passwort für lokal angelegte User hashen willst.
    // Mit Authentik-Integration ist es meist überflüssig.
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserHomeofficeSettingsRepository userHomeofficeSettingsRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userHomeofficeSettingsRepository = userHomeofficeSettingsRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Neu hinzugefügt: Methode, um User nach Username zu finden (wichtig für CustomOAuth2UserService)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Nur für administrative Zwecke oder Initialisierung, da Registrierung über Authentik läuft
    @Transactional
    public User createUser(String username, String email, String password, Set<ERole> roles) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use!");
        }

        User newUser = new User(username, email, passwordEncoder.encode(password));
        Set<Role> userRoles = new java.util.HashSet<>();

        roles.forEach(roleName -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Error: Role " + roleName + " is not found."));
            userRoles.add(role);
        });

        newUser.setRoles(userRoles);
        return userRepository.save(newUser);
    }

    @Transactional
    public User updateUser(Long userId, String newUsername, String newEmail, Group newGroup) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (newUsername != null && !newUsername.isEmpty() && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new IllegalArgumentException("Username is already taken!");
            }
            user.setUsername(newUsername);
        }
        if (newEmail != null && !newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email is already in use!");
            }
            user.setEmail(newEmail);
        }
        if (newGroup != null) {
            user.setGroup(newGroup);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    @Transactional
    public UserHomeofficeSettings updateUserHomeofficeSettings(Long userId, Integer maxDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserHomeofficeSettings settings = userHomeofficeSettingsRepository.findByUser(user)
                .orElse(new UserHomeofficeSettings());

        settings.setUser(user);
        settings.setMaxHomeofficeDaysPerWeek(maxDays);
        return userHomeofficeSettingsRepository.save(settings);
    }

    public Integer getMaxHomeofficeDaysForUser(User user) {
        // 1. Individuelle Einstellung prüfen (höchste Priorität)
        Optional<UserHomeofficeSettings> userSettings = userHomeofficeSettingsRepository.findByUser(user);
        if (userSettings.isPresent() && userSettings.get().getMaxHomeofficeDaysPerWeek() != null) {
            return userSettings.get().getMaxHomeofficeDaysPerWeek();
        }

        // 2. Gruppen-Einstellung prüfen
        if (user.getGroup() != null) {
            return user.getGroup().getMaxHomeofficeDaysPerWeek();
        }

        // 3. Standardwert, falls weder individuelle noch Gruppen-Einstellung vorhanden
        return 0; // Oder ein anderer Standardwert
    }

    // Initialisiert die Standardrollen, falls sie noch nicht existieren
    @Transactional
    public void initializeRoles() {
        if (!roleRepository.findByName(ERole.ROLE_AZUBI).isPresent()) {
            roleRepository.save(new Role(ERole.ROLE_AZUBI));
        }
        if (!roleRepository.findByName(ERole.ROLE_AUSBILDER).isPresent()) {
            roleRepository.save(new Role(ERole.ROLE_AUSBILDER));
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}