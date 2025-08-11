package com.ohmyimage.controller;

import com.ohmyimage.dto.PinDto;
import com.ohmyimage.model.Board;
import com.ohmyimage.model.Pin;
import com.ohmyimage.model.User;
import com.ohmyimage.service.BoardService;
import com.ohmyimage.service.PinService;
import com.ohmyimage.service.UserService;
import com.ohmyimage.util.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pins")
public class PinController {

    @Autowired private PinService pinService;
    @Autowired private BoardService boardService;
    @Autowired private UserService userService;

    private User getUserFromAuthentication(Principal principal, Authentication authentication) {
        if (principal == null || authentication == null) return null;
        Object principalObj = authentication.getPrincipal();
        String identifier = null;
        User user = null;
        if (principalObj instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principalObj;
            identifier = oauth2User.getAttribute("email");
            if (identifier != null) user = userService.findByEmail(identifier);
        } else if (principalObj instanceof User) {
            identifier = ((User) principalObj).getUsername();
            user = userService.findByUsername(identifier);
        } else {
            identifier = principal.getName();
            user = userService.findByUsername(identifier);
            if (user == null) user = userService.findByEmail(identifier);
        }
        return user;
    }

    @GetMapping("/create")
    public String createPinForm(Model model, Principal principal, Authentication authentication) {
        User user = getUserFromAuthentication(principal, authentication);
        if (user == null) return "redirect:/login";
        List<Board> boards = boardService.findByOwner(user);
        model.addAttribute("boards", boards);
        model.addAttribute("pinDto", new PinDto());
        return "pins/create";
    }

    @PostMapping("/create")
    public String createPin(@ModelAttribute PinDto pinDto,
                            Principal principal,
                            Authentication authentication,
                            Model model) {
        User user = getUserFromAuthentication(principal, authentication);
        if (user == null) return "redirect:/login";
        try {
            MultipartFile file = pinDto.getImage();
            if (file == null || file.isEmpty()) {
                model.addAttribute("error", "Lütfen bir görsel yükleyiniz.");
                model.addAttribute("boards", boardService.findByOwner(user));
                model.addAttribute("pinDto", pinDto);
                return "pins/create";
            }
            Board board = boardService.findById(pinDto.getBoardId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seçilen pano bulunamadı"));
            // Panoya pin ekleme yetkisi kontrolü (pano sahibi veya admin)
            if (!board.getOwner().getId().equals(user.getId()) &&
                !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu panoya pin ekleme yetkiniz yok.");
            }
            Pin pin = Pin.builder()
                    .title(pinDto.getTitle())
                    .description(pinDto.getDescription())
                    .imageData(file.getBytes())
                    .createdAt(LocalDateTime.now())
                    .owner(user)
                    .board(board)
                    .build();
            pinService.save(pin);
            return "redirect:/boards/" + board.getId();
        } catch (ResponseStatusException rse) {
            model.addAttribute("error", rse.getReason());
            model.addAttribute("boards", boardService.findByOwner(user));
            model.addAttribute("pinDto", pinDto);
            return "pins/create";
        }
        catch (Exception e) {
            model.addAttribute("error", "Pin oluşturulamadı: " + e.getMessage());
            model.addAttribute("boards", boardService.findByOwner(user));
            model.addAttribute("pinDto", pinDto);
            return "pins/create";
        }
    }

    @GetMapping("/{id}")
    public String viewPin(@PathVariable Long id, Model model, Principal principal, Authentication authentication) {
        Pin pin = pinService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin bulunamadı"));

        // Mevcut kullanıcıyı al ve admin durumunu kontrol et
        User currentUser = getUserFromAuthentication(principal, authentication);
        boolean isOwner = false;
        boolean isAdmin = false;

        if (currentUser != null) {
            // Pin sahibi silinmiş mi null kontrolü
            isOwner = pin.getOwner() != null && pin.getOwner().getId().equals(currentUser.getId());
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        model.addAttribute("pin", pin);
        model.addAttribute("imgBase64", Base64Util.encodeBase64(pin.getImageData()));
        model.addAttribute("currentUser", currentUser); // HTML'de kullanıcı ID'si kontrolü için kullanılabilir
        model.addAttribute("isOwner", isOwner); // HTML'de sahiplik kontrolü için kullanılabilir
        model.addAttribute("isAdmin", isAdmin); // HTML'de admin yetkisi kontrolü için kullanılabilir

        return "pins/view";
    }

    @GetMapping("/search")
    public String searchPins(@RequestParam String query, Model model, Principal principal, Authentication authentication) {
        List<Pin> results = pinService.searchByTitle(query);

        // Mevcut kullanıcıyı al ve admin durumunu kontrol et
        User currentUser = getUserFromAuthentication(principal, authentication);
        boolean isAdmin = false;

        if (currentUser != null && authentication != null) {
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        // Pinleri Base64 formatına dönüştürerek modele ekle
        List<MainController.PinWithBase64> wrappers = results.stream()
                .map(p -> new MainController.PinWithBase64(p, Base64Util.encodeBase64(p.getImageData())))
                .collect(Collectors.toList());

        model.addAttribute("pins", wrappers);
        model.addAttribute("currentUser", currentUser); // HTML'de kullanıcı ID'si kontrolü için kullanılabilir
        model.addAttribute("isAdmin", isAdmin); // HTML'de admin yetkisi kontrolü için kullanılabilir
        model.addAttribute("searchQuery", query);

        return "pins/list";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or @pinService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String editPinForm(@PathVariable Long id,
                              Model model,
                              Principal principal,
                              Authentication authentication) {
        User currentUser = getUserFromAuthentication(principal, authentication);
        if (currentUser == null) return "redirect:/login";
        Pin pin = pinService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin bulunamadı"));
        PinDto pinDto = new PinDto();
        pinDto.setId(pin.getId());
        pinDto.setTitle(pin.getTitle());
        pinDto.setDescription(pin.getDescription());
        pinDto.setBoardId(pin.getBoard().getId());
        // Adminse tüm panoları değilse sadece kendi panolarını göster
        List<Board> boards = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                ? boardService.findAll()
                : boardService.findByOwner(currentUser);
        model.addAttribute("boards", boards);
        model.addAttribute("pinDto", pinDto);
        return "pins/edit";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or @pinService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String editPin(@PathVariable Long id,
                          @ModelAttribute PinDto pinDto,
                          Principal principal,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        User currentUser = getUserFromAuthentication(principal, authentication);
        if (currentUser == null) return "redirect:/login";
        Pin pin = pinService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin bulunamadı"));
        try {
            Board board = boardService.findById(pinDto.getBoardId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hedef pano bulunamadı"));
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            // Admin değilse ve hedef pano kullanıcının kendi panosu değilse izin verme
            if (!isAdmin && (board.getOwner() == null || !board.getOwner().getId().equals(currentUser.getId()))) {
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu panoya pini taşıma yetkiniz yok.");
            }
            pin.setTitle(pinDto.getTitle());
            pin.setDescription(pinDto.getDescription());
            pin.setBoard(board);
            MultipartFile file = pinDto.getImage();
            if (file != null && !file.isEmpty()) {
                pin.setImageData(file.getBytes());
            }
            pinService.save(pin);
            redirectAttributes.addFlashAttribute("message", "Pin başarıyla güncellendi.");
            return "redirect:/pins/" + id;
        } catch (ResponseStatusException rse) {
            model.addAttribute("error", rse.getReason());
             // Hata durumunda panoları tekrar yükle
            model.addAttribute("boards",
                    authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                            ? boardService.findAll()
                            : boardService.findByOwner(currentUser)
            );
            model.addAttribute("pinDto", pinDto); // Dto'yu tekrar modele ekle
            return "pins/edit";
        } catch (Exception e) {
            model.addAttribute("error", "Pin güncellenemedi: " + e.getMessage());
             // Hata durumunda panoları tekrar yükle
             model.addAttribute("boards",
                    authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                            ? boardService.findAll()
                            : boardService.findByOwner(currentUser)
            );
             model.addAttribute("pinDto", pinDto); // Dto'yu tekrar modele ekle
            return "pins/edit";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') or @pinService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String deletePin(@PathVariable Long id,
                            Principal principal,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User currentUser = getUserFromAuthentication(principal, authentication);
        if (currentUser == null) return "redirect:/login";
        // PreAuthorize anotasyonu kontrolü zaten yaptı findById çağrısına gerek yok
        try {
             // Silinecek pinin pano ID'sini alalım
            Long boardId = pinService.findById(id).map(Pin::getBoard).map(Board::getId).orElse(null);

            pinService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Pin başarıyla silindi.");
            return boardId != null
                    ? "redirect:/boards/" + boardId
                    : "redirect:/"; // Panosu silinmişse ana sayfaya dön
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Pin silinemedi: " + e.getMessage());
            return "redirect:/pins/" + id; // Hata olursa pin detay sayfasına geri dön
        }
    }
}