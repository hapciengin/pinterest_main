package com.ohmyimage.service;

import com.ohmyimage.model.Comment;
import com.ohmyimage.model.Pin;
import com.ohmyimage.model.User;
import com.ohmyimage.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Transactional
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    // DELETE yetki kontrolü için gerekli:
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    public List<Comment> findByPin(Pin pin) {
        return commentRepository.findByPin(pin);
    }

    public List<Comment> findByUser(User user) {
        return commentRepository.findByUser(user);
    }

    @Transactional
    public void deleteById(Long id) {
        commentRepository.deleteById(id);
    }
}