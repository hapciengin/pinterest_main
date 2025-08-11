package com.ohmyimage.controller;

import com.ohmyimage.model.Board;
import com.ohmyimage.model.Pin;
import com.ohmyimage.model.User;
import com.ohmyimage.service.BoardService;
import com.ohmyimage.service.PinService;
import com.ohmyimage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @Autowired private UserService userService;
    @Autowired private BoardService boardService;
    @Autowired private PinService pinService;

    @GetMapping("/search")
    public String searchAll(
            @RequestParam("q") String query,
            Model model) {

        List<User>  users  = userService.searchByUsername(query);
        List<Board> boards = boardService.searchByName(query);
        List<Pin>   pins   = pinService.searchByTitle(query);

        model.addAttribute("query",  query);
        model.addAttribute("users",  users);
        model.addAttribute("boards", boards);
        model.addAttribute("pins",   pins);
        return "search/results";
    }
}