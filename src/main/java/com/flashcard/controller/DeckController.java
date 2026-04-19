package com.flashcard.controller;

import com.flashcard.dto.response.CardResponse;
import com.flashcard.dto.response.DeckResponse;
import com.flashcard.service.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DeckResponse> createDeck(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String subject,
            @RequestParam MultipartFile file) {

        return ResponseEntity.ok(
                deckService.createDeckFromPdf(userDetails.getUsername(), title, subject, file)
        );
    }

    @GetMapping
    public ResponseEntity<List<DeckResponse>> getMyDecks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(deckService.getUserDecks(userDetails.getUsername()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DeckResponse>> search(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String q) {
        return ResponseEntity.ok(deckService.searchDecks(userDetails.getUsername(), q));
    }

    @GetMapping("/{deckId}/cards")
    public ResponseEntity<List<CardResponse>> getDeckCards(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long deckId) {
        return ResponseEntity.ok(deckService.getDeckCards(userDetails.getUsername(), deckId));
    }
}