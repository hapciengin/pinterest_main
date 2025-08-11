package com.ohmyimage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {
    private Long id;

    @NotBlank(message = "Kullanıcı adı boş olamaz")
    @Size(min = 3, max = 20, message = "3-20 karakter arasında olmalı")
    private String username;

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta girin")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, message = "En az 6 karakter olmalı")
    private String password;
}