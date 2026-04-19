package com.flashcard.repository;

import com.flashcard.entity.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByUserIdOrderByLastStudiedDesc(Long userId);
    List<Deck> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title);
}