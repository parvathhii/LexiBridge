package com.lexibridge.repository;

import com.lexibridge.entity.ParentStudent;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {
    @EntityGraph(attributePaths = "student")
    List<ParentStudent> findByParent(User parent);
    List<ParentStudent> findByParentId(Long parentId);
    List<ParentStudent> findByStudent(User student);
    boolean existsByParentAndStudent(User parent, User student);
    boolean existsByParent(User parent);
    boolean existsByStudent(User student);
}
