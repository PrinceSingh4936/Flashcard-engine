package com.flashcard.controller;

import com.flashcard.dto.response.CardResponse;
import com.flashcard.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCard(userDetails.getUsername(), cardId));
    }

    @GetMapping("/deck/{deckId}")
    public ResponseEntity<List<CardResponse>> getCardsByDeck(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long deckId) {
        return ResponseEntity.ok(cardService.getCardsByDeck(userDetails.getUsername(), deckId));
    }

    @GetMapping("/deck/{deckId}/new")
    public ResponseEntity<List<CardResponse>> getNewCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long deckId) {
        return ResponseEntity.ok(cardService.getNewCards(userDetails.getUsername(), deckId));
    }

    @PatchMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cardService.updateCard(
                userDetails.getUsername(),
                cardId,
                body.get("front"),
                body.get("back"),
                body.get("cardType")
        ));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId) {
        cardService.deleteCard(userDetails.getUsername(), cardId);
        return ResponseEntity.noContent().build();
    }
}