package com.lexibridge.repository;

import com.lexibridge.entity.Quiz;
import com.lexibridge.entity.ReadingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByReadingLevel(Integer readingLevel);
    Optional<Quiz> findByMaterial(ReadingMaterial material);

    @Query("select q from Quiz q left join fetch q.questions left join fetch q.material where q.id = :id")
    Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);
}
