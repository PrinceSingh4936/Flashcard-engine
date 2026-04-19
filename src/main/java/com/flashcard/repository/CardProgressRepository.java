package com.flashcard.repository;

import com.flashcard.entity.CardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardProgressRepository extends JpaRepository<CardProgress, Long> {

    Optional<CardProgress> findByUserIdAndCardId(Long userId, Long cardId);

    // Cards due today or overdue
    @Query("SELECT cp FROM CardProgress cp WHERE cp.user.id = :userId AND cp.nextReviewDate <= :today")
    List<CardProgress> findDueCards(@Param("userId") Long userId, @Param("today") LocalDate today);

    // Mastered = interval >= 21 days
    @Query("SELECT COUNT(cp) FROM CardProgress cp WHERE cp.user.id = :userId AND cp.card.deck.id = :deckId AND cp.intervalDays >= 21")
    int countMastered(@Param("userId") Long userId, @Param("deckId") Long deckId);

    // Shaky = repetitions > 0 but ease factor dropped low
    @Query("SELECT COUNT(cp) FROM CardProgress cp WHERE cp.user.id = :userId AND cp.card.deck.id = :deckId AND cp.easeFactor < 2.0 AND cp.repetitions > 0")
    int countShaky(@Param("userId") Long userId, @Param("deckId") Long deckId);

    List<CardProgress> findByUserIdAndCardDeckId(Long userId, Long deckId);
}