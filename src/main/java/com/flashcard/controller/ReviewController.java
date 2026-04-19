package com.flashcard.controller;

import com.flashcard.dto.request.ReviewRequest;
import com.flashcard.dto.response.CardResponse;
import com.flashcard.dto.response.ProgressResponse;
import com.flashcard.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/due")
    public ResponseEntity<List<CardResponse>> getDueCards(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reviewService.getDueCards(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ProgressResponse> submitReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(
                reviewService.submitReview(
                        userDetails.getUsername(),
                        request.getCardId(),
                        request.getRating())
        );
    }
}