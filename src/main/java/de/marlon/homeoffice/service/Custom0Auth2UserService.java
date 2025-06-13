package de.marlon.homeoffice.service; // Your package

import de.marlon.homeoffice.model.ERole;
import de.marlon.homeoffice.model.Role;
import de.marlon.homeoffice.model.User;
import de.marlon.homeoffice.repository.RoleRepository;
import de.marlon.homeoffice.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

@Service
public class Custom0Auth2UserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(Custom0Auth2UserService.class); // Add Logger

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Custom0Auth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional // Ensure transactional operations
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.debug("Attempting to load user from OIDC provider...");
        OidcUser oidcUser = super.loadUser(userRequest); // Delegate to default OidcUserService

        logger.debug("Raw OIDC User attributes: {}", oidcUser.getAttributes());

        String username = oidcUser.getPreferredUsername();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        logger.debug("Extracted from OIDC: Username={}, Email={}, FullName={}", username, email, name);

        if (username == null || email == null) {
            logger.error("Missing required user claims: preferred_username or email.");
            throw new OAuth2AuthenticationException("Missing required user claims: preferred_username or email.");
        }

        Optional<User> existingUser = userRepository.findByUsername(username);
        User user;
        Set<Role> userRoles = new HashSet<>();

        if (existingUser.isPresent()) {
            user = existingUser.get();
            logger.debug("Existing user found: {}", user.getUsername());
            user.setEmail(email); // Update email if it changed
            // You can update other attributes here as needed
            userRoles.addAll(user.getRoles()); // Keep existing roles
        } else {
            logger.debug("New user detected: {}. Creating in local database.", username);
            user = new User();
            user.setUsername(username);
            user.setEmail(email);
            // Dummy password needed by JPA, not for actual authentication
            user.setPassword("{noop}DUMMY_PASSWORD_AUTHENTIK"); // No need to encode, but {noop} prefix is critical
            user.setActive(true);
            // Assign default role (AZUBI) for new users
            Role azubiRole = roleRepository.findByName(ERole.ROLE_AZUBI)
                    .orElseThrow(() -> {
                        logger.error("Role AZUBI not found in database! Please ensure roles are initialized.");
                        return new RuntimeException("Error: Role AZUBI is not found.");
                    });
            userRoles.add(azubiRole);
            user.setRoles(userRoles); // Set initial roles
        }

        // --- Role Mapping from Authentik Claims (Optional but highly recommended) ---
        // This is where you'd map Authentik groups/roles to your internal ERole system.
        // Example: If Authentik sends a "groups" claim and "Ausbilder" is one of them.
        List<String> authentikGroups = oidcUser.getClaimAsStringList("groups"); // Adjust "groups" to your Authentik claim name
        if (authentikGroups != null) {
            logger.debug("Authentik groups claim: {}", authentikGroups);
            if (authentikGroups.contains("Ausbilder")) { // Make sure "Ausbilder" is the exact group name in Authentik
                Role ausbilderRole = roleRepository.findByName(ERole.ROLE_AUSBILDER)
                        .orElseThrow(() -> {
                            logger.error("Role AUSBILDER not found in database! Please ensure roles are initialized.");
                            return new RuntimeException("Error: Role AUSBILDER is not found.");
                        });
                userRoles.add(ausbilderRole);
                logger.debug("Assigned ROLE_AUSBILDER to user {}", username);
            }
            // You might remove default AZUBI if specific groups are found, depending on your logic
            // E.g., if authentikGroups indicates only "Ausbilder", perhaps they shouldn't be "Azubi" too.
            // For now, we just add if present.
        } else {
            logger.debug("No 'groups' claim found in OIDC user info. Defaulting to roles from local DB or initial AZUBI.");
        }

        // Ensure user has at least one role
        if (userRoles.isEmpty()) {
            Role azubiRole = roleRepository.findByName(ERole.ROLE_AZUBI)
                    .orElseThrow(() -> {
                        logger.error("Role AZUBI not found during fallback!");
                        return new RuntimeException("Error: Role AZUBI is not found during fallback.");
                    });
            userRoles.add(azubiRole);
            logger.warn("User {} had no roles after processing, assigned default ROLE_AZUBI.", username);
        }
        user.setRoles(userRoles); // Update user's roles
        userRepository.save(user); // Save/update user in DB after role assignments

        logger.debug("User {} saved/updated in DB with roles: {}", user.getUsername(), user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.joining(", ")));

        // Create Spring Security authorities from the roles assigned in your local DB
        Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        logger.debug("Spring Security authorities generated for {}: {}", username, authorities);

        // Return a DefaultOidcUser with the combined authorities from your DB
        return new DefaultOidcUser(authorities, userRequest.getIdToken(), oidcUser.getUserInfo());
    }


}