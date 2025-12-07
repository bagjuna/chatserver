package com.example.chatserver.domain.member.controller;

import com.example.chatserver.global.security.auth.JwtTokenProvider;
import com.example.chatserver.domain.member.entity.Member;
import com.example.chatserver.domain.member.dto.MemberListResDto;
import com.example.chatserver.domain.member.dto.MemberLoginReqDto;
import com.example.chatserver.domain.member.dto.MemberSaveReqDto;
import com.example.chatserver.domain.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/create")
    public ResponseEntity<?> memberCreate(@RequestBody MemberSaveReqDto memberSaveReqDto) {
        Member member = memberService.create(memberSaveReqDto);
        return new ResponseEntity<>(member.getId(), HttpStatus.CREATED);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginReqDto memberLoginReqDto) {
        // email과 password 검증
        log.info("email : {}, password : {}", memberLoginReqDto.getEmail(), memberLoginReqDto.getPassword());
        Member member = memberService.login(memberLoginReqDto);

        // 일치할 경우 accessToken 발급
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", member.getId());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<?> memberList() {
        List<MemberListResDto> dtos = memberService.findAll();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }



}
