package com.ohmyimage.controller;

import com.ohmyimage.model.User;
import com.ohmyimage.service.PasswordResetService;
import com.ohmyimage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class PasswordResetController {

    // Şifre sıfırlama işlemleri için gerekli servisleri enjekte ediyoruz
    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Şifre sıfırlama talep formunu döndürüyoruz
    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password"; // Kullanıcıya şifre sıfırlama talep formunu sunuyoruz
    }

    // Şifre sıfırlama talebini işliyoruz
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        // E-posta adresine göre sıfırlama token'ı oluşturuluyor
        passwordResetService.createPasswordResetToken(email);

        // Kullanıcıya başarılı mesajını iletiyoruz
        model.addAttribute("message",
                "Eğer e-posta kayıtlıysa, şifre sıfırlama maili gönderildi.");
        return "auth/forgot-password"; // Şifre sıfırlama formunu tekrar döndürüyoruz
    }

    // Şifre sıfırlama formunu kullanıcıya sunuyoruz
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        // Token'ı doğruluyoruz ve geçerli bir token var mı kontrol ediyoruz
        Optional<User> userOpt = passwordResetService.validatePasswordResetToken(token);

        if (userOpt.isEmpty()) {
            // Token geçersiz veya süresi dolmuşsa hata mesajı göster
            model.addAttribute("error", "Token geçersiz veya süresi dolmuş.");
        } else {
            // Geçerli token varsa token'ı modelde gönderiyoruz
            model.addAttribute("token", token);
        }
        return "auth/reset-password"; // Şifre sıfırlama formunu kullanıcıya döndürüyoruz
    }

    // Şifre sıfırlama işlemi
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       Model model) {
        // Kullanıcı tarafından girilen şifre ve onay şifresi uyuşmuyorsa hata mesajı
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Şifreler uyuşmuyor.");
            model.addAttribute("token", token); // Token'ı formda tutuyoruz
            return "auth/reset-password"; // Hata ile formu tekrar gösteriyoruz
        }

        // Token'ı doğruluyoruz ve geçerli bir kullanıcı var mı kontrol ediyoruz
        Optional<User> userOpt = passwordResetService.validatePasswordResetToken(token);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Token geçersiz veya süresi dolmuş.");
            return "auth/reset-password"; // Geçersiz token ile formu tekrar gösteriyoruz
        }

        // Geçerli token ve kullanıcı ile şifreyi güncelliyoruz
        User user = userOpt.get();
        userService.updatePassword(user, passwordEncoder.encode(password)); // Şifreyi encode ederek kaydediyoruz

        // Şifre değişikliğinden sonra token'i sil
        passwordResetService.deleteToken(token);

        // Şifre güncelleme başarılı mesajını kullanıcıya iletiyoruz
        model.addAttribute("message", "Şifreniz başarıyla güncellendi. Giriş yapabilirsiniz.");
        return "auth/reset-password"; // Şifre güncelleme işlemini tamamladık ve formu gösteriyoruz
    }
}