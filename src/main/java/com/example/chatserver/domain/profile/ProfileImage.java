package com.example.chatserver.domain.profile;

import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.global.common.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storeFileName;  // S3에 저장된 고유 파일명 (UUID)
    private String uploadFileName; // 사용자가 올린 실제 파일명
    private String imageUrl;       // S3에서 받아온 Full URL

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public ProfileImage(String storeFileName, String uploadFileName, String imageUrl, Member member) {
        this.storeFileName = storeFileName;
        this.uploadFileName = uploadFileName;
        this.imageUrl = imageUrl;
        this.member = member;
    }
}
