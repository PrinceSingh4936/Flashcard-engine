package com.flashcard.service;

import com.flashcard.dto.response.CardResponse;
import com.flashcard.dto.response.ProgressResponse;
import com.flashcard.entity.*;
import com.flashcard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final DeckRepository deckRepository;
    private final SpacedRepetitionService spacedRepetitionService;

    public List<CardResponse> getDueCards(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cardProgressRepository
                .findDueCards(user.getId(), LocalDate.now())
                .stream()
                .map(cp -> CardResponse.builder()
                        .id(cp.getCard().getId())
                        .front(cp.getCard().getFront())
                        .back(cp.getCard().getBack())
                        .cardType(cp.getCard().getCardType())
                        .masteryLabel(spacedRepetitionService.getMasteryLabel(cp))
                        .intervalDays(cp.getIntervalDays())
                        .easeFactor(cp.getEaseFactor())
                        .nextReviewDate(cp.getNextReviewDate().toString())
                        .build())
                .toList();
    }

    public ProgressResponse submitReview(String userEmail, Long cardId, int rating) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Update deck lastStudied
        Deck deck = card.getDeck();
        deck.setLastStudied(LocalDateTime.now());
        deckRepository.save(deck);

        // Update streak
        updateStreak(user);

        CardProgress progress = spacedRepetitionService.processReview(user, card, rating);

        return ProgressResponse.builder()
                .cardId(cardId)
                .masteryLabel(spacedRepetitionService.getMasteryLabel(progress))
                .intervalDays(progress.getIntervalDays())
                .easeFactor(progress.getEaseFactor())
                .repetitions(progress.getRepetitions())
                .nextReviewDate(progress.getNextReviewDate().toString())
                .build();
    }

    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActive();

        if (lastActive == null || lastActive.isBefore(today.minusDays(1))) {
            user.setStreak(1);
        } else if (lastActive.isEqual(today.minusDays(1))) {
            user.setStreak(user.getStreak() + 1);
        }
        // if lastActive == today, streak unchanged

        user.setLastActive(today);
        userRepository.save(user);
    }
}