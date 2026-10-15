package com.lexibridge.repository;

import com.lexibridge.entity.ReadingMaterial;
import com.lexibridge.entity.ReadingSession;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {
    List<ReadingSession> findByStudent(User student);
    @EntityGraph(attributePaths = "material")
    List<ReadingSession> findByStudentOrderByCreatedAtDesc(User student);
    List<ReadingSession> findByMaterial(ReadingMaterial material);
    long countByStudent(User student);
    @EntityGraph(attributePaths = "material")
    List<ReadingSession> findTop10ByStudentOrderByCreatedAtDesc(User student);
}
