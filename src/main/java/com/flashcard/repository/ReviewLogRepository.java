package com.flashcard.repository;

import com.flashcard.entity.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {
    List<ReviewLog> findTop3ByUserIdAndCardIdOrderByReviewedAtDesc(Long userId, Long cardId);
}