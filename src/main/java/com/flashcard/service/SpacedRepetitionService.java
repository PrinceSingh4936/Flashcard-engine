package com.flashcard.service;

import com.flashcard.entity.Card;
import com.flashcard.entity.CardProgress;
import com.flashcard.entity.ReviewLog;
import com.flashcard.entity.User;
import com.flashcard.repository.CardProgressRepository;
import com.flashcard.repository.ReviewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SpacedRepetitionService {

    private final CardProgressRepository cardProgressRepository;
    private final ReviewLogRepository reviewLogRepository;

    /**
     * SM-2 Algorithm
     * rating: 0 = complete blackout, 5 = perfect recall
     * 0-2 = failed → reset repetitions, review again tomorrow
     * 3-5 = passed → increase interval based on ease factor
     */
    public CardProgress processReview(User user, Card card, int rating) {
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("Rating must be 0-5");

        CardProgress progress = cardProgressRepository
                .findByUserIdAndCardId(user.getId(), card.getId())
                .orElseGet(() -> CardProgress.builder()
                        .user(user)
                        .card(card)
                        .easeFactor(2.5)
                        .intervalDays(1)
                        .repetitions(0)
                        .nextReviewDate(LocalDate.now())
                        .build());

        // Log this review
        ReviewLog log = ReviewLog.builder()
                .user(user)
                .card(card)
                .rating(rating)
                .reviewedAt(LocalDateTime.now())
                .build();
        reviewLogRepository.save(log);

        // SM-2 core logic
        if (rating < 3) {
            // Failed recall — reset
            progress.setRepetitions(0);
            progress.setIntervalDays(1);
        } else {
            // Passed recall — advance
            int reps = progress.getRepetitions();
            if (reps == 0) {
                progress.setIntervalDays(1);
            } else if (reps == 1) {
                progress.setIntervalDays(6);
            } else {
                int newInterval = (int) Math.round(progress.getIntervalDays() * progress.getEaseFactor());
                progress.setIntervalDays(newInterval);
            }
            progress.setRepetitions(reps + 1);
        }

        // Update ease factor (always, regardless of pass/fail)
        double newEase = progress.getEaseFactor()
                + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02));
        progress.setEaseFactor(Math.max(1.3, newEase)); // floor at 1.3

        progress.setNextReviewDate(LocalDate.now().plusDays(progress.getIntervalDays()));
        progress.setLastReviewed(LocalDateTime.now());

        return cardProgressRepository.save(progress);
    }

    public String getMasteryLabel(CardProgress progress) {
        int interval = progress.getIntervalDays();
        if (interval >= 21) return "MASTERED";
        if (interval >= 7)  return "STRONG";
        if (progress.getEaseFactor() < 2.0) return "SHAKY";
        return "LEARNING";
    }
}