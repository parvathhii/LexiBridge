package com.lexibridge.service;

import com.lexibridge.entity.*;
import com.lexibridge.repository.ParentStudentRepository;
import com.lexibridge.repository.ParentTeacherFeedbackRepository;
import com.lexibridge.repository.TeacherStudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ParentTeacherFeedbackService {
    private final ParentTeacherFeedbackRepository feedbackRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    public ParentTeacherFeedbackService(ParentTeacherFeedbackRepository feedbackRepository,
                                        ParentStudentRepository parentStudentRepository,
                                        TeacherStudentRepository teacherStudentRepository) {
        this.feedbackRepository = feedbackRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.teacherStudentRepository = teacherStudentRepository;
    }

    @Transactional
    public List<User> linkedStudents(User user) {
        if (user.getRole() == Role.ROLE_PARENT) {
            return parentStudentRepository.findByParent(user).stream().map(ParentStudent::getStudent).toList();
        }
        if (user.getRole() == Role.ROLE_TEACHER) {
            return teacherStudentRepository.findByTeacher(user).stream().map(TeacherStudent::getStudent).toList();
        }
        return List.of();
    }

    @Transactional
    public List<ParentTeacherFeedback> historyAndMarkRead(User user) {
        List<ParentTeacherFeedback> messages = feedbackRepository.findBySenderOrRecipientOrderByCreatedAtDesc(user, user)
                .stream()
                .filter(message -> isLinkedToStudent(user, message.getStudent())
                        && isLinkedToStudent(message.getSender(), message.getStudent())
                        && isLinkedToStudent(message.getRecipient(), message.getStudent()))
                .toList();
        boolean changed = false;
        for (ParentTeacherFeedback message : messages) {
            if (message.getRecipient().getId().equals(user.getId()) && !message.isRead()) {
                message.setRead(true);
                changed = true;
            }
        }
        if (changed) feedbackRepository.saveAll(messages);
        return messages;
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return feedbackRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public int submit(User sender, User student, String category, String text) {
        if (sender.getRole() != Role.ROLE_PARENT && sender.getRole() != Role.ROLE_TEACHER) {
            throw new IllegalArgumentException("Only parents and teachers can send feedback.");
        }
        boolean linked = sender.getRole() == Role.ROLE_PARENT
                ? parentStudentRepository.existsByParentAndStudent(sender, student)
                : teacherStudentRepository.existsByTeacherAndStudent(sender, student);
        if (!linked) throw new IllegalArgumentException("Choose a student linked to your account.");

        List<User> recipients = sender.getRole() == Role.ROLE_PARENT
                ? teacherStudentRepository.findByStudent(student).stream().map(TeacherStudent::getTeacher).toList()
                : parentStudentRepository.findByStudent(student).stream().map(ParentStudent::getParent).toList();
        Map<Long, User> distinctRecipients = new LinkedHashMap<>();
        for (User recipient : recipients) distinctRecipients.put(recipient.getId(), recipient);
        if (distinctRecipients.isEmpty()) {
            throw new IllegalArgumentException("There is no linked parent or teacher available for this student yet.");
        }

        for (User recipient : distinctRecipients.values()) {
            ParentTeacherFeedback feedback = new ParentTeacherFeedback();
            feedback.setSender(sender);
            feedback.setRecipient(recipient);
            feedback.setStudent(student);
            feedback.setCategory(category);
            feedback.setFeedbackMessage(text);
            feedback.setRead(false);
            feedbackRepository.save(feedback);
        }
        return distinctRecipients.size();
    }

    @Transactional
    public void reply(User user, Long feedbackId, String replyText) {
        ParentTeacherFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback message not found."));
        if (!feedback.getRecipient().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the person who received this message can reply.");
        }
        if (!isLinkedToStudent(user, feedback.getStudent())
                || !isLinkedToStudent(feedback.getSender(), feedback.getStudent())) {
            throw new IllegalArgumentException("This feedback is no longer connected to your linked student.");
        }
        if (feedback.getReply() != null && !feedback.getReply().isBlank()) {
            throw new IllegalArgumentException("A reply has already been added to this message.");
        }
        feedback.setReply(replyText);
        feedback.setRead(true);
        feedbackRepository.save(feedback);
    }

    private boolean isLinkedToStudent(User user, User student) {
        if (user.getRole() == Role.ROLE_PARENT) {
            return parentStudentRepository.existsByParentAndStudent(user, student);
        }
        if (user.getRole() == Role.ROLE_TEACHER) {
            return teacherStudentRepository.existsByTeacherAndStudent(user, student);
        }
        return false;
    }
}
