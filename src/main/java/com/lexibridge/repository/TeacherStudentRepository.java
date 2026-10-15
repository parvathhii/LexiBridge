package com.lexibridge.repository;

import com.lexibridge.entity.TeacherStudent;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface TeacherStudentRepository extends JpaRepository<TeacherStudent, Long> {
    @EntityGraph(attributePaths = "student")
    List<TeacherStudent> findByTeacher(User teacher);
    List<TeacherStudent> findByTeacherId(Long teacherId);
    List<TeacherStudent> findByStudent(User student);
    boolean existsByTeacherAndStudent(User teacher, User student);
}
