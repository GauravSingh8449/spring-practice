package com.parctice.spring.demo.controller;

import com.parctice.spring.demo.model.User;
import com.parctice.spring.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // ================= Register =================
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username,
                                           @RequestParam String email,
                                           @RequestParam String password,
                                           HttpServletRequest request) {

        // email format check
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }

        // email already exist check
        if (!userService.isEmailAvailable(email)) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        // username already exist check
        if (!userService.isUsernameAvailable(username)) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        // new user create
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // password hashing optional

        userService.registerUser(user);

        return ResponseEntity.ok("Registration successful! You can now login.");
    }

    // ================= Login =================
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username,
                                        @RequestParam String password) {

        User validUser = userService.login(username, password);
        if (validUser != null) {
            return ResponseEntity.ok("Welcome " + validUser.getUsername() + "!");
        } else {
            return ResponseEntity.badRequest().body("Invalid Username or Password!");
        }
    }
}
