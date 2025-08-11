package com.ohmyimage.service;

import com.ohmyimage.model.Board;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;

    public Board save(Board board) {
        return boardRepository.save(board);
    }

    public List<Board> findByOwner(User user) {
        return boardRepository.findByOwner(user);
    }

    public Optional<Board> findById(Long id) {
        return boardRepository.findById(id);
    }

    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    public void deleteById(Long id) {
        boardRepository.deleteById(id);
    }

    public boolean existsByNameAndOwner(String name, User owner) {
        return boardRepository.existsByNameAndOwner(name, owner);
    }

    // universal search için eklendi
    public List<Board> searchByName(String namePart) {
        return boardRepository.findByNameContainingIgnoreCase(namePart);
    }
}