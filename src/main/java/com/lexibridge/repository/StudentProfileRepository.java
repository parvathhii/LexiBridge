package com.lexibridge.repository;

import com.lexibridge.entity.StudentProfile;
import com.lexibridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    Optional<StudentProfile> findByUserId(Long userId);
    boolean existsByParentLinkCodeIgnoreCase(String parentLinkCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from StudentProfile profile join fetch profile.user where upper(profile.parentLinkCode) = upper(:code)")
    Optional<StudentProfile> findByParentLinkCodeForUpdate(@Param("code") String code);
}
