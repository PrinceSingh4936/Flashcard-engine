package com.flashcard.dto.response;

import com.flashcard.enums.CardType;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CardResponse {
    private Long id;
    private String front;
    private String back;
    private CardType cardType;
    // progress fields (null if never reviewed)
    private String masteryLabel;
    private Integer intervalDays;
    private Double easeFactor;
    private String nextReviewDate;
}