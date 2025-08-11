package com.ohmyimage.config;

// Gerekli model, servis ve repository sınıflarının importları
import com.ohmyimage.model.Board;
import com.ohmyimage.model.Role;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.RoleRepository;
import com.ohmyimage.service.BoardService;
import com.ohmyimage.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

@Configuration // Bu sınıfın bir konfigurasyon sınıfı olduğunu belirtir
@EnableMethodSecurity // Metot bazlı güvenlik (örneğin @PreAuthorize) aktif edilir
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class); // Loglama için

    @Autowired
    private UserService userService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private RoleRepository roleRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        // Kullanıcıyı username'e göre bulur. Spring Security'nin ihtiyaç duyduğu servis.
        return username -> {
            logger.debug("UserDetailsService called for username: {}", username);
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warn("UserDetailsService could not find user by username: {}", username);
            }
            return user;
        };
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // Parola güvenliğini sağlamak için BCrypt kullanılır.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Form tabanlı login için Authentication Provider ayarlanıyor.
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService());
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        // OAuth2 ile giriş yapan kullanıcıyı sisteme tanıtır, gerekirse kaydeder.
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            logger.debug("OAuth2UserService called for provider: {}", request.getClientRegistration().getRegistrationId());
            OAuth2User oauth2User = delegate.loadUser(request);

            Map<String, Object> attributes = oauth2User.getAttributes(); // Kullanıcı bilgileri
            logger.debug("OAuth2 Attributes received: {}", attributes);

            String email = (String) attributes.get("email"); // E-posta zorunlu
            if (email == null) {
                logger.error("Email not found in OAuth2 attributes");
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
            }

            logger.debug("Email found in OAuth2 attributes: {}", email);
            User user = userService.findByEmail(email); // Veritabanında kullanıcı aranıyor
            boolean isNewUser = false;
            if (user == null) {
                // Yeni kullanıcı kaydı yapılacaksa
                isNewUser = true;
                logger.info("Creating new user for email: {}", email);
                user = new User();

                // Kullanıcı adı belirleme
                String username = email;
                if(userService.existsByUsername(username)) {
                    // Aynı kullanıcı adı varsa sonuna rastgele bir şey ekleniyor
                    username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
                }
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(passwordEncoder().encode(UUID.randomUUID().toString())); // Rastgele şifre atanıyor

                // Varsayılan rol atanıyor
                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> {
                            logger.error("Default role ROLE_USER not found in database");
                            return new RuntimeException("Default role not found");
                        });
                user.setRoles(Collections.singleton(userRole));

                user = userService.save(user); // Veritabanına kaydet

                // Gmail'den gelen kullanıcıya otomatik board oluştur
                Board defaultBoard = Board.builder()
                        .name("gmail")
                        .owner(user) // Sahibi yeni kullanıcı
                        .build();

                boardService.save(defaultBoard);
                logger.info("New user and gmail board created for: {}", user.getUsername());
            } else {
                logger.info("Existing user found for email: {}", email);
            }

            Set<Role> userRoles = user.getRoles();
            if (userRoles == null || userRoles.isEmpty()) {
                // Eğer kullanıcının rolü yoksa varsayılan rol atanır
                Role defaultRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));
                user.setRoles(Collections.singleton(defaultRole));
                if (!isNewUser) {
                    userService.save(user); // Sadece eski kullanıcıysa tekrar kaydedilir
                }
                userRoles = user.getRoles();
            }

            // Rol bilgileri yetki listesine dönüştürülüyor
            List<SimpleGrantedAuthority> authorities = userRoles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());
            logger.debug("Authorities for user {}: {}", email, authorities);

            // OAuth2 girişinde sistemin tanıyacağı kullanıcı nesnesi döndürülüyor
            return new DefaultOAuth2User(
                    authorities,
                    attributes,
                    "email" // Ana kimlik bilgisi olarak email kullanılacak
            );
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Spring Security'nin asıl yapılandırması burada yapılır
        http
                .authenticationProvider(authenticationProvider()) // Form girişleri için
                .csrf(csrf -> csrf.disable()) // CSRF kapatılıyor (güvenlik durumuna göre açılabilir)
                .authorizeHttpRequests(auth -> auth
                        // Herkesin erişebileceği sayfalar
                        .requestMatchers(
                                "/", "/pins/search", "/pins/{id:[0-9]+}",
                                "/css/**", "/js/**", "/images/**",
                                "/register", "/login",
                                "/forgot-password", "/reset-password**"
                        ).permitAll()
                        // Arama sayfaları herkes için açık
                        .requestMatchers("/search", "/search/**").permitAll()
                        // Sadece admin erişebilir
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Giriş yapmış kullanıcılar erişebilir
                        .requestMatchers("/pins/create", "/pins/*/edit", "/pins/*/delete", "/pins/*/comments").authenticated()
                        .requestMatchers("/boards", "/boards/create", "/boards/*/**").authenticated()
                        .requestMatchers("/profile", "/user/boards").authenticated()
                        // Diğer tüm istekler de giriş gerektirir
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll() // Login sayfası herkes için açık
                        .defaultSuccessUrl("/", true) // Başarılı giriş sonrası ana sayfa
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Çıkış sonrası yönlendirme
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserService()) // OAuth2 giriş sonrası kullanıcıyı işleme alma
                        )
                        .defaultSuccessUrl("/", true)
                );

        return http.build(); // Konfigürasyon tamamlandı
    }
}