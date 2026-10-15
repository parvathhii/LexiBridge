package com.lexibridge.controller;

import com.lexibridge.entity.*;
import com.lexibridge.repository.*;
import com.lexibridge.service.ReadingMaterialService;
import com.lexibridge.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/teacher")
public class TeacherController {
    private final UserService userService;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final SpeechAttemptRepository speechAttemptRepository;
    private final ReadingErrorRepository readingErrorRepository;
    private final GameAttemptRepository gameAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ReadingMaterialRepository readingMaterialRepository;
    private final QuizRepository quizRepository;
    private final TeacherActivityRepository teacherActivityRepository;
    private final ReadingMaterialService readingMaterialService;

    public TeacherController(UserService userService, TeacherStudentRepository teacherStudentRepository,
            StudentProfileRepository studentProfileRepository, ReadingSessionRepository readingSessionRepository,
            SpeechAttemptRepository speechAttemptRepository, ReadingErrorRepository readingErrorRepository,
            GameAttemptRepository gameAttemptRepository, QuizAttemptRepository quizAttemptRepository,
            ReadingMaterialRepository readingMaterialRepository, QuizRepository quizRepository,
            TeacherActivityRepository teacherActivityRepository, ReadingMaterialService readingMaterialService) {
        this.userService = userService; this.teacherStudentRepository = teacherStudentRepository;
        this.studentProfileRepository = studentProfileRepository; this.readingSessionRepository = readingSessionRepository;
        this.speechAttemptRepository = speechAttemptRepository; this.readingErrorRepository = readingErrorRepository;
        this.gameAttemptRepository = gameAttemptRepository; this.quizAttemptRepository = quizAttemptRepository;
        this.readingMaterialRepository = readingMaterialRepository; this.quizRepository = quizRepository;
        this.teacherActivityRepository = teacherActivityRepository; this.readingMaterialService = readingMaterialService;
    }

    @GetMapping("/dashboard")
    @Transactional
    public String dashboard(Principal principal, @RequestParam(required = false) Long studentId, Model model) {
        User teacher = currentTeacher(principal);
        if (teacher == null) return "redirect:/login";
        List<TeacherStudent> links = teacherStudentRepository.findByTeacher(teacher);
        List<Map<String, Object>> roster = new ArrayList<>();
        for (TeacherStudent link : links) {
            User student = link.getStudent();
            List<ReadingSession> reads = readingSessionRepository.findByStudent(student);
            List<SpeechAttempt> speech = speechAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
            List<GameAttempt> games = gameAttemptRepository.findByStudent(student);
            List<QuizAttempt> quizzes = quizAttemptRepository.findByStudent(student);
            StudentProfile profile = studentProfileRepository.findByUser(student).orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("student", student); row.put("classroom", link.getClassroomName()); row.put("profile", profile);
            row.put("readingLevel", profile == null ? 1 : profile.getCurrentReadingLevel());
            row.put("minutes", reads.stream().mapToInt(s -> Optional.ofNullable(s.getDurationSeconds()).orElse(0)).sum() / 60);
            row.put("words", reads.stream().mapToInt(s -> Optional.ofNullable(s.getWordsRead()).orElse(0)).sum());
            row.put("readingCount", reads.size()); row.put("speechCount", speech.size());
            row.put("speechAccuracy", averageInt(speech.stream().map(SpeechAttempt::getAccuracy).toList()));
            row.put("gameCount", games.size()); row.put("quizCount", quizzes.size());
            row.put("readingAccuracy", averageInt(reads.stream().map(ReadingSession::getReadingAccuracy).toList()));
            roster.add(row);
        }
        Map<String, Object> selectedRow = roster.stream().filter(r -> studentId != null && ((User) r.get("student")).getId().equals(studentId))
                .findFirst().orElse(roster.isEmpty() ? null : roster.get(0));
        User student = selectedRow == null ? null : (User) selectedRow.get("student");
        model.addAttribute("teacher", teacher); model.addAttribute("students", roster);
        model.addAttribute("totalStudents", roster.size());
        model.addAttribute("assignedClassesCount", links.stream().map(TeacherStudent::getClassroomName).filter(Objects::nonNull).distinct().count());
        model.addAttribute("readingMaterialsCount", readingMaterialRepository.count());
        model.addAttribute("activeQuizzesCount", quizRepository.count());
        model.addAttribute("selected", selectedRow);
        model.addAttribute("materials", readingMaterialRepository.findAll()); model.addAttribute("quizzes", quizRepository.findAll());
        if (model.asMap().containsKey("message")) model.addAttribute("message", model.asMap().get("message"));
        if (student != null) {
            List<ReadingSession> reads = readingSessionRepository.findByStudentOrderByCreatedAtDesc(student);
            List<SpeechAttempt> speech = speechAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
            List<GameAttempt> games = gameAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
            List<QuizAttempt> quizzes = quizAttemptRepository.findByStudent(student);
            List<ReadingError> errors = readingErrorRepository.findByStudentOrderByCreatedAtDesc(student);
            model.addAttribute("selectedStudent", student);
            model.addAttribute("readingSessions", reads.stream().limit(8).toList());
            model.addAttribute("speechAttempts", speech.stream().limit(8).toList());
            model.addAttribute("gameAttempts", games.stream().limit(8).toList()); model.addAttribute("quizAttempts", quizzes.stream().limit(10).toList());
            model.addAttribute("activities", teacherActivityRepository.findByTeacherAndStudentOrderByCreatedAtDesc(teacher, student));
            model.addAttribute("heatmap", errorSummary(errors)); model.addAttribute("difficultWords", difficultWords(errors, speech));
            model.addAttribute("weekly", weeklySummary(reads, speech, games, quizzes));
            model.addAttribute("readingAverage", averageInt(reads.stream().map(ReadingSession::getReadingAccuracy).toList()));
            model.addAttribute("speechAverage", averageInt(speech.stream().map(SpeechAttempt::getAccuracy).toList()));
            model.addAttribute("gameAverage", averageDouble(games.stream().map(GameAttempt::getAccuracy).toList()));
            model.addAttribute("quizAverage", averageDouble(quizzes.stream().map(QuizAttempt::getPercentage).toList()));
        } else {
            model.addAttribute("readingSessions", List.of()); model.addAttribute("speechAttempts", List.of());
            model.addAttribute("gameAttempts", List.of()); model.addAttribute("quizAttempts", List.of());
            model.addAttribute("activities", List.of()); model.addAttribute("heatmap", List.of());
            model.addAttribute("difficultWords", List.of()); model.addAttribute("weekly", List.of());
            model.addAttribute("readingAverage", 0); model.addAttribute("speechAverage", 0);
            model.addAttribute("gameAverage", 0); model.addAttribute("quizAverage", 0);
        }
        return "teacher/dashboard";
    }

    @PostMapping("/students")
    public String linkStudent(Principal principal, @RequestParam String studentAccount,
            RedirectAttributes redirect) {
        User teacher = currentTeacher(principal);
        if (teacher == null) return "redirect:/login";

        String account = studentAccount == null ? "" : studentAccount.trim();
        User student = userService.findByUsername(account)
                .or(() -> userService.findByEmail(account)).orElse(null);
        if (student == null || student.getRole() != Role.ROLE_STUDENT || !student.isEnabled()) {
            redirect.addFlashAttribute("message", "No active student account matched that username or email.");
            return "redirect:/teacher/dashboard";
        }
        if (teacherStudentRepository.existsByTeacherAndStudent(teacher, student)) {
            redirect.addFlashAttribute("message", student.getFullName() + " is already in your student list.");
            return "redirect:/teacher/dashboard?studentId=" + student.getId();
        }

        String classroom = teacherStudentRepository.findByTeacher(teacher).stream()
                .map(TeacherStudent::getClassroomName).filter(Objects::nonNull).findFirst().orElse("My classroom");
        teacherStudentRepository.save(new TeacherStudent(teacher, student, classroom));
        redirect.addFlashAttribute("message", student.getFullName() + " was added to your student list.");
        return "redirect:/teacher/dashboard?studentId=" + student.getId();
    }

    @PostMapping("/materials")
    public String createMaterial(Principal principal, @RequestParam String title, @RequestParam String content,
            @RequestParam(required = false) String description, @RequestParam(defaultValue = "Story") String category,
            @RequestParam(defaultValue = "1") Integer readingLevel, @RequestParam(required = false) String targetPhonemes,
            @RequestParam(defaultValue = "3") Integer minutes, RedirectAttributes redirect) {
        User teacher = currentTeacher(principal); if (teacher == null) return "redirect:/login";
        if (title.isBlank() || content.isBlank() || readingLevel < 1 || readingLevel > 5) {
            redirect.addFlashAttribute("message", "Add a title, reading text, and a level from 1 to 5."); return "redirect:/teacher/dashboard";
        }
        int words = content.trim().split("\\s+").length;
        readingMaterialService.saveMaterial(new ReadingMaterial(title.trim(), description, content, category, readingLevel,
                targetPhonemes, Math.max(1, minutes), words, "📘", teacher));
        redirect.addFlashAttribute("message", "Reading material saved."); return "redirect:/teacher/dashboard";
    }

    @PostMapping("/quizzes")
    public String createQuiz(Principal principal, @RequestParam String title, @RequestParam(defaultValue = "1") Integer readingLevel,
            @RequestParam(required = false) Long materialId, @RequestParam String question, @RequestParam String optionA,
            @RequestParam String optionB, @RequestParam String optionC, @RequestParam String optionD,
            @RequestParam String correctOption, @RequestParam(required = false) String explanation, RedirectAttributes redirect) {
        if (currentTeacher(principal) == null) return "redirect:/login";
        if (title.isBlank() || question.isBlank() || readingLevel < 1 || readingLevel > 5 ||
                !List.of("A", "B", "C", "D").contains(correctOption.toUpperCase(Locale.ROOT))) {
            redirect.addFlashAttribute("message", "Complete the quiz fields and choose a valid correct answer."); return "redirect:/teacher/dashboard";
        }
        ReadingMaterial material = materialId == null ? null : readingMaterialRepository.findById(materialId).orElse(null);
        Quiz quiz = new Quiz(title.trim(), material, readingLevel);
        quiz.addQuestion(new QuizQuestion(question.trim(), optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(),
                correctOption.toUpperCase(Locale.ROOT), explanation));
        quizRepository.save(quiz); redirect.addFlashAttribute("message", "Quiz created and available to students.");
        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/activities")
    public String assignActivity(Principal principal, @RequestParam Long studentId, @RequestParam(defaultValue = "PRACTICE") String activityType,
            @RequestParam String title, @RequestParam(required = false) String instructions, @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) Long materialId, @RequestParam(required = false) Long quizId, RedirectAttributes redirect) {
        User teacher = currentTeacher(principal); if (teacher == null) return "redirect:/login";
        User student = teacherStudentRepository.findByTeacher(teacher).stream().map(TeacherStudent::getStudent)
                .filter(s -> s.getId().equals(studentId)).findFirst().orElse(null);
        if (student == null || title.isBlank()) {
            redirect.addFlashAttribute("message", "Choose one of your assigned students and enter an activity title."); return "redirect:/teacher/dashboard";
        }
        String type = List.of("READING", "QUIZ", "PRACTICE").contains(activityType.toUpperCase(Locale.ROOT)) ? activityType.toUpperCase(Locale.ROOT) : "PRACTICE";
        ReadingMaterial material = materialId == null ? null : readingMaterialRepository.findById(materialId).orElse(null);
        Quiz quiz = quizId == null ? null : quizRepository.findById(quizId).orElse(null);
        LocalDate due = null; try { if (dueDate != null && !dueDate.isBlank()) due = LocalDate.parse(dueDate); } catch (Exception ignored) { }
        teacherActivityRepository.save(new TeacherActivity(teacher, student, material, quiz, type, title.trim(), instructions, due));
        redirect.addFlashAttribute("message", "Activity assigned to " + student.getFullName() + ".");
        return "redirect:/teacher/dashboard?studentId=" + studentId;
    }

    @GetMapping("/reports/{studentId}.csv")
    @Transactional
    public ResponseEntity<byte[]> downloadReport(Principal principal, @PathVariable Long studentId) {
        User teacher = currentTeacher(principal); if (teacher == null) return ResponseEntity.notFound().build();
        User student = teacherStudentRepository.findByTeacher(teacher).stream().map(TeacherStudent::getStudent)
                .filter(s -> s.getId().equals(studentId)).findFirst().orElse(null);
        if (student == null) return ResponseEntity.notFound().build();
        List<ReadingSession> reads = readingSessionRepository.findByStudentOrderByCreatedAtDesc(student);
        List<SpeechAttempt> speech = speechAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
        List<GameAttempt> games = gameAttemptRepository.findByStudentOrderByCompletedAtDesc(student);
        List<QuizAttempt> quizzes = quizAttemptRepository.findByStudent(student);
        StringBuilder csv = new StringBuilder("Student,Activity,Date,Result,Details\r\n");
        for (ReadingSession r : reads) csv.append(csv(student.getFullName())).append(",Reading,").append(r.getCreatedAt()).append(',')
                .append(Optional.ofNullable(r.getReadingAccuracy()).map(v -> v + "%").orElse("Not scored")).append(',').append(csv(r.getMaterial().getTitle())).append("\r\n");
        for (SpeechAttempt a : speech) csv.append(csv(student.getFullName())).append(",Speech,").append(a.getCompletedAt()).append(',').append(a.getAccuracy()).append("%,").append(csv(a.getTargetWord())).append("\r\n");
        for (GameAttempt a : games) csv.append(csv(student.getFullName())).append(",Game,").append(a.getCompletedAt()).append(',').append(a.getAccuracy()).append("%,").append(csv(a.getGameCode())).append(" score ").append(a.getScore()).append("\r\n");
        for (QuizAttempt a : quizzes) csv.append(csv(student.getFullName())).append(",Quiz,").append(a.getCompletedAt()).append(',').append(a.getPercentage()).append("%,").append(csv(a.getQuiz().getTitle())).append("\r\n");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lexibridge-progress-" + studentId + ".csv\"")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private User currentTeacher(Principal p) { return p == null ? null : userService.findByUsername(p.getName()).orElse(null); }
    private int averageInt(List<Integer> values) { return (int)Math.round(values.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0)); }
    private int averageDouble(List<Double> values) { return (int)Math.round(values.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0)); }
    private String csv(String value) { return "\"" + Optional.ofNullable(value).orElse("").replace("\"", "\"\"") + "\""; }
    private List<Map<String, Object>> errorSummary(List<ReadingError> errors) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (ReadingError e : errors) totals.merge(e.getPattern(), Optional.ofNullable(e.getOccurrences()).orElse(1), Integer::sum);
        return totals.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(8)
                .map(e -> Map.<String,Object>of("pattern", e.getKey(), "count", e.getValue())).toList();
    }
    private List<Map<String, Object>> difficultWords(List<ReadingError> errors, List<SpeechAttempt> speech) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        errors.stream().filter(e -> !"LETTER_CONFUSION".equalsIgnoreCase(e.getErrorType())).forEach(e -> totals.merge(e.getPattern(), Optional.ofNullable(e.getOccurrences()).orElse(1), Integer::sum));
        speech.stream().filter(a -> a.getAccuracy() != null && a.getAccuracy() < 80).forEach(a -> totals.merge(a.getTargetWord(), 1, Integer::sum));
        return totals.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(10)
                .map(e -> Map.<String,Object>of("word", e.getKey(), "count", e.getValue())).toList();
    }
    private List<Map<String, Object>> weeklySummary(List<ReadingSession> reads, List<SpeechAttempt> speech, List<GameAttempt> games, List<QuizAttempt> quizzes) {
        LocalDate today = LocalDate.now(); List<Map<String,Object>> weeks = new ArrayList<>();
        for (int offset=7; offset>=0; offset--) {
            LocalDate start=today.minusWeeks(offset).with(java.time.DayOfWeek.MONDAY), end=start.plusDays(7);
            LocalDateTime from=start.atStartOfDay(), to=end.atStartOfDay(); List<Integer> scores=new ArrayList<>();
            reads.stream().filter(x->between(x.getCreatedAt(),from,to)).map(ReadingSession::getReadingAccuracy).filter(Objects::nonNull).forEach(scores::add);
            speech.stream().filter(x->between(x.getCompletedAt(),from,to)).map(SpeechAttempt::getAccuracy).filter(Objects::nonNull).forEach(scores::add);
            games.stream().filter(x->between(x.getCompletedAt(),from,to)).map(GameAttempt::getAccuracy).filter(Objects::nonNull).forEach(x->scores.add((int)Math.round(x)));
            quizzes.stream().filter(x->between(x.getCompletedAt(),from,to)).map(QuizAttempt::getPercentage).filter(Objects::nonNull).forEach(x->scores.add((int)Math.round(x)));
            long count=reads.stream().filter(x->between(x.getCreatedAt(),from,to)).count()+speech.stream().filter(x->between(x.getCompletedAt(),from,to)).count()+games.stream().filter(x->between(x.getCompletedAt(),from,to)).count()+quizzes.stream().filter(x->between(x.getCompletedAt(),from,to)).count();
            weeks.add(Map.of("label",start.getMonth().toString().substring(0,3)+" "+start.getDayOfMonth(),"score",averageInt(scores),"sessions",(int)count));
        }
        return weeks;
    }
    private boolean between(LocalDateTime date, LocalDateTime from, LocalDateTime to) { return date!=null && !date.isBefore(from) && date.isBefore(to); }
}
