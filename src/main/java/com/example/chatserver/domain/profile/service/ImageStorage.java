package com.example.chatserver.domain.profile.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {
    /**
     * 파일을 업로드하고 저장된 경로(URL)를 반환합니다.
     * @param file 업로드할 파일
     * @param directory S3 내의 저장 폴더 (예: "profiles", "posts")
     */
    String upload(MultipartFile file, String directory);
    
    // 나중에 파일 삭제 기능이 필요하면 여기에 추가하면 됩니다.
    // void delete(String fileName); 
}
