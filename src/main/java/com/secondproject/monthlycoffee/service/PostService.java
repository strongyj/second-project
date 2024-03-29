package com.secondproject.monthlycoffee.service;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.secondproject.monthlycoffee.config.security.JwtProperties;
import com.secondproject.monthlycoffee.config.security.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.secondproject.monthlycoffee.dto.post.PostCreateDto;
import com.secondproject.monthlycoffee.dto.post.PostDeleteDto;
import com.secondproject.monthlycoffee.dto.post.PostModifyDto;
import com.secondproject.monthlycoffee.dto.post.PostBasicDto;
import com.secondproject.monthlycoffee.dto.post.PostDetailDto;
import com.secondproject.monthlycoffee.entity.ExpenseInfo;
import com.secondproject.monthlycoffee.entity.MemberInfo;
import com.secondproject.monthlycoffee.entity.PostInfo;
import com.secondproject.monthlycoffee.repository.CommentInfoRepository;
import com.secondproject.monthlycoffee.repository.ExpenseInfoRepository;
import com.secondproject.monthlycoffee.repository.LovePostInfoRepository;
import com.secondproject.monthlycoffee.repository.MemberInfoRepository;
import com.secondproject.monthlycoffee.repository.PostInfoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostInfoRepository postRepo;
    private final MemberInfoRepository memberRepo;
    private final ExpenseInfoRepository expenseRepo;
    private final CommentInfoRepository commentRepo;
    private final LovePostInfoRepository lovePostRepo;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public Page<PostBasicDto> getAllPost(Pageable pageable) {
        return postRepo.findAll(pageable).map(p -> new PostBasicDto(p, lovePostRepo.countByPost(p), commentRepo.countByPost(p)));
    }

    @Transactional(readOnly = true)
    public PostDetailDto getPostDetail(Long id, String bearerAccess) {
        PostInfo post = postRepo.findById(id).orElseThrow();
        if(StringUtils.hasText(bearerAccess) && bearerAccess.startsWith(JwtProperties.ACCESS_TOKEN_PREFIX)) {
            String access = jwtUtil.resolve(bearerAccess);
            Long memberId = jwtUtil.verifyAccessAndExtractClaim(access);
            return new PostDetailDto(post, lovePostRepo.existsByPostAndMemberId(post, memberId));
        }
        return new PostDetailDto(post);
    }

    public PostDetailDto create(PostCreateDto post, Long memberId) {
        ExpenseInfo expense = expenseRepo.findByIdAndMemberId(post.expenseId(), memberId).orElseThrow(() -> new NoSuchElementException("해당 회원의 지출이 아닙니다."));
        if(postRepo.existsByExpense(expense)) {
            throw new IllegalArgumentException("지출 하나당 한개의 게시글만 등록할 수 있습니다.");
        }
        PostInfo newPost = new PostInfo(post.content(), expense);
        postRepo.save(newPost);
        return new PostDetailDto(newPost);
    }

    public PostDetailDto modify(Long postId, Long memberId, PostModifyDto modifyPost) {
        PostInfo post = postRepo.findById(postId).orElseThrow();
        checkPostAuthor(post, memberId);

        post.modifyContent(modifyPost.content());
        return new PostDetailDto(post);
    }

    public PostDeleteDto delete(Long id, Long memberId) {
        PostInfo post = postRepo.findById(id).orElseThrow();
        checkPostAuthor(post, memberId);

        commentRepo.deleteAllByPostInBatch(post);
        lovePostRepo.deleteAllByPostInBatch(post);
        postRepo.delete(post);
        return new PostDeleteDto(post.getId(), "삭제되었습니다.");
    }

    private void checkPostAuthor(PostInfo post, Long memberId) {
        MemberInfo member = Optional.ofNullable(post.getExpense()).map(ExpenseInfo::getMember)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원의 게시글입니다."));
        if (member.getId() != memberId)
            throw new IllegalArgumentException("게시글을 작성한 회원이 아닙니다.");
    }
}
