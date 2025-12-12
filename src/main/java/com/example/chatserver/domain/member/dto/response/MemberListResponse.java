package com.example.chatserver.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberListResponse {

    private Long id;
    private String name;
    private String email;



}
