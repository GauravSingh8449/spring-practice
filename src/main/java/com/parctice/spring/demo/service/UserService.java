package com.parctice.spring.demo.service;

import com.parctice.spring.demo.model.User;
import com.parctice.spring.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final String UPLOAD_DIR = "D:/uploads/"; // change according to your path

    // ================= Email availability check =================
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ================= Username availability check =================
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // ================= Register user =================
    public User registerUser(User user) {
        if (existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }
        if (existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already taken!");
        }

        user.setEnabled(true);
        user.setCurrentLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ================= Login =================
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            user.setLastLogin(user.getCurrentLogin());    // previous login
            user.setCurrentLogin(LocalDateTime.now());    // current login
            userRepository.save(user);
            return user;
        }
        return null;
    }

    // ================= Save or update user =================
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // ================= Find user by ID =================
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // ================= Update user =================
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        existingUser.setEmail(user.getEmail());

        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            existingUser.setPassword(user.getPassword());
        }

        if (user.getProfilePicPath() != null && !user.getProfilePicPath().isEmpty()) {
            existingUser.setProfilePicPath(user.getProfilePicPath());
        }

        return userRepository.save(existingUser);
    }

    // ================= Save Profile Picture =================
    public String saveProfilePicture(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + ext;

        File dest = new File(UPLOAD_DIR + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();

        file.transferTo(dest);
        return fileName;
    }

    // ================= Delete Profile Picture =================
    public void deleteProfilePicture(User user) {
        if (user.getProfilePicPath() != null && !user.getProfilePicPath().isEmpty()) {
            File file = new File(UPLOAD_DIR + user.getProfilePicPath());
            if (file.exists()) file.delete();
            user.setProfilePicPath(null);
            userRepository.save(user);
        }
    }
}
