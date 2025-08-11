package com.ohmyimage.repository;

import com.ohmyimage.model.Pin;
import com.ohmyimage.model.Board;
import com.ohmyimage.model.User; 
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PinRepository extends JpaRepository<Pin, Long> {
    List<Pin> findByTitleContainingIgnoreCase(String query);
    List<Pin> findByBoard(Board board);
    List<Pin> findByOwner(User owner);
}