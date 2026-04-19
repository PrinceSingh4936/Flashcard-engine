package com.flashcard.entity;

import com.flashcard.enums.CardType;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String front;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String back;

    @Enumerated(EnumType.STRING)
    private CardType cardType = CardType.CONCEPT;
}