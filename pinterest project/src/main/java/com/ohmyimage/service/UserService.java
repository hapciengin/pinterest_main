package com.ohmyimage.service;

import com.ohmyimage.model.Board;
import com.ohmyimage.model.Comment;
import com.ohmyimage.model.Pin;
import com.ohmyimage.model.Role;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.BoardRepository;
import com.ohmyimage.repository.CommentRepository;
import com.ohmyimage.repository.PasswordResetTokenRepository;
import com.ohmyimage.repository.PinRepository;
import com.ohmyimage.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PinRepository pinRepository;
    @Autowired private BoardRepository boardRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    // universal search için eklendi
    public List<User> searchByUsername(String usernamePart) {
        return userRepository.findByUsernameContainingIgnoreCase(usernamePart);
    }

    public void updateRoles(Long userId, Set<Role> roles) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setRoles(roles);
            userRepository.save(u);
        });
    }

    public void updatePassword(User user, String newHashedPassword) {
        user.setPassword(newHashedPassword);
        userRepository.save(user);
        logger.info("Password updated for user: {}", user.getUsername());
        // opsiyonel: eski tokenları sil
        tokenRepository.findByUser(user).forEach(tokenRepository::delete);
    }

    @Transactional
    public void deleteById(Long userId) {
        logger.info("Deleting user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found ID: " + userId));

        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("Ana admin hesabı silinemez!");
        }

        // 1) tokenları sil
        tokenRepository.findByUser(user).forEach(tokenRepository::delete);

        // 2) yorum sahipliğini temizle
        List<Comment> comments = commentRepository.findByUser(user);
        comments.forEach(c -> { c.setUser(null); commentRepository.save(c); });

        // 3) pin sahipliğini temizle
        List<Pin> pins = pinRepository.findByOwner(user);
        pins.forEach(p -> { p.setOwner(null); pinRepository.save(p); });

        // 4) board sahipliğini temizle
        List<Board> boards = boardRepository.findByOwner(user);
        boards.forEach(b -> { b.setOwner(null); boardRepository.save(b); });

        // 5) kullanıcıyı sil
        userRepository.delete(user);
        logger.info("User deleted: {}", userId);
    }
}