package com.ohmyimage;

import com.ohmyimage.model.Role;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.RoleRepository;
import com.ohmyimage.repository.UserRepository;
import com.ohmyimage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class OhMyImageApplication {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(OhMyImageApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Eğer veritabanı boşsa, init yap
            if (userRepository.count() == 0 && roleRepository.count() == 0) {
                // Eğer roller henüz eklenmemişse, ekle
                roleRepository.save(Role.builder().name("ROLE_USER").build());
                roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
                System.out.println("Roller başarıyla eklendi.");


                // Örnek admin kullanıcı oluştur (eğer yoksa)
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@ohmyimage.com")
                        .build();

                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(roleRepository.findByName("ROLE_ADMIN").orElse(null));
                adminRoles.add(roleRepository.findByName("ROLE_USER").orElse(null));
                admin.setRoles(adminRoles);

                userService.save(admin);
                System.out.println("Admin kullanıcı başarıyla oluşturuldu.");
            } else {
                System.out.println("Veritabanı zaten mevcut. Başlangıç verileri eklenmiyor.");
            }
        };
    }
}