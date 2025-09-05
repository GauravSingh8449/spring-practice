package com.parctice.spring.demo.repository;

import com.parctice.spring.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Login ke liye
    User findByUsername(String username);

    // Email check ke liye
    User findByEmail(String email);

    // Optional: Username aur Email dono check karne ke liye
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

