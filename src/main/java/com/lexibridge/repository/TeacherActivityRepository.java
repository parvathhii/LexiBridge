package com.lexibridge.repository;

import com.lexibridge.entity.TeacherActivity;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeacherActivityRepository extends JpaRepository<TeacherActivity, Long> {
    List<TeacherActivity> findByTeacherOrderByCreatedAtDesc(User teacher);
    List<TeacherActivity> findByTeacherAndStudentOrderByCreatedAtDesc(User teacher, User student);
}
