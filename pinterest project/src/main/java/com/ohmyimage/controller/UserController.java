package com.ohmyimage.controller;

import com.ohmyimage.model.Board;
import com.ohmyimage.model.User;
import com.ohmyimage.service.BoardService;
import com.ohmyimage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
@Controller
public class UserController {

    @Autowired private UserService userService;
    @Autowired private BoardService boardService;

    // Giriş yapan kullanıcıyı alır (form login veya OAuth2)
    private User getUser(Principal principal, Authentication auth) {
        if (principal == null || auth == null) return null;
        Object p = auth.getPrincipal();
        String id = null;
        User u = null;
        if (p instanceof OAuth2User) {
            // OAuth2 üzerinden giriş yapan kullanıcıyı alıyoruz
            id = ((OAuth2User) p).getAttribute("email");
            u = userService.findByEmail(id);
        } else if (p instanceof User) {
            // Standart kullanıcı için username üzerinden alıyoruz
            id = ((User) p).getUsername();
            u = userService.findByUsername(id);
        } else {
            // Principal üzerinden de kullanıcıyı bulabiliyoruz
            id = principal.getName();
            u = userService.findByUsername(id);
            if (u == null) u = userService.findByEmail(id);
        }
        return u;
    }

    // Kullanıcının profil sayfası
    @GetMapping("/profile")
    public String profile(Principal principal, Authentication auth, Model model) {
        User user = getUser(principal, auth);
        if (user == null) return "redirect:/login"; // Giriş yapılmamışsa login sayfasına yönlendir
        model.addAttribute("user", user);
        model.addAttribute("boards", boardService.findByOwner(user)); // Kullanıcının sahip olduğu board'ları getir
        return "user/profile";
    }

    // Kendi hesabını silme onay sayfası
    @GetMapping("/profile/delete")
    @PreAuthorize("isAuthenticated()") // Sadece giriş yapmış kullanıcılar erişebilir
    public String confirmDelete() {
        return "user/confirm-delete";
    }

    // Kendi hesabını silme işlemi
    @PostMapping("/profile/delete")
    @PreAuthorize("isAuthenticated()") // Sadece giriş yapmış kullanıcılar işlemi gerçekleştirebilir
    public String deleteOwn(Principal principal, Authentication auth, RedirectAttributes attrs) {
        User user = getUser(principal, auth);
        if (user == null) { // Kullanıcı bulunamazsa hata mesajı göster
            attrs.addFlashAttribute("error", "Hesabınız bulunamadı.");
            return "redirect:/profile";
        }

        // Admin kullanıcının kendi hesabını silmesini engelle
        if (user.getUsername().equals("admin")) {
            attrs.addFlashAttribute("error", "Ana admin hesabı bu sayfadan silinemez. Yönetim panelini kullanın.");
            return "redirect:/profile";
        }

        try {
            userService.deleteById(user.getId()); // Kullanıcıyı sil
            SecurityContextHolder.clearContext(); // Güvenlik bağlamını temizle (kullanıcı çıkış yapacak)
            attrs.addFlashAttribute("message", "Hesabınız başarıyla silindi.");
            return "redirect:/login?accountDeleted"; // Hesap silindikten sonra login sayfasına yönlendir
        } catch (Exception e) {
            attrs.addFlashAttribute("error", "Hesabınızı silerken bir hata oluştu: " + e.getMessage());
            return "redirect:/profile"; // Hata oluşursa profile sayfasına geri dön
        }
    }

    // Admin – kullanıcı listesi
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll(); // Tüm kullanıcıları al
        model.addAttribute("users", users);
        return "admin/users";
    }

    // Admin – kullanıcı detayı görüntüleme
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id).orElse(null); // Kullanıcıyı id ile bul
        if (user == null) {
            return "redirect:/admin/users"; // Kullanıcı bulunamazsa listeye geri dön
        }
        model.addAttribute("user", user);
        List<Board> boards = boardService.findByOwner(user); // Kullanıcının board'larını getir
        model.addAttribute("boards", boards);
        return "admin/user-detail";
    }

    // Admin – kullanıcı silme işlemi
    @PostMapping("/admin/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDeleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long targetUserId = id;
        User adminUser = userService.findByUsername("admin"); // Admin kullanıcıyı bul

        if (adminUser != null && targetUserId.equals(adminUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Ana admin hesabı silinemez."); // Admin hesabı silinemez
        } else {
            try {
                userService.deleteById(targetUserId); // Kullanıcıyı sil
                redirectAttributes.addFlashAttribute("message", "Kullanıcı başarıyla silindi.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Kullanıcı silme işlemi sırasında hata: " + e.getMessage());
            }
        }
        return "redirect:/admin/users"; // Listeye geri dön
    }
}