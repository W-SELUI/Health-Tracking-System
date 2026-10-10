package com.group15.health_tracking_system.activity;
import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
@Controller @RequestMapping("/activities")
public class ActivityController {
 private final ActivityService service;
 public ActivityController(ActivityService s){service=s;}
 @ModelAttribute("exerciseTypes") ExerciseType[] types(){return ExerciseType.values();}
 @GetMapping public String history(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate start,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate end,Model model){
  if(end==null)end=LocalDate.now();if(start==null)start=end.minusDays(29);
  model.addAttribute("start",start);model.addAttribute("end",end);model.addAttribute("activities",service.history(start,end));return "activities/history";
 }
 @GetMapping("/new") public String add(Model model){ActivityForm f=new ActivityForm();f.setActivityDate(LocalDate.now());f.setSteps(0);f.setExerciseType(ExerciseType.NONE);f.setCaloriesBurned(java.math.BigDecimal.ZERO);model.addAttribute("form",f);model.addAttribute("action","/activities");return "activities/form";}
 @PostMapping public String create(@Valid @ModelAttribute("form") ActivityForm f,BindingResult errors,Model model){if(errors.hasErrors()){model.addAttribute("action","/activities");return "activities/form";}service.create(f);return "redirect:/activities";}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model model){model.addAttribute("form",service.get(id).toForm());model.addAttribute("action","/activities/"+id);return "activities/form";}
 @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") ActivityForm f,BindingResult errors,Model model){service.get(id);if(errors.hasErrors()){model.addAttribute("action","/activities/"+id);return "activities/form";}service.edit(id,f);return "redirect:/activities";}
 @PostMapping("/{id}/delete") public String delete(@PathVariable Long id){service.delete(id);return "redirect:/activities";}
 @GetMapping("/summary") @ResponseBody public ActivitySummary summary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate start,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate end){return service.summary(start,end);}
}
