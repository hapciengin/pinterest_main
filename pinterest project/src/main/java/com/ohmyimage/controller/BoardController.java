package com.ohmyimage.controller;

import com.ohmyimage.dto.BoardDto;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller // Bu sınıf, gelen HTTP isteklerini karşılayan bir Controller'dır
@RequestMapping("/boards") // Bu controller altındaki tüm endpointler /boards ile başlar
public class BoardController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private UserService userService;

    @Autowired
    private PinService pinService;

    // Authentication ve Principal objelerinden kullanıcıyı çekmek için kullanılan yardımcı metot
    private User getUserFromAuthentication(Principal principal, Authentication authentication) {
        if (principal == null || authentication == null) return null;

        Object p = authentication.getPrincipal(); // Kullanıcı bilgisi (OAuth2User ya da User olabilir)
        String id;
        User u = null;

        // Eğer kullanıcı OAuth2 ile giriş yaptıysa
        if (p instanceof OAuth2User) {
            id = ((OAuth2User) p).getAttribute("email");
            u = userService.findByEmail(id);
        }
        // Eğer klasik User objesiyle giriş yaptıysa
        else if (p instanceof User) {
            id = ((User) p).getUsername();
            u = userService.findByUsername(id);
        }
        // Diğer durumlar (örneğin test, custom auth vs.)
        else {
            id = principal.getName();
            u = userService.findByUsername(id);
            if (u == null) u = userService.findByEmail(id);
        }
        return u;
    }

    // Kullanıcının tüm panolarını gösterir
    @GetMapping
    public String userBoards(Principal principal, Authentication auth, Model model) {
        User user = getUserFromAuthentication(principal, auth);
        if (user == null) return "redirect:/login"; // Eğer kullanıcı yoksa login sayfasına yönlendir
        model.addAttribute("boards", boardService.findByOwner(user)); // Kullanıcıya ait panolar
        return "user/boards";
    }

    // Yeni pano oluşturma formunu gösterir
    @GetMapping("/create")
    public String createForm(Principal principal, Authentication auth, Model model) {
        if (getUserFromAuthentication(principal, auth) == null) return "redirect:/login";
        model.addAttribute("boardDto", new BoardDto()); // Boş bir DTO ile formu başlat
        return "boards/create";
    }

    // Pano oluşturma işlemini yapar
    @PostMapping("/create")
    public String create(@ModelAttribute BoardDto dto,
                         Principal principal, Authentication auth, Model model) {
        User user = getUserFromAuthentication(principal, auth);
        if (user == null) return "redirect:/login";

        // Aynı isimde pano daha önce oluşturulmuşsa hata ver
        if (boardService.existsByNameAndOwner(dto.getName(), user)) {
            model.addAttribute("error", "Bu isimde bir panonuz zaten mevcut.");
            model.addAttribute("boardDto", dto);
            return "boards/create";
        }

        // Yeni panoyu kaydet
        boardService.save(Board.builder().name(dto.getName()).owner(user).build());
        return "redirect:/boards"; // Oluşturduktan sonra pano listesine git
    }

    // Belirli bir panoyu görüntüleme sayfası
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @boardService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String viewBoard(@PathVariable Long id,
                            Principal principal, Authentication auth, Model model) {
        User user = getUserFromAuthentication(principal, auth);
        if (user == null) return "redirect:/login";

        // ID'ye göre pano bulunur, yoksa 404 fırlatılır
        Board board = boardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pano bulunamadı"));

        // Panoya ait pin'ler bulunur
        List<Pin> pins = pinService.findByBoard(board);

        // Her bir pin'in resmi base64 formatına dönüştürülerek frontend'e gönderilir
        List<MainController.PinWithBase64> wrapped = pins.stream()
                .map(p -> new MainController.PinWithBase64(p, Base64Util.encodeBase64(p.getImageData())))
                .collect(Collectors.toList());

        model.addAttribute("board", board);
        model.addAttribute("pins", wrapped);
        return "boards/view"; // boards klasöründeki view.html sayfası
    }

    // Pano düzenleme formunu gösterir
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or @boardService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String editForm(@PathVariable Long id, Model model) {
        // DTO'ya dönüştürerek formu başlatır
        BoardDto dto = boardService.findById(id)
                .map(b -> {
                    BoardDto d = new BoardDto();
                    d.setId(b.getId());
                    d.setName(b.getName());
                    return d;
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pano bulunamadı"));
        model.addAttribute("boardDto", dto);
        return "boards/edit";
    }

    // Düzenleme işlemini gerçekleştirir
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or @boardService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String edit(@PathVariable Long id,
                       @ModelAttribute BoardDto dto,
                       Model model) {

        Board board = boardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pano bulunamadı"));

        // Aynı isimde başka pano var mı kontrol edilir
        if (!board.getName().equals(dto.getName())
                && boardService.existsByNameAndOwner(dto.getName(), board.getOwner())) {
            model.addAttribute("error", "Bu isimde başka bir panonuz zaten mevcut.");
            model.addAttribute("boardDto", dto);
            return "boards/edit";
        }

        // Yeni isim setlenip kaydedilir
        board.setName(dto.getName());
        boardService.save(board);
        return "redirect:/boards/" + id; // Düzenlenen panoya yönlendirilir
    }

    // Pano silme işlemi
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') or @boardService.findById(#id).orElse(null)?.owner?.username == authentication.name")
    public String delete(@PathVariable Long id,
                         Principal principal, Authentication auth,
                         RedirectAttributes ra) {
        User user = getUserFromAuthentication(principal, auth);
        if (user == null) return "redirect:/login";

        boardService.deleteById(id); // Pano silinir
        ra.addFlashAttribute("message", "Pano silindi."); // Silinme mesajı gösterilir
        return "redirect:/boards"; // Pano listesine dönülür
    }
}