package com.ohmyimage.controller;

import com.ohmyimage.model.Comment;
import com.ohmyimage.model.Pin;
import com.ohmyimage.model.User;
import com.ohmyimage.service.CommentService;
import com.ohmyimage.service.PinService;
import com.ohmyimage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/pins")
public class CommentController {

    // Servis sınıflarının enjekte edilmesi
    @Autowired private CommentService commentService;
    @Autowired private PinService pinService;
    @Autowired private UserService userService;

    // Authentication ve Principal'dan kullanıcı bilgilerini çıkartıp, User nesnesine dönüştüren yardımcı metod
    private User getUserFromAuthentication(Principal principal, Authentication authentication) {
        if (principal == null || authentication == null) return null;

        String identifier;
        User user = null;

        Object principalObj = authentication.getPrincipal();

        // OAuth2 üzerinden gelen kullanıcıyı yönetiyoruz
        if (principalObj instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principalObj;
            identifier = oauth2User.getAttribute("email");
            if (identifier != null) {
                user = userService.findByEmail(identifier); // Email ile kullanıcıyı bul
            }
        } else if (principalObj instanceof User) {
            identifier = ((User) principalObj).getUsername();
            user = userService.findByUsername(identifier); // Normal kullanıcıyı bul
        } else {
            identifier = principal.getName();
            user = userService.findByUsername(identifier); // Eğer yukarıdaki durumlar geçerli değilse, kullanıcı adı ile arama yap
            if (user == null) {
                user = userService.findByEmail(identifier); // Eğer bulamazsak, email ile de ararız
            }
        }
        return user;
    }

    // Pin'e yorum ekleme metodu (giriş yapmış her kullanıcı için)
    @PostMapping("/{pinId}/comments")
    public String addComment(@PathVariable Long pinId,
                             @RequestParam String text,
                             Principal principal,
                             Authentication authentication) {
        User user = getUserFromAuthentication(principal, authentication); // Kullanıcıyı al
        if (user == null) {
            return "redirect:/login"; // Eğer kullanıcı yoksa, login sayfasına yönlendir
        }

        Optional<Pin> pinOpt = pinService.findById(pinId); // Pin'i bul
        if (pinOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin bulunamadı"); // Eğer pin bulunamazsa, hata döndür
        }

        // Yeni yorum oluşturma
        Comment comment = Comment.builder()
                .text(text)
                .createdAt(LocalDateTime.now()) // Yorumun oluşturulma zamanı
                .user(user) // Yorum yapan kullanıcı
                .pin(pinOpt.get()) // Yorum yapılan pin
                .build();

        commentService.save(comment); // Yorum veritabanına kaydedilir
        return "redirect:/pins/" + pinId; // Pin detayına yönlendir
    }

    // Yorum silme metodu (yorum sahibi veya admin için)
    @PostMapping("/{pinId}/comments/{commentId}/delete")
    @PreAuthorize("hasRole('ADMIN') or @commentService.findById(#commentId).orElse(null)?.user?.username == #authentication.principal.name")
    public String deleteComment(@PathVariable Long pinId,
                                @PathVariable Long commentId,
                                Principal principal,
                                Authentication authentication) {
        // @PreAuthorize: Yalnızca admin veya yorum sahibinin bu işlemi yapabilmesini sağlar
        commentService.deleteById(commentId); // Yorum veritabanından silinir
        return "redirect:/pins/" + pinId; // Yorum silindikten sonra, pin detayına yönlendir
    }
}