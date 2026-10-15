package com.lexibridge.config;

import com.lexibridge.entity.*;
import com.lexibridge.repository.*;
import com.lexibridge.service.StudentLinkCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final ReadingMaterialRepository readingMaterialRepository;
    private final QuizRepository quizRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final SpeechAttemptRepository speechAttemptRepository;
    private final GameAttemptRepository gameAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ReadingErrorRepository readingErrorRepository;
    private final TeacherActivityRepository teacherActivityRepository;
    private final ParentReminderRepository parentReminderRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentLinkCodeGenerator linkCodeGenerator;

    public DataInitializer(UserRepository userRepository,
                           StudentProfileRepository studentProfileRepository,
                           ParentStudentRepository parentStudentRepository,
                           TeacherStudentRepository teacherStudentRepository,
                           ReadingMaterialRepository readingMaterialRepository,
                           QuizRepository quizRepository,
                           ReadingSessionRepository readingSessionRepository,
                           SpeechAttemptRepository speechAttemptRepository,
                           GameAttemptRepository gameAttemptRepository,
                           QuizAttemptRepository quizAttemptRepository,
                           ReadingErrorRepository readingErrorRepository,
                           TeacherActivityRepository teacherActivityRepository,
                           ParentReminderRepository parentReminderRepository,
                           PasswordEncoder passwordEncoder,
                           StudentLinkCodeGenerator linkCodeGenerator) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.teacherStudentRepository = teacherStudentRepository;
        this.readingMaterialRepository = readingMaterialRepository;
        this.quizRepository = quizRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.speechAttemptRepository = speechAttemptRepository;
        this.gameAttemptRepository = gameAttemptRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.readingErrorRepository = readingErrorRepository;
        this.teacherActivityRepository = teacherActivityRepository;
        this.parentReminderRepository = parentReminderRepository;
        this.passwordEncoder = passwordEncoder;
        this.linkCodeGenerator = linkCodeGenerator;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Checking and seeding default demo accounts...");

        User student = createOrGetUser("student", "student@lexibridge.edu", "Anagha Krishnan", Role.ROLE_STUDENT);
        User parent = createOrGetUser("parent", "parent@lexibridge.edu", "Meera Nair", Role.ROLE_PARENT);
        User teacher = createOrGetUser("teacher", "teacher@lexibridge.edu", "Anu Jose", Role.ROLE_TEACHER);
        User admin = createOrGetUser("admin", "admin@lexibridge.edu", "Portal Administrator", Role.ROLE_ADMIN);
        User aadhya = createOrGetUser("student2", "student2@lexibridge.edu", "Aadhya Menon", Role.ROLE_STUDENT);
        User devika = createOrGetUser("student3", "student3@lexibridge.edu", "Devika Nair", Role.ROLE_STUDENT);
        User ishan = createOrGetUser("student4", "student4@lexibridge.edu", "Ishan Menon", Role.ROLE_STUDENT);

        // Ensure Student Profile
        if (student != null && studentProfileRepository.findByUser(student).isEmpty()) {
            StudentProfile profile = new StudentProfile(student);
            profile.setAge(9);
            profile.setGradeLevel("Grade 3");
            profile.setCurrentReadingLevel(2);
            profile.setFontPreference("OpenDyslexic");
            profile.setFontSize(18);
            profile.setLineSpacing(1.8);
            profile.setThemePreference("theme-offwhite");
            profile.setReadingRulerEnabled(true);
            profile.setTotalReadingMinutes(65);
            profile.setWordsPracticedCount(142);
            studentProfileRepository.save(profile);
            logger.info("Initialized demo StudentProfile for 'student'");
        }
        ensureDemoStudentProfile(aadhya, 9, 1);
        ensureDemoStudentProfile(devika, 9, 2);
        ensureDemoStudentProfile(ishan, 10, 3);

        // Link Parent -> Student
        if (parent != null && student != null && !parentStudentRepository.existsByParentAndStudent(parent, student)) {
            parentStudentRepository.save(new ParentStudent(parent, student));
            logger.info("Linked demo Parent -> Student");
        }

        // Link Teacher -> Student
        if (teacher != null && student != null && !teacherStudentRepository.existsByTeacherAndStudent(teacher, student)) {
            teacherStudentRepository.save(new TeacherStudent(teacher, student, "Grade 3 - Lotus Cohort"));
            logger.info("Linked demo Teacher -> Student in 'Grade 3 - Lotus Cohort'");
        }
        linkDemoStudent(teacher, aadhya);
        linkDemoStudent(teacher, devika);
        linkDemoStudent(teacher, ishan);
        ensureParentLinkCodes();

        // Seed Sample Reading Materials
        seedReadingMaterials(teacher != null ? teacher : admin);
        seedDemoStudentProgress(student, parent, teacher);

        logger.info("Demo accounts, learning materials, and quizzes initialization complete.");
    }

    private void ensureDemoStudentProfile(User student, int age, int readingLevel) {
        if (student == null || studentProfileRepository.findByUser(student).isPresent()) return;
        StudentProfile profile = new StudentProfile(student);
        profile.setAge(age);
        profile.setGradeLevel("Grade 3");
        profile.setCurrentReadingLevel(readingLevel);
        profile.setFontPreference("OpenDyslexic");
        profile.setFontSize(18);
        profile.setLineSpacing(1.8);
        profile.setThemePreference("theme-offwhite");
        profile.setReadingRulerEnabled(true);
        studentProfileRepository.save(profile);
    }

    private void ensureParentLinkCodes() {
        for (User student : userRepository.findByRole(Role.ROLE_STUDENT)) {
            StudentProfile profile = studentProfileRepository.findByUser(student)
                    .orElseGet(() -> new StudentProfile(student));
            if (!parentStudentRepository.existsByStudent(student)
                    && (profile.getParentLinkCode() == null || profile.getParentLinkCode().isBlank())) {
                profile.setParentLinkCode(linkCodeGenerator.createUniqueCode());
                studentProfileRepository.save(profile);
            }
        }
    }

    private void linkDemoStudent(User teacher, User student) {
        if (teacher != null && student != null && !teacherStudentRepository.existsByTeacherAndStudent(teacher, student)) {
            teacherStudentRepository.save(new TeacherStudent(teacher, student, "Grade 3 - Lotus Cohort"));
            logger.info("Linked demo student '{}' to Anu Jose's class", student.getUsername());
        }
    }

    private User createOrGetUser(String username, String email, String fullName, Role role) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Password@123"));
            user.setRole(role);
            user.setEnabled(true);
            logger.info("Created demo user '{}' with role {}", username, role);
        }

        // Keep the built-in demo accounts recognizable after an app restart.
        user.setFullName(fullName);
        return userRepository.save(user);
    }

    private void seedDemoStudentProgress(User student, User parent, User teacher) {
        if (student == null) return;
        StudentProfile profile = studentProfileRepository.findByUser(student)
                .orElseGet(() -> new StudentProfile(student));
        if (Boolean.TRUE.equals(profile.getDemoProgressSeeded())) return;
        List<ReadingMaterial> materials = readingMaterialRepository.findByReadingLevel(1);
        if (materials.isEmpty()) return;

        Quiz quiz = quizRepository.findAll().stream().findFirst().orElseGet(() -> createDemoQuiz(materials.get(0)));
        LocalDate today = LocalDate.now();
        LocalDate firstWeek = today.with(DayOfWeek.MONDAY).minusWeeks(7);
        int[] readingScores = {58, 62, 66, 70, 75, 80, 84, 89};
        int[] speechScores = {60, 64, 69, 73, 78, 83, 88, 94};
        int[] quizScores = {57, 61, 66, 70, 76, 81, 87, 93};
        int[] gamesPerWeek = {1, 2, 2, 3, 3, 4, 4, 5};
        List<ReadingSession> weeklyReads = new ArrayList<>(readingSessionRepository.findByStudentOrderByCreatedAtDesc(student));
        for (int week = 0; week < 8; week++) {
            LocalDate weekStart = firstWeek.plusWeeks(week);
            if (weeklyReads.stream().noneMatch(read -> sameWeek(read.getCreatedAt(), weekStart))) {
                LocalDate day = demoActivityDay(firstWeek, week, today);
                ReadingSession reading = saveDemoReading(student, materials.get(week % materials.size()), day,
                        readingScores[week], Math.max(0, 3 - week / 2), week < 2 ? 1 : 0,
                        150 + week * 15, 42 + week * 4);
                weeklyReads.add(reading);
            }
        }
        for (int ago = 0; ago < 6; ago++) {
            LocalDate day = today.minusDays(ago);
            if (weeklyReads.stream().noneMatch(read -> read.getCreatedAt().toLocalDate().equals(day))) {
                ReadingSession reading = saveDemoReading(student, materials.get((5 - ago) % materials.size()), day,
                        readingScores[7], 0, 0, 120, 72);
                weeklyReads.add(reading);
            }
        }
        int minutes = weeklyReads.stream().mapToInt(read -> read.getDurationSeconds() == null ? 0 : read.getDurationSeconds() / 60).sum();
        int words = weeklyReads.stream().mapToInt(read -> read.getWordsRead() == null ? 0 : read.getWordsRead()).sum();
        profile.setTotalReadingMinutes(minutes);
        profile.setWordsPracticedCount(words);
        studentProfileRepository.save(profile);

        String[] speechWords = {"bright", "puddle", "rabbit", "basket", "quiet", "bridge", "garden", "playful"};
        List<SpeechAttempt> speechAttempts = new ArrayList<>(speechAttemptRepository.findByStudentOrderByCompletedAtDesc(student));
        for (int week = 0; week < 8; week++) {
            LocalDate weekStart = firstWeek.plusWeeks(week);
            if (speechAttempts.stream().noneMatch(attempt -> sameWeek(attempt.getCompletedAt(), weekStart))) {
                String target = speechWords[week];
                String heard = week < 2 ? (week == 0 ? "blight" : "pudle") : target;
                SpeechAttempt attempt = new SpeechAttempt(student, target, heard, speechScores[week], 1 + Math.min(2, week / 3));
                attempt.setCompletedAt(demoActivityDay(firstWeek, week, today).atTime(0, 20));
                speechAttempts.add(speechAttemptRepository.save(attempt));
            }
        }

        String[] gameCodes = {"LETTER_DETECTIVE", "WORD_BUILDER", "SYLLABLE_SWITCH", "FLUENCY_BUILDER", "ERROR_HUNTER", "SENTENCE_SEQUENCE"};
        List<GameAttempt> gameAttempts = new ArrayList<>(gameAttemptRepository.findByStudentOrderByCompletedAtDesc(student));
        for (int week = 0; week < 8; week++) {
            LocalDate weekStart = firstWeek.plusWeeks(week);
            if (gameAttempts.stream().noneMatch(attempt -> sameWeek(attempt.getCompletedAt(), weekStart))) {
                for (int round = 0; round < gamesPerWeek[week]; round++) {
                    int accuracy = Math.min(98, 62 + week * 4 + round);
                    GameAttempt attempt = new GameAttempt(student, gameCodes[(week + round) % gameCodes.length],
                            5 + week * 2 + round, (double) accuracy, week < 3 ? "b/d, p/q" : "", 90 + round * 20);
                    attempt.setDifficultyLevel(1 + Math.min(2, week / 3));
                    attempt.setCompletedAt(demoActivityDay(firstWeek, week, today).atTime(0, 30).plusMinutes(round));
                    gameAttempts.add(gameAttemptRepository.save(attempt));
                }
            }
        }

        int total = Math.max(1, quiz.getQuestions().size());
        List<QuizAttempt> quizAttempts = new ArrayList<>(quizAttemptRepository.findByStudent(student));
        for (int week = 0; week < 8; week++) {
            LocalDate weekStart = firstWeek.plusWeeks(week);
            if (quizAttempts.stream().noneMatch(attempt -> sameWeek(attempt.getCompletedAt(), weekStart))) {
                int correct = Math.max(1, Math.min(total, (int) Math.round(total * quizScores[week] / 100.0)));
                QuizAttempt attempt = new QuizAttempt(student, quiz, correct, total, (double) quizScores[week]);
                attempt.setCompletedAt(demoActivityDay(firstWeek, week, today).atTime(0, 45));
                quizAttempts.add(quizAttemptRepository.save(attempt));
            }
        }

        List<ReadingError> errors = new ArrayList<>(readingErrorRepository.findByStudentOrderByCreatedAtDesc(student));
        if (errors.stream().noneMatch(error -> "LETTER_CONFUSION".equals(error.getErrorType()))) {
            for (int week = 0; week < 8; week++) {
                LocalDate weekStart = firstWeek.plusWeeks(week);
                ReadingSession reading = weeklyReads.stream()
                        .filter(read -> sameWeek(read.getCreatedAt(), weekStart)).findFirst().orElse(null);
                if (reading == null) continue;
                int count = Math.max(1, 4 - week / 2);
                saveDemoError(student, reading, "LETTER_CONFUSION", week % 2 == 0 ? "b/d" : "p/q", count);
            }
        }
        if (errors.stream().noneMatch(error -> "SKIPPED_WORD".equals(error.getErrorType())
                || "REPEATED_WORD".equals(error.getErrorType()))) {
            for (int week = 0; week < 5; week++) {
                LocalDate weekStart = firstWeek.plusWeeks(week);
                ReadingSession reading = weeklyReads.stream()
                        .filter(read -> sameWeek(read.getCreatedAt(), weekStart)).findFirst().orElse(null);
                if (reading == null) continue;
                saveDemoError(student, reading, "SKIPPED_WORD", week % 2 == 0 ? "friend" : "bright", Math.max(1, 3 - week / 2));
                if (week < 3) saveDemoError(student, reading, "REPEATED_WORD", "the", 1);
            }
        }

        if (teacher != null && teacherActivityRepository.findByTeacherAndStudentOrderByCreatedAtDesc(teacher, student).isEmpty()) {
            teacherActivityRepository.save(new TeacherActivity(teacher, student, materials.get(0), quiz,
                    "READING", "Read a short story aloud", "Read for ten calm minutes and notice familiar letter patterns.", today.plusDays(3)));
        }
        if (parent != null && parentReminderRepository.findByParentAndStudentOrderByDueDateAsc(parent, student).isEmpty()) {
            parentReminderRepository.save(new ParentReminder(parent, student, "Read together for 10 minutes",
                    "Choose a familiar story and celebrate careful reading.", today.plusDays(2)));
        }
        profile.setDemoProgressSeeded(true);
        studentProfileRepository.save(profile);
        logger.info("Ensured demo reading progress is available for Anagha Krishnan");
    }

    private ReadingSession saveDemoReading(User student, ReadingMaterial material, LocalDate day, int accuracy,
                                           int skipped, int repeated, int durationSeconds, int wordsPerMinute) {
        ReadingSession session = new ReadingSession(student, material, durationSeconds, material.getWordCount(),
                (double) wordsPerMinute, false, true);
        session.setReadingAccuracy(accuracy);
        session.setSkippedWordCount(skipped);
        session.setRepeatedWordCount(repeated);
        session.setCreatedAt(day.atStartOfDay());
        return readingSessionRepository.save(session);
    }

    private LocalDate demoActivityDay(LocalDate firstWeek, int week, LocalDate today) {
        LocalDate day = firstWeek.plusWeeks(week).plusDays(2);
        return day.isAfter(today) ? today : day;
    }

    private boolean sameWeek(LocalDateTime dateTime, LocalDate weekStart) {
        return dateTime != null && !dateTime.toLocalDate().isBefore(weekStart)
                && dateTime.toLocalDate().isBefore(weekStart.plusWeeks(1));
    }

    private void saveDemoError(User student, ReadingSession session, String type, String pattern, int occurrences) {
        ReadingError error = new ReadingError(student, session, type, pattern, occurrences);
        error.setCreatedAt(session.getCreatedAt().plusMinutes(2));
        readingErrorRepository.save(error);
    }

    private Quiz createDemoQuiz(ReadingMaterial material) {
        Quiz quiz = new Quiz("Reading Quiz: " + material.getTitle(), material, material.getReadingLevel());
        quiz.addQuestion(new QuizQuestion("What is the story mainly about?", "A reading adventure", "A cooking lesson",
                "A trip to space", "A rainy day", "A", "The passage tells a reading adventure."));
        quiz.addQuestion(new QuizQuestion("Which sound should the reader notice?", "b and d", "x and z",
                "m and n", "f and v", "A", "The story practices careful letter recognition."));
        return quizRepository.save(quiz);
    }

    private void seedReadingMaterials(User author) {
        if (readingMaterialRepository.count() > 0) {
            ensureUpperReadingLevels(author);
            return;
        }

        logger.info("Seeding initial dyslexia-friendly reading materials...");

        // Material 1: B vs D Adventure (Level 1)
        ReadingMaterial m1 = new ReadingMaterial(
                "The Brave Little Fox and the Big Drum",
                "A gentle story designed to practice distinguishing the letters 'b' and 'd'.",
                "Barnaby the little brown fox loved to play in the sunny forest.\n\n" +
                "One bright day, Barnaby discovered a big brass drum resting beside an old oak tree.\n\n" +
                "He tapped the drum with a small dry branch. Boom! Boom! The sound echoed down the deep valley.\n\n" +
                "A blue bird flew down from the branch above. 'You play with bold joy, Barnaby!' chirped the bird.\n\n" +
                "Barnaby smiled warmly. Together, the brave fox and the blue bird danced until the golden sun went down behind the blue mountain.",
                "Story",
                1,
                "b vs d distinction",
                3,
                92,
                "🦊",
                author
        );
        ReadingMaterial savedM1 = readingMaterialRepository.save(m1);

        // Seed Quiz for Material 1
        Quiz q1 = new Quiz("Reading Quiz: The Brave Little Fox", savedM1, 1);
        q1.addQuestion(new QuizQuestion(
                "What musical instrument did Barnaby find beside the old oak tree?",
                "A small silver flute",
                "A big brass drum",
                "A wooden xylophone",
                "A golden bell",
                "B",
                "Barnaby discovered a big brass drum resting beside the oak tree."
        ));
        q1.addQuestion(new QuizQuestion(
                "Which feathered forest friend came down to dance with Barnaby?",
                "A blue bird",
                "A wise brown owl",
                "A noisy green parrot",
                "A shy yellow finch",
                "A",
                "A blue bird flew down from the branch above to dance."
        ));
        q1.addQuestion(new QuizQuestion(
                "What sound did the drum make when Barnaby tapped it?",
                "Tick! Tock!",
                "Tap! Tap!",
                "Boom! Boom!",
                "Ding! Dong!",
                "C",
                "Boom! Boom! The sound echoed down the deep valley."
        ));
        quizRepository.save(q1);

        // Material 2: P vs Q Phonics (Level 1)
        ReadingMaterial m2 = new ReadingMaterial(
                "Penny Duck at the Quiet Pond",
                "Practice recognizing 'p' and 'q' while following Penny across the calm waters.",
                "Penny the duck paddled across the quiet pond early in the morning.\n\n" +
                "The water was as clear as glass. Penny saw quick little fish swimming past pink water lilies.\n\n" +
                "Suddenly, Penny heard a gentle quack from the tall green reeds.\n\n" +
                "'Quick, come look!' called Quincy the frog. 'A painted turtle is resting on this smooth stone.'\n\n" +
                "Penny paddled quietly toward her friend. It was quite a pleasant and peaceful morning at the pond.",
                "Story",
                1,
                "p vs q distinction",
                3,
                82,
                "🦆",
                author
        );
        readingMaterialRepository.save(m2);

        // Material 3: Sea Adventure (Level 2)
        ReadingMaterial m3 = new ReadingMaterial(
                "The Whispering Whale of Willow Bay",
                "Follow a gentle humpback whale and practice 'wh' digraphs and long vowel sounds.",
                "Far out in the deep blue sea lived a great whale named Winston.\n\n" +
                "Winston was different from other whales. Instead of loud booming songs, Winston loved to whisper gentle melodies.\n\n" +
                "When stormy winds began to blow across Willow Bay, the small fishing boats looked for safe waters.\n\n" +
                "Winston swam near the harbor, whistling a soft guiding tune through the churning waves.\n\n" +
                "The fishermen followed the white foam trail behind Winston until every boat reached the quiet shore safely.\n\n" +
                "Everyone cheered for the brave white-crested whale who guarded their bay with kindness.",
                "Adventure",
                2,
                "wh digraph & long vowels",
                4,
                105,
                "🐋",
                author
        );
        readingMaterialRepository.save(m3);

        // Material 4: Science Explorer (Level 2)
        ReadingMaterial m4 = new ReadingMaterial(
                "Journey to the Stars: Our Giant Sun",
                "Learn fascinating facts about our nearest star with clear syllable spacing.",
                "Every morning, daylight fills our sky thanks to our nearest star, the Sun.\n\n" +
                "The Sun is a gigantic ball of glowing hot gases located millions of kilometers away from Earth.\n\n" +
                "Even though the Sun looks small in our sky, more than one million Earths could fit inside it!\n\n" +
                "The Sun provides heat and light energy. Without this solar warmth, rivers would freeze into solid ice and plants could never grow.\n\n" +
                "Eight planets travel around the Sun in smooth circular paths called orbits. Earth completes one full orbit around the Sun in 365 days.",
                "Science",
                2,
                "multi-syllable phonics",
                4,
                98,
                "☀️",
                author
        );
        readingMaterialRepository.save(m4);

        // Material 5: Advanced Mystery (Level 3)
        ReadingMaterial m5 = new ReadingMaterial(
                "The Mystery of the Clockwork Owl",
                "An exciting tale with consonant blends (str, spl, scr) and multi-syllable words.",
                "High in the clock tower of Greenfield, young Oliver found an extraordinary discovery.\n\n" +
                "Resting inside a wooden crate was a shiny mechanical owl crafted from polished copper and brass gears.\n\n" +
                "Oliver carefully wound the silver key behind its wing. The gears began to spin with a cheerful ticking sound.\n\n" +
                "The clockwork owl spread its metallic feathers, fluttered smoothly across the workshop, and landed squarely on a dusty map.\n\n" +
                "Under its golden talons lay a hidden compartment containing a handwritten letter from Oliver's grandfather.\n\n" +
                "'To the curious explorer who brings this owl to life: adventure awaits those who look beyond what is ordinary.'",
                "Adventure",
                3,
                "consonant blends & suffixes",
                5,
                116,
                "🦉",
                author
        );
        readingMaterialRepository.save(m5);

        ensureUpperReadingLevels(author);
        logger.info("Successfully seeded reading materials through level 5 and initial comprehension quiz.");
    }

    private void ensureUpperReadingLevels(User author) {
        if (readingMaterialRepository.findByReadingLevel(4).isEmpty()) {
            readingMaterialRepository.save(new ReadingMaterial(
                    "The City Garden Project",
                    "A longer community story with rich vocabulary and clear paragraph breaks.",
                    "Maya noticed that the empty lot beside the library collected litter after every storm. She imagined transforming it into a garden where neighbors could grow food and spend time together.\n\n" +
                    "She brought her idea to the community meeting. At first, a few residents worried that the project would require too much work. Maya listened carefully, then suggested that each family could care for one small section. The librarian offered seeds, and a nearby shop donated tools.\n\n" +
                    "Over the next month, the volunteers cleared the ground, built wooden beds, and planted tomatoes, beans, and bright flowers. Children painted signs while older neighbors shared gardening advice.\n\n" +
                    "By summer, the garden had become a lively gathering place. The families shared fresh vegetables, and people who had barely spoken before now worked side by side. Maya learned that a thoughtful idea can grow when a community gives it care.",
                    "Community", 4, "multisyllable words and paragraph fluency", 6, 150, "Garden", author));
        }
        if (readingMaterialRepository.findByReadingLevel(5).isEmpty()) {
            readingMaterialRepository.save(new ReadingMaterial(
                    "How Bridges Carry a Burden",
                    "An informational passage about engineering, cause and effect, and precise vocabulary.",
                    "A bridge may look still, but it is constantly responding to forces. Its own weight presses downward, while traffic adds another changing load. Wind can push from the side, and temperature can make the materials expand or contract. Engineers study these forces before choosing a design.\n\n" +
                    "A beam bridge transfers weight toward supports at either end. An arch bridge directs much of its load outward and down into strong foundations. A suspension bridge uses tall towers and carefully balanced cables to support a long roadway. Each design solves a different problem, such as crossing a narrow stream or a wide harbor.\n\n" +
                    "Engineers also plan for unexpected conditions. They test models, inspect the ground, and estimate how structures may behave during storms or earthquakes. Once a bridge opens, regular inspections help workers notice small changes before they become serious problems.\n\n" +
                    "Understanding a bridge means looking beyond its shape. Its materials, supports, location, and maintenance all work together. A safe crossing is the result of many careful decisions made before and long after construction.",
                    "Science", 5, "academic vocabulary, inference, and complex sentences", 7, 165, "Bridge", author));
        }
    }
}
