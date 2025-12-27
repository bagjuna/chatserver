package com.example.chatserver.domain.member.entity;

import com.example.chatserver.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor

@NoArgsConstructor(access = PROTECTED)
@Getter
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;


    @Column(nullable = false, unique = true, updatable = false)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;




}
