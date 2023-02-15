package com.secondproject.monthlycoffee.dto.member;

import java.time.LocalDate;

import com.secondproject.monthlycoffee.entity.MemberInfo;
import com.secondproject.monthlycoffee.entity.type.Gender;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberDto(
    @Schema(description = "회원 식별 번호")
    Long id,
    @Schema(description = "회원 UID")
    String uid,
    @Schema(description = "닉네임")
    String nickname,
    @Schema(description = "생일")
    LocalDate birth,
    @Schema(description = "성별")
    Gender gender
) {
    public MemberDto(MemberInfo entity) {
        this(
            entity.getId(), 
            entity.getUid(),
            entity.getNickname(),
            entity.getBirth(),
            entity.getGender()
            );
    }    
}
