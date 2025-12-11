package com.example.chatserver.global.security.userdetails;

import com.example.chatserver.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return CustomUserDetails.of(memberRepository.findByPublicId(username).orElseThrow(() -> new UsernameNotFoundException("해당하는 ID의 유저가 존재하지 않습니다.")));
        } catch (Exception e) {
            log.debug("Username not found", e);
            throw new UsernameNotFoundException("Username not found");
        }
    }
}
