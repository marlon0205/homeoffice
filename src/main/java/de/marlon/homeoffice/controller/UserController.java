package de.marlon.homeoffice.controller;

import de.marlon.homeoffice.model.User;
import de.marlon.homeoffice.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OidcUser principal) {
        Map<String, Object> userInfo = new HashMap<>();
        
        if (principal != null) {
            userInfo.put("username", principal.getPreferredUsername());
            userInfo.put("name", principal.getFullName());
            userInfo.put("email", principal.getEmail());
            userInfo.put("roles", principal.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .toList());
        }
        
        return userInfo;
    }
}
