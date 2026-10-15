package com.lexibridge.repository;

import com.lexibridge.entity.ParentTeacherFeedback;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentTeacherFeedbackRepository extends JpaRepository<ParentTeacherFeedback, Long> {
    @EntityGraph(attributePaths = {"sender", "recipient", "student"})
    List<ParentTeacherFeedback> findBySenderOrRecipientOrderByCreatedAtDesc(User sender, User recipient);

    long countByRecipientAndReadFalse(User recipient);
}
