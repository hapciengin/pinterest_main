package com.ohmyimage.repository;

import com.ohmyimage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);

    // universal search için
    List<User> findByUsernameContainingIgnoreCase(String usernamePart);
}