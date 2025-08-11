package com.ohmyimage.controller;

import com.ohmyimage.model.Pin;
import com.ohmyimage.model.User;
import com.ohmyimage.service.PinService;
import com.ohmyimage.service.UserService;
import com.ohmyimage.util.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MainController {
    // PinService ve UserService sınıflarını enjekte ediyoruz
    @Autowired
    private PinService pinService;

    @Autowired
    private UserService userService;

    // Authentication ve Principal'dan kullanıcı bilgilerini çıkaran yardımcı metod
    private User getUserFromAuthentication(Principal principal, Authentication authentication) {
        if (principal == null || authentication == null) return null; // principal veya authentication null ise, kullanıcı yok demektir
        Object principalObj = authentication.getPrincipal(); // Kullanıcı bilgilerini alıyoruz
        String identifier = null;
        User user = null;

        // Eğer OAuth2 üzerinden giriş yaptıysa
        if (principalObj instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principalObj;
            identifier = oauth2User.getAttribute("email"); // OAuth2 ile gelen email'i alıyoruz
            if (identifier != null) user = userService.findByEmail(identifier); // Email ile kullanıcıyı bul
        } else if (principalObj instanceof User) {
            identifier = ((User) principalObj).getUsername(); // Eğer User objesi ise, kullanıcı adını alıyoruz
            user = userService.findByUsername(identifier); // Kullanıcı adını kullanarak kullanıcıyı buluyoruz
        } else {
            identifier = principal.getName(); // Eğer yukarıdaki durumlar geçerli değilse, principal'dan ismi alıyoruz
            user = userService.findByUsername(identifier); // Kullanıcı adı ile arama yapıyoruz
            if (user == null) user = userService.findByEmail(identifier); // Eğer kullanıcı bulamazsak, email ile de arıyoruz
        }
        return user; // Bulunan kullanıcıyı geri döndürüyoruz
    }

    // Ana sayfa için HTTP GET isteği ile gelen requesti işle
    @GetMapping("/")
    public String index(Model model, Principal principal, Authentication authentication) {
        // Tüm pinleri alıyoruz
        List<Pin> pins = pinService.findAll();

        // Mevcut kullanıcıyı al ve admin olup olmadığını kontrol et
        User currentUser = getUserFromAuthentication(principal, authentication);
        boolean isAdmin = false;

        // Eğer kullanıcı mevcutsa ve authentication nesnesi var ise admin olup olmadığını kontrol et
        if (currentUser != null && authentication != null) {
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); // Kullanıcının admin rolüne sahip olup olmadığını kontrol et
        }

        // Pinleri Base64 formatına dönüştürerek modele ekliyoruz
        List<PinWithBase64> pinsWithBase64 = pins.stream()
                .map(pin -> new PinWithBase64(pin, Base64Util.encodeBase64(pin.getImageData()))) // Pinlerin görsellerini Base64'e dönüştür
                .collect(Collectors.toList()); // Listeyi topluyoruz

        // Model'e pinleri, mevcut kullanıcıyı ve admin olup olmadığını ekliyoruz
        model.addAttribute("pins", pinsWithBase64);
        model.addAttribute("currentUser", currentUser); // Kullanıcı ID'si kontrolü için HTML'ye aktarılıyor
        model.addAttribute("isAdmin", isAdmin); // Admin yetkisi kontrolü için HTML'ye aktarılıyor

        // Ana sayfayı döndürüyoruz (HTML sayfası)
        return "index";
    }

    // Pin ve Base64 görselini birlikte tutan sınıf
    public static class PinWithBase64 {
        public final Pin pin; // Pin nesnesi
        public final String imgBase64; // Base64 formatında görsel

        public PinWithBase64(Pin pin, String imgBase64) {
            this.pin = pin;
            this.imgBase64 = imgBase64; // Constructor ile pin ve görseli alıyoruz
        }

        // Getter metodları ile pin ve Base64 görseli alabiliyoruz
        public Pin getPin() {
            return pin;
        }

        public String getImgBase64() {
            return imgBase64;
        }
    }
}