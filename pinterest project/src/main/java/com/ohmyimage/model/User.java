package com.ohmyimage.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;
@Entity // Bu sınıfın bir veritabanı tablosu olarak temsil edileceğini belirtir.
@Getter @Setter // Lombok anotasyonları: Getter ve Setter metotlarını otomatik olarak oluşturur.
@NoArgsConstructor @AllArgsConstructor // Lombok anotasyonları: Parametresiz ve tüm parametreleri olan constructor'lar oluşturur.
@Builder // Lombok anotasyonu: Builder pattern'i kullanarak nesne oluşturmayı sağlar.
@Table(name = "app_user") // Veritabanında 'app_user' adlı tabloya karşılık gelir.
public class User implements UserDetails { // User sınıfı, Spring Security'nin UserDetails arayüzünü implement eder.

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // 'id' alanı birincil anahtar olarak kullanılır ve otomatik artan bir değere sahiptir.
    private Long id;

    @Column(nullable = false, unique = true) // 'username' alanı veritabanında null olamaz ve benzersiz olmalıdır.
    private String username;

    @Column(nullable = false) // 'password' alanı null olamaz.
    private String password;

    @Column(nullable = false) // 'email' alanı null olamaz.
    private String email;

    @ManyToMany(fetch = FetchType.EAGER) // Kullanıcı ve roller arasında çoktan çoğa ilişki olduğunu belirtir.
    @JoinTable(
            name = "user_roles", // Bağlantı tablosunun adı
            joinColumns = @JoinColumn(name = "user_id"), // Kullanıcıyı temsil eden sütun
            inverseJoinColumns = @JoinColumn(name = "role_id") // Roller tablosunu temsil eden sütun
    )
    @Builder.Default // Eğer kullanıcı rolü eklenmezse boş bir Set döndürülmesini sağlar.
    private Set<Role> roles = new HashSet<>(); // Kullanıcının rollerini saklar.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Kullanıcının yetkilerini almak için rolleri 'GrantedAuthority' objelerine dönüştürür.
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.getName())) // Her rolü basit bir yetki olarak dönüştürür.
                .collect(Collectors.toList()); // Yetkileri bir liste olarak döner.
    }

    public String getName() {
        return this.username; // Kullanıcı adını döner.
    }

    // UserDetails arayüzünün gerekli metotları (true döndürerek bu kullanıcıyı her zaman aktif ve kilitlenmemiş kabul ederiz):
    @Override public boolean isAccountNonExpired()   { return true; } // Hesap süresi dolmamış.
    @Override public boolean isAccountNonLocked()    { return true; } // Hesap kilitlenmemiş.
    @Override public boolean isCredentialsNonExpired(){ return true; } // Kimlik bilgileri süresi dolmamış.
    @Override public boolean isEnabled()             { return true; } // Hesap etkin.
}