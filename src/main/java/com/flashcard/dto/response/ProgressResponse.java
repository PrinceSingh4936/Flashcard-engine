package com.flashcard.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProgressResponse {
    private Long cardId;
    private String masteryLabel;
    private int intervalDays;
    private double easeFactor;
    private int repetitions;
    private String nextReviewDate;
}