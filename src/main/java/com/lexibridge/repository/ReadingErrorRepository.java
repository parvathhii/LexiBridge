package com.lexibridge.repository;

import com.lexibridge.entity.ReadingError;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReadingErrorRepository extends JpaRepository<ReadingError, Long> {
    List<ReadingError> findByStudentOrderByCreatedAtDesc(User student);
}
