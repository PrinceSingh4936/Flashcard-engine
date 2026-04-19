package com.flashcard.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class DeckResponse {
    private Long id;
    private String title;
    private String subject;
    private int cardCount;
    private int masteredCount;
    private int shakyCount;
    private int dueCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastStudied;
}