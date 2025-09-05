package com.parctice.spring.demo.controller;

import com.parctice.spring.demo.model.User;
import com.parctice.spring.demo.service.UserService;
import com.parctice.spring.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // ================= Show Registration Page =================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // ================= Handle Registration =================
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        user.setEnabled(true);
        userService.saveUser(user);

        // Send welcome email
        String subject = "Welcome to Our Application!";
        String body = "Hi " + user.getUsername() + ",\n\n" +
                "Thank you for registering. You can now login using your credentials.\n\n" +
                "Best Regards,\nSupport Team";
        emailService.sendEmail(user.getEmail(), subject, body);

        model.addAttribute("message", "Registration successful! You can now login.");
        return "login";
    }

    // ================= Show Login Page =================
    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("user", new User());
        if (error != null) model.addAttribute("error", "Invalid username or password!");
        return "login";
    }

    // ================= Handle Login =================
    @PostMapping("/login")
    public String loginUser(@ModelAttribute User user, HttpSession session) {
        User validUser = userService.login(user.getUsername(), user.getPassword());
        if (validUser != null) {
            User freshUser = userService.findById(validUser.getId());
            session.setAttribute("loggedInUser", freshUser);
            return "redirect:/home";
        } else {
            return "redirect:/login?error=true";
        }
    }

    // ================= Show Home Page =================
    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("message", "Welcome " + user.getUsername() + "!");
        return "home";
    }

    // ================= Show Profile Page =================
    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        User freshUser = userService.findById(user.getId());
        session.setAttribute("loggedInUser", freshUser);

        model.addAttribute("user", freshUser);
        model.addAttribute("cacheBuster", System.currentTimeMillis());
        return "profile";
    }

    // ================= Handle Profile Update (Password) =================
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User user, HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null) return "redirect:/login";

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            model.addAttribute("user", loggedUser);
            model.addAttribute("errorMessage", "Password cannot be empty!");
            model.addAttribute("cacheBuster", System.currentTimeMillis());
            return "profile";
        }

        loggedUser.setPassword(user.getPassword());
        userService.updateUser(loggedUser);

        // Send email after profile update
        String subject = "Your password has been updated";
        String body = "Hi " + loggedUser.getUsername() + ",\n\n" +
                "Your account password has been successfully updated.\n\n" +
                "If you did not perform this change, please contact support immediately.\n\n" +
                "Best Regards,\nSupport Team";
        emailService.sendEmail(loggedUser.getEmail(), subject, body);

        User freshUser = userService.findById(loggedUser.getId());
        session.setAttribute("loggedInUser", freshUser);

        model.addAttribute("user", freshUser);
        model.addAttribute("successMessage", "Profile updated successfully!");
        model.addAttribute("cacheBuster", System.currentTimeMillis());
        return "profile";
    }

    // ================= Show Settings Page =================
    @GetMapping("/settings")
    public String showSettingsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        User freshUser = userService.findById(user.getId());
        model.addAttribute("user", freshUser);
        return "settings"; // settings.html
    }

    // ================= Handle Settings Update (Old Password Verification + Email) =================
    @PostMapping("/settings/update")
    public String updateSettings(@ModelAttribute User user, HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null) return "redirect:/login";

        // Check old password
        if (user.getOldPassword() == null || !loggedUser.getPassword().equals(user.getOldPassword())) {
            model.addAttribute("errorMessage", "Old password is incorrect!");
            model.addAttribute("user", loggedUser);
            return "settings";
        }

        // Update password and email
        loggedUser.setPassword(user.getPassword());
        loggedUser.setEmail(user.getEmail());
        userService.updateUser(loggedUser);

        // ✅ Send email about settings update
        String subject = "Your account settings have been updated";
        String body = "Hi " + loggedUser.getUsername() + ",\n\n" +
                "Your account settings (email/password) have been successfully updated.\n\n" +
                "If you did not perform this change, please contact support immediately.\n\n" +
                "Best Regards,\nSupport Team";
        emailService.sendEmail(loggedUser.getEmail(), subject, body);

        User freshUser = userService.findById(loggedUser.getId());
        session.setAttribute("loggedInUser", freshUser);

        model.addAttribute("successMessage", "Settings updated successfully!");
        model.addAttribute("user", freshUser);
        return "settings";
    }

    // ================= Handle Profile Picture Upload (AJAX + Crop) =================
    @PostMapping("/profile/upload")
    @ResponseBody
    public Map<String, Object> uploadProfilePic(@RequestParam("file") MultipartFile file,
                                                HttpSession session) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            resp.put("status", "error");
            resp.put("message", "User not logged in");
            return resp;
        }

        if (file == null || file.isEmpty()) {
            resp.put("status", "error");
            resp.put("message", "No file selected");
            return resp;
        }

        String filename = userService.saveProfilePicture(file);
        user.setProfilePicPath(filename);
        userService.updateUser(user);
        session.setAttribute("loggedInUser", user);

        resp.put("status", "success");
        resp.put("path", filename);
        return resp;
    }

    // ================= Logout =================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
