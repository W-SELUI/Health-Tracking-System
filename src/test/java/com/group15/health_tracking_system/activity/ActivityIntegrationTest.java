package com.group15.health_tracking_system.activity;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import java.time.LocalDate;import java.math.BigDecimal;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:activitytest;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@org.springframework.transaction.annotation.Transactional
@AutoConfigureMockMvc @WithMockUser(username="alice",roles="PATIENT")
class ActivityIntegrationTest {
 @Autowired ActivityService service;@Autowired ActivityLogRepository logs;@Autowired com.group15.health_tracking_system.user.repository.UserRepository users;@Autowired MockMvc mvc;
 Long alice,bob; LocalDate day=LocalDate.now().minusDays(2);
 @BeforeEach void setup(){logs.deleteAll();users.deleteAll();alice=users.save(new com.group15.health_tracking_system.user.entity.User("Alice","Patient","alice","unused-test-hash",com.group15.health_tracking_system.user.entity.Role.PATIENT)).getId();bob=users.save(new com.group15.health_tracking_system.user.entity.User("Bob","Patient","bob","unused-test-hash",com.group15.health_tracking_system.user.entity.Role.PATIENT)).getId();}
 ActivityForm form(int steps,String kcal){ActivityForm f=new ActivityForm();f.setActivityDate(day);f.setSteps(steps);f.setExerciseType(ExerciseType.WALKING);f.setDurationMinutes(30);f.setCaloriesBurned(new BigDecimal(kcal));return f;}
 @Test void createEditDeletePersist(){var a=service.create(form(1000,"50.25"));assertThat(logs.findById(a.getId()).orElseThrow().getPatientId()).isEqualTo(alice);service.edit(a.getId(),form(2000,"75.00"));assertThat(logs.findById(a.getId()).orElseThrow().getSteps()).isEqualTo(2000);service.delete(a.getId());assertThat(logs.findById(a.getId())).isEmpty();}
 @Test void negativeAndRequiredInputsRejected(){var f=form(-1,"10");assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);f.setSteps(1);f.setCaloriesBurned(new BigDecimal("-0.01"));assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);f.setCaloriesBurned(BigDecimal.ZERO);f.setDurationMinutes(-1);assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);f.setDurationMinutes(null);f.setActivityDate(null);assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);assertThat(logs.count()).isZero();}
 @Test void futureDateAndOtherNameRejected(){var f=form(0,"0");f.setActivityDate(LocalDate.now().plusDays(1));assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);f.setActivityDate(day);f.setExerciseType(ExerciseType.OTHER);assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);}
 @Test void zeroValuesAndOptionalDurationAccepted(){var f=form(0,"0");f.setDurationMinutes(null);assertThat(service.create(f).getDurationMinutes()).isNull();}
 @Test void ownershipBlocksReadEditDelete(){var other=logs.save(new ActivityLog(users.findById(bob).orElseThrow(),form(9999,"999")));assertThatThrownBy(()->service.get(other.getId())).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->service.edit(other.getId(),form(1,"1"))).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->service.delete(other.getId())).isInstanceOf(ResponseStatusException.class);assertThat(logs.findById(other.getId())).isPresent();}
 @Test void inclusiveRangeAndKnownAnalytics(){service.create(form(1000,"100.25"));var f=form(2000,"200.50");f.setActivityDate(day.plusDays(1));f.setDurationMinutes(null);service.create(f);var excluded=form(9999,"999");excluded.setActivityDate(day.minusDays(1));service.create(excluded);logs.save(new ActivityLog(users.findById(bob).orElseThrow(),form(9999,"999")));var summary=service.summary(day,day.plusDays(2));assertThat(summary.logCount()).isEqualTo(2);assertThat(summary.totalSteps()).isEqualTo(3000);assertThat(summary.totalDurationMinutes()).isEqualTo(30);assertThat(summary.totalCaloriesBurned()).isEqualByComparingTo("300.75");assertThat(summary.daysWithEntries()).isEqualTo(2);assertThat(summary.calendarDays()).isEqualTo(3);assertThat(summary.averageStepsPerCalendarDay()).isEqualByComparingTo("1000.00");assertThat(service.history(day,day)).hasSize(1);}
 @Test void noDataAndInvalidRange(){assertThat(service.summary(day,day).totalSteps()).isZero();assertThat(service.summary(day,day).averageStepsPerCalendarDay()).isEqualByComparingTo("0.00");assertThatThrownBy(()->service.summary(day.plusDays(1),day)).isInstanceOf(ResponseStatusException.class);}
 @Test void templatesRenderWithCsrfAndEscapeNotes()throws Exception{var f=form(10,"1");f.setNotes("<script>alert(1)</script>");service.create(f);mvc.perform(get("/activities")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;"))).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));mvc.perform(get("/activities/new")).andExpect(status().isOk());}
 @Test void csrfRequiredAndSpoofedPatientIgnored()throws Exception{mvc.perform(post("/activities")).andExpect(status().isForbidden());mvc.perform(post("/activities").with(csrf()).param("patientId",bob.toString()).param("activityDate",day.toString()).param("steps","10").param("exerciseType","WALKING").param("durationMinutes","5").param("caloriesBurned","2.50")).andExpect(status().is3xxRedirection());assertThat(logs.findAll()).allMatch(a->a.getPatientId().equals(alice));}
 @Test @WithMockUser(username="provider",roles="PROVIDER") void providerDenied()throws Exception{mvc.perform(get("/activities")).andExpect(status().isForbidden());}
 @Test void signedOutRedirected()throws Exception{mvc.perform(get("/activities").with(anonymous())).andExpect(status().is3xxRedirection());}
 @Test void crossPatientHttpRequestsReturnNotFound() throws Exception {
  var other=logs.save(new ActivityLog(users.findById(bob).orElseThrow(),form(100,"10")));
  mvc.perform(get("/activities/"+other.getId()+"/edit")).andExpect(status().isNotFound());
  mvc.perform(post("/activities/"+other.getId()+"/delete").with(csrf())).andExpect(status().isNotFound());
  assertThat(logs.findById(other.getId())).isPresent();
 }
 @Test void dashboardLinksToActivities() throws Exception {
  mvc.perform(get("/patient/dashboard")).andExpect(status().isOk())
   .andExpect(content().string(org.hamcrest.Matchers.containsString("/activities")));
 }
 @Test @WithMockUser(username="admin",roles="ADMIN") void adminDenied() throws Exception {
  mvc.perform(get("/activities")).andExpect(status().isForbidden());
 }

}
