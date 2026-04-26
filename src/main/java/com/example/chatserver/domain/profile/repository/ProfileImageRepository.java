package com.example.chatserver.domain.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chatserver.domain.profile.ProfileImage;

@Repository
public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {

}
