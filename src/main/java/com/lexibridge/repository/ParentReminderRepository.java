package com.lexibridge.repository;

import com.lexibridge.entity.ParentReminder;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface ParentReminderRepository extends JpaRepository<ParentReminder, Long> {
    List<ParentReminder> findByParentAndStudentOrderByDueDateAsc(User parent, User student);
    @EntityGraph(attributePaths = "student")
    Optional<ParentReminder> findByIdAndParent(Long id, User parent);
}
