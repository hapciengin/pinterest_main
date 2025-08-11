package com.ohmyimage.repository;

import com.ohmyimage.model.Comment;
import com.ohmyimage.model.Pin;
import com.ohmyimage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPin(Pin pin);
    List<Comment> findByUser(User user);
}