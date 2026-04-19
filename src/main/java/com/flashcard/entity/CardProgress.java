package com.flashcard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "card_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","card_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    private double easeFactor = 2.5;
    private int intervalDays = 1;
    private int repetitions = 0;
    private LocalDate nextReviewDate = LocalDate.now();
    private LocalDateTime lastReviewed;
}