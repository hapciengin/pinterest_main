package com.ohmyimage.controller;

import com.ohmyimage.dto.UserDto;
import com.ohmyimage.model.Role;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.RoleRepository;
import com.ohmyimage.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;

@Controller // Bu sınıf bir Controller (denetleyici) olduğunu belirtir. HTTP isteklerini yönetecek.
public class AuthController {

    @Autowired // Spring otomatik olarak bu servisi enjekte eder
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login") // "/login" URL'sine GET isteği geldiğinde çalışacak metod
    public String login() {
        return "auth/login"; // auth klasörü altındaki login.html sayfasını döndürür
    }

    @GetMapping("/register") // Kayıt formunu göstermek için kullanılan GET isteği
    public String registerForm(Model model) {
        model.addAttribute("userDto", new UserDto()); // Form verilerini tutacak boş bir DTO ekleniyor
        return "auth/register"; // auth/register.html sayfası gösterilir
    }

    @PostMapping("/register") // Formdan gelen verilerle kullanıcıyı kaydetmek için POST isteği
    public String registerUser(
            @Valid UserDto userDto, // DTO içindeki veriler validasyon kurallarına göre kontrol edilir
            BindingResult result,   // Validasyon hataları varsa bu nesne aracılığıyla alınır
            Model model) {

        if (result.hasErrors()) {
            return "auth/register"; // Formda hata varsa tekrar aynı sayfa gösterilir
        }

        // Kullanıcı adı daha önceden alınmış mı kontrolü
        if (userService.existsByUsername(userDto.getUsername())) {
            result.rejectValue("username", "error.user", "Bu kullanıcı adı zaten mevcut.");
            return "auth/register"; // Aynı kullanıcı adı varsa hata mesajı verip form geri döner
        }

        // Yeni kullanıcı oluşturuluyor
        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword())) // Şifre hashlenerek kaydedilir
                .build();

        // Varsayılan kullanıcı rolü atanıyor
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER bulunamadı")); // Rol bulunamazsa hata ver
        user.setRoles(Collections.singleton(userRole)); // Kullanıcıya tek bir rol atanır
        userService.save(user); // Kullanıcı veritabanına kaydedilir

        return "redirect:/login"; // Kayıt başarılıysa login sayfasına yönlendirilir
    }
}