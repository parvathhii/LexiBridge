# LexiBridge – A Dyslexia-Friendly Learning Portal

A full-stack, accessible web application designed to support neurodivergent learners through dyslexia-friendly typography, reading aids, gamified exercises, and cognitive analytics.

---

## 1. Technology Stack
- **Backend:** Java 17, Spring Boot 3.3.4 (MVC, Security, Data JPA, Validation)
- **Frontend:** Thymeleaf, HTML5, CSS3, Vanilla JavaScript
- **Database:** MySQL 8.4 (`lexibridge`)
- **ORM:** Hibernate 6 / Spring Data JPA
- **Build Tool:** Maven 3.9+ (or included Maven Wrapper `mvnw`)
- **Security:** Spring Security 6 with BCrypt password hashing
- **Visual Analytics:** Chart.js

---

## 2. Prerequisites & Installed Tools
This machine has been configured with:
- **Java JDK:** Microsoft Build of OpenJDK 17 (`C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`)
- **Maven:** Apache Maven 3.9.9 (`C:\Users\PARVATHI\tools\apache-maven-3.9.9`)
- **MySQL Server:** MySQL Server 8.4 (`C:\Program Files\MySQL\MySQL Server 8.4`) with data directory initialized at `C:\Users\PARVATHI\tools\mysql-data`.

---

## 3. Database Setup & Configuration

### Default Configuration
By default, the application connects to:
- **Host:** `localhost:3306`
- **Database:** `lexibridge`
- **User:** `root`
- **Password:** *(empty)*

### Environment Variable Overrides
You can customize database credentials without modifying source code by setting:
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/lexibridge?useSSL=false&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

### Manual Schema Initialization
If you want to inspect or execute the database schema manually:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root < schema.sql
```

---

## 4. Running the Application Locally

### Step 1: Start MySQL Server (if not already running)
Double click `start-mysql.bat` or run:
```powershell
.\start-mysql.bat
```

### Step 2: Compile & Run Tests
```powershell
mvn clean test
```
Or using the Maven Wrapper:
```powershell
.\mvnw.cmd test
```

### Step 3: Start Spring Boot
Double click `run-app.bat` or run:
```powershell
mvn spring-boot:run
```

The portal will be live at:
**http://localhost:8080**

### Step 4: Verify Database Health
Navigate in your browser to:
**http://localhost:8080/api/health/db**

Expected JSON output:
```json
{
  "application": "LexiBridge – Dyslexia-Friendly Learning Portal",
  "database": "MySQL",
  "version": "8.4.9",
  "status": "UP",
  "url": "jdbc:mysql://localhost:3306/lexibridge...",
  "user": "root@localhost",
  "userRowCount": 0
}
```

---

## 5. VS Code Configuration
1. Open VS Code in this directory (`c:\Users\PARVATHI\OneDrive\Desktop\Dyslexiaa`).
2. Install the recommended extensions:
   - **Extension Pack for Java** (Microsoft)
   - **Spring Boot Extension Pack** (VMware)
3. Ensure VS Code recognizes Java 17 by setting `java.jdt.ls.java.home` to:
   `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot` (if prompted).
4. Run the main class `com.lexibridge.LexiBridgeApplication` directly from VS Code or through the integrated terminal.

---

## 6. Project Roadmap
- [x] **Phase 1:** Spring Boot foundation and MySQL connection verification.
- [ ] **Phase 2:** Authentication, user roles (Student, Parent, Teacher, Admin), and role-based dashboards.
- [ ] **Phase 3:** Smart Reading Mode, OpenDyslexic fonts, ruler, TTS, and reading materials.
- [ ] **Phase 4:** Educational games (8 games) and quizzes.
- [ ] **Phase 5:** Analytics, Dyslexia heatmap, and adaptive AI recommendations.
- [ ] **Phase 6:** Parent, teacher, and admin portals with report exports.
- [ ] **Phase 7:** End-to-end tests, accessibility validation, and project report.
