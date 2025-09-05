package com.parctice.spring.demo.service;

import com.parctice.spring.demo.model.User;
import com.parctice.spring.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Directory to save profile pictures
    private final String UPLOAD_DIR = "D:/uploads/"; // apne server path ke hisaab se change karo

    // ================= Email availability check =================
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // ================= Username availability check =================
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    // ================= Register user =================
    public User registerUser(User user) {
        if (!isEmailAvailable(user.getEmail())) {
            throw new RuntimeException("Email already in use!");
        }
        if (!isUsernameAvailable(user.getUsername())) {
            throw new RuntimeException("Username already taken!");
        }
        return userRepository.save(user);
    }

    // ================= Login check =================
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
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

        // Username lock
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

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + ext;

        File dest = new File(UPLOAD_DIR + fileName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

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
