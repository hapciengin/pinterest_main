package com.ohmyimage.service;

import com.ohmyimage.model.Board;
import com.ohmyimage.model.Pin;
import com.ohmyimage.repository.PinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PinService {
    @Autowired
    private PinRepository pinRepository;

    @Transactional
    public Pin save(Pin pin) {
        return pinRepository.save(pin);
    }

    @Transactional(readOnly = true)
    public Optional<Pin> findById(Long id) {
        return pinRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Pin> searchByTitle(String query) {
        return pinRepository.findByTitleContainingIgnoreCase(query);
    }

    @Transactional(readOnly = true)
    public List<Pin> findByBoard(Board board) {
        return pinRepository.findByBoard(board);
    }

    @Transactional(readOnly = true)
    public List<Pin> findAll() {
        return pinRepository.findAll();
    }

    @Transactional
    public void deleteById(Long id) {
        pinRepository.deleteById(id);
    }
}