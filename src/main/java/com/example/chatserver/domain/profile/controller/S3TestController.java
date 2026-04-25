package com.example.chatserver.domain.profile.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.chatserver.domain.profile.service.ImageStorage;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class S3TestController {

    private final ImageStorage imageStorage; // 인터페이스 주입

    @PostMapping("/test/upload")
    public String uploadTest(@RequestParam("file") MultipartFile file) {
        // "test" 폴더에 업로드 시도
        return imageStorage.upload(file, "test");
    }
}
