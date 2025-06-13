package de.marlon.homeoffice.config; // Or a suitable config/util package

import de.marlon.homeoffice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // This makes it a Spring-managed bean
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    // Spring will automatically inject UserService here
    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        // This method will be executed once the application context is fully loaded.
        userService.initializeRoles();
        System.out.println("Application startup: Roles initialized.");
        // Add any other startup data initialization here if needed
    }
}