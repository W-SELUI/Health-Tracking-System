# Shaniza activity integration — Java 25 / MySQL 8

## Apply to your feature branch
1. Keep your cloned project at C:\Users\alish\Downloads\health_tracking_system.
2. Extract this ZIP into a separate folder. Copy its `src` folder and this README into your cloned project. Merge folders and replace the two existing files when Windows asks. Keep the clone's Git metadata, pom.xml and Maven wrapper.
3. Run `git branch --show-current`; it must show `feature/shaniza-activity`.
4. Run `mvnw.cmd clean test`. Send the final result before pushing. Existing group tests plus 14 activity tests should be discovered. This integrated build has NOT yet been executed successfully: dependency downloads were blocked in the preparation environment. The earlier 11 passing tests applied to the standalone module.

## Run without mixing practice identities with group identities
Stop the standalone app with Ctrl+C to free port 8080.
Use MySQL Command Line Client to create a separate local integration database:

```sql
CREATE DATABASE health_tracking_system_shaniza CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

In Command Prompt at the cloned project, set:

```bat
set "DB_URL=jdbc:mysql://localhost:3306/health_tracking_system_shaniza?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
set "SPRING_DATASOURCE_URL="
set "DB_USERNAME=root"
set "DB_PASSWORD=REPLACE_WITH_YOUR_MYSQL_PASSWORD"
mvnw.cmd spring-boot:run
```
Replace the password placeholder completely. Do not paste passwords into chat or commit them. The key retrieval setting is for this localhost development setup.
Register at http://localhost:8080/register, choose PATIENT, then log in using that email and registration password. The old standalone `shaniza` login is not used here.
Open My activity records on the patient dashboard. Verify add, edit, date filtering, delete, negative input rejection, and persistence after restarting. Register a second patient to check isolation. Provider and admin accounts must not access /activities.
The group can use its usual database after reviewing the schema; do not reuse the standalone activity_log table as-is. Its patient IDs referenced temporary accounts and its timestamp columns differ. Do not drop or automatically migrate existing data.

## Changes for review
- Added ActivityLog extending shared BaseEntity, with a required User association and patient foreign key, plus optimistic locking.
- Added patient/date-range repository queries, create/read/edit/delete service and forms, inclusive activity summaries, and escaped Thymeleaf pages.
- Derived ownership from CurrentUserService and persisted PATIENT role; browser-supplied patient IDs are ignored.
- Added patient-only /activities security rule and dashboard link.
- Added 14 activity integration tests for CRUD, validation, ownership, CSRF, output escaping, role restrictions, dashboard navigation and summaries.
- Preserved existing login, registration, pom.xml, application.properties, wrapper and group tests.

## Analytics handoff and limits
ActivityService.summary(start,end) returns the current patient's totals and average steps per calendar day, including zero-entry days. Repository queries expose patient/date-range data for the analytics member. Provider analytics must first enforce the consent member's guard; no provider access has been added here. Nutrition, consent, goals and charts are outside this activity change.
The code is support material: review, understand and adapt it in accordance with your course's AI-use rules; disclose assistance as required.

## Git workflow after checks pass
Review `git status` and `git diff`. Only the added activity files, SecurityConfig.java, dashboard/patient.html and this README should be changed.

```bat
git add src README_SHANIZA_ACTIVITY.md
git commit -m "Add patient activity logging"
git push -u origin feature/shaniza-activity
```
Create a GitHub pull request from feature/shaniza-activity to main. Suggested title: Add patient activity logging. Describe CRUD, validation, patient isolation, MySQL persistence and test results (report actual results). Leave merging to your group leader.
