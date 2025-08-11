package com.ohmyimage.repository;

import com.ohmyimage.model.Board;
import com.ohmyimage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByOwner(User user);
    boolean existsByNameAndOwner(String name, User owner);

    // universal search için
    List<Board> findByNameContainingIgnoreCase(String namePart);
}