package com.flashcard.service;

import com.flashcard.dto.response.CardResponse;
import com.flashcard.dto.response.DeckResponse;
import com.flashcard.entity.*;
import com.flashcard.enums.CardType;
import com.flashcard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final UserRepository userRepository;
    private final PdfIngestionService pdfIngestionService;
    private final GeminiApiService geminiApiService;
    private final SpacedRepetitionService spacedRepetitionService;

    public DeckResponse createDeckFromPdf(String userEmail, String title,
                                          String subject, MultipartFile file) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Chunk PDF directly by pages (memory efficient)
        List<String> chunks = pdfIngestionService.chunkText(file);

        // 2. Create deck
        Deck deck = Deck.builder()
                .user(user)
                .title(title)
                .subject(subject)
                .pdfUrl("local")
                .build();
        deck = deckRepository.save(deck);

        // 3. Generate cards for each chunk
        List<Card> allCards = new ArrayList<>();
        for (String chunk : chunks) {
            List<Map<String, String>> generated = geminiApiService.generateCards(chunk);
            for (Map<String, String> cardData : generated) {
                Card card = Card.builder()
                        .deck(deck)
                        .front(cardData.get("front"))
                        .back(cardData.get("back"))
                        .cardType(parseCardType(cardData.get("cardType")))
                        .build();
                allCards.add(cardRepository.save(card));
            }
        }

        deck.setCardCount(allCards.size());
        deckRepository.save(deck);

        log.info("Created deck '{}' with {} cards", title, allCards.size());
        return toDeckResponse(deck, user.getId());
    }

    public List<DeckResponse> getUserDecks(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return deckRepository.findByUserIdOrderByLastStudiedDesc(user.getId())
                .stream()
                .map(deck -> toDeckResponse(deck, user.getId()))
                .toList();
    }

    public List<DeckResponse> searchDecks(String userEmail, String query) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return deckRepository.findByUserIdAndTitleContainingIgnoreCase(user.getId(), query)
                .stream()
                .map(deck -> toDeckResponse(deck, user.getId()))
                .toList();
    }

    public List<CardResponse> getDeckCards(String userEmail, Long deckId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cardRepository.findByDeckId(deckId).stream()
                .map(card -> {
                    var progress = cardProgressRepository
                            .findByUserIdAndCardId(user.getId(), card.getId())
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
                })
                .toList();
    }

    private DeckResponse toDeckResponse(Deck deck, Long userId) {
        int mastered = cardProgressRepository.countMastered(userId, deck.getId());
        int shaky    = cardProgressRepository.countShaky(userId, deck.getId());
        int due      = cardProgressRepository.findDueCards(userId, LocalDate.now()).size();

        return DeckResponse.builder()
                .id(deck.getId())
                .title(deck.getTitle())
                .subject(deck.getSubject())
                .cardCount(deck.getCardCount())
                .masteredCount(mastered)
                .shakyCount(shaky)
                .dueCount(due)
                .createdAt(deck.getCreatedAt())
                .lastStudied(deck.getLastStudied())
                .build();
    }

    private CardType parseCardType(String type) {
        try {
            return CardType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return CardType.CONCEPT;
        }
    }
}