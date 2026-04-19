package com.flashcard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "review_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    private int rating; // 0-5 (SM-2 scale)
    private LocalDateTime reviewedAt = LocalDateTime.now();
}