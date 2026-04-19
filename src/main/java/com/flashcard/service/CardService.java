package com.flashcard.service;

import com.flashcard.dto.response.CardResponse;
import com.flashcard.entity.Card;
import com.flashcard.entity.User;
import com.flashcard.enums.CardType;
import com.flashcard.repository.CardProgressRepository;
import com.flashcard.repository.CardRepository;
import com.flashcard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final UserRepository userRepository;
    private final SpacedRepetitionService spacedRepetitionService;

    public CardResponse getCard(String userEmail, Long cardId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        return toCardResponse(user.getId(), card);
    }

    public List<CardResponse> getCardsByDeck(String userEmail, Long deckId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cardRepository.findByDeckId(deckId)
                .stream()
                .map(card -> toCardResponse(user.getId(), card))
                .toList();
    }

    public CardResponse updateCard(String userEmail, Long cardId,
                                   String front, String back, String cardType) {
        userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (front != null && !front.isBlank())    card.setFront(front);
        if (back != null && !back.isBlank())       card.setBack(back);
        if (cardType != null && !cardType.isBlank()) {
            try {
                card.setCardType(CardType.valueOf(cardType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        cardRepository.save(card);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return toCardResponse(user.getId(), card);
    }

    public void deleteCard(String userEmail, Long cardId) {
        userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Remove progress records first to avoid FK constraint
        cardProgressRepository.findByUserIdAndCardId(
                userRepository.findByEmail(userEmail).get().getId(), cardId
        ).ifPresent(cardProgressRepository::delete);

        cardRepository.delete(card);
    }

    public List<CardResponse> getNewCards(String userEmail, Long deckId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // New = cards with no progress record yet
        List<Long> reviewedCardIds = cardProgressRepository
                .findByUserIdAndCardDeckId(user.getId(), deckId)
                .stream()
                .map(cp -> cp.getCard().getId())
                .toList();

        return cardRepository.findByDeckId(deckId)
                .stream()
                .filter(card -> !reviewedCardIds.contains(card.getId()))
                .map(card -> toCardResponse(user.getId(), card))
                .toList();
    }

    private CardResponse toCardResponse(Long userId, Card card) {
        var progress = cardProgressRepository
                .findByUserIdAndCardId(userId, card.getId())
                .orElse(null);

        return CardResponse.builder()
                .id(card.getId())
                .front(card.getFront())
                .back(card.getBack())
                .cardType(card.getCardType())
                .masteryLabel(progress != null
                        ? spacedRepetitionService.getMasteryLabel(progress)
                        : "NEW")
                .intervalDays(progress != null ? progress.getIntervalDays() : 0)
                .easeFactor(progress != null ? progress.getEaseFactor() : 2.5)
                .nextReviewDate(progress != null
                        ? progress.getNextReviewDate().toString()
                        : LocalDate.now().toString())
                .build();
    }
}