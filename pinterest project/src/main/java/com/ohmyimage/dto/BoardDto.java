package com.ohmyimage.dto;

import lombok.Data;

@Data
public class BoardDto {
    private Long id;
    private String name;
    private Long ownerId;
}