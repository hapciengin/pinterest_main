package com.ohmyimage.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PinDto {
    private Long id;
    private String title;
    private String description;
    private MultipartFile image;
    private Long boardId;
}