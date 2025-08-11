package com.ohmyimage.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImageService {

    /**
     * MultipartFile'dan byte[] dönüştürür
     */
    public byte[] convertToBytes(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new byte[0];
        }
        return file.getBytes();
    }
}