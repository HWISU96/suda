package com.ssafy.backend.domain.learn.repository;

import com.ssafy.backend.domain.learn.entity.Learn;
import com.ssafy.backend.domain.learn.entity.LearnDifficulty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnRepository extends JpaRepository<Learn, Long> {

  List<Learn> findByCategory_IdAndDifficultyAndActiveTrueOrderBySortOrderAsc(
      Long categoryId, LearnDifficulty difficulty);
}
