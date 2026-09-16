package com.fcuro.userreg.web;

import com.fcuro.userreg.repository.SheetAccessException;
import com.fcuro.userreg.service.EmailAlreadyExistsException;
import com.fcuro.userreg.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String form(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String submit(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(form.getSpreadsheetId(), form.getSheetName(), form.getEmail(), form.getPassword(),
                    form.getTargetId(), form.isAdmin());
        } catch (EmailAlreadyExistsException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        } catch (SheetAccessException e) {
            bindingResult.rejectValue("spreadsheetId", "sheetAccess",
                    "スプレッドシートにアクセスできませんでした。スプレッドシートID・シート名と共有設定をご確認ください。");
            return "register";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/home";
    }
}
