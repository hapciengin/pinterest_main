package com.ohmyimage.service;

import com.ohmyimage.model.PasswordResetToken;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender mailSender;

    @Transactional
    public void createPasswordResetToken(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return;
        }
        // Eski tokenları sil
        List<PasswordResetToken> old = tokenRepository.findByUser(user);
        tokenRepository.deleteAll(old);

        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        tokenRepository.save(prt);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setSubject("OhMyImage Şifre Sıfırlama");
        String link = "http://localhost:8080/reset-password?token=" + token;
        msg.setText("Şifrenizi sıfırlamak için tıklayın:\n" + link);
        mailSender.send(msg);
    }

    public Optional<User> validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> prtOpt = tokenRepository.findByToken(token);
        if (prtOpt.isEmpty()) {
            return Optional.empty();
        }
        PasswordResetToken prt = prtOpt.get();
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(prt);
            return Optional.empty();
        }
        return Optional.of(prt.getUser());
    }

    @Transactional
    public void deleteToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenRepository::delete);
    }

    @Transactional
    public void deleteAllUserTokens(User user) {
        List<PasswordResetToken> tokens = tokenRepository.findByUser(user);
        tokenRepository.deleteAll(tokens);
    }
}