package com.fcuro.userreg.web;

import com.fcuro.userreg.security.SheetUserPrincipal;
import com.fcuro.userreg.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserListController {

    private final UserService userService;

    public UserListController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String list(@AuthenticationPrincipal SheetUserPrincipal principal, Model model) {
        model.addAttribute("users", userService.listUsers(principal.getSpreadsheetId(), principal.getSheetName()));
        return "users";
    }

    @PostMapping("/users/{allowId}/retired-flag")
    public String updateRetiredFlag(@AuthenticationPrincipal SheetUserPrincipal principal,
                                     @PathVariable String allowId,
                                     @RequestParam boolean retired) {
        userService.updateRetiredFlag(principal.getSpreadsheetId(), principal.getSheetName(), allowId, retired);
        return "redirect:/users";
    }

    @PostMapping("/users/{allowId}/admin-flag")
    public String updateAdminFlag(@AuthenticationPrincipal SheetUserPrincipal principal,
                                   @PathVariable String allowId,
                                   @RequestParam boolean admin) {
        userService.updateAdminFlag(principal.getSpreadsheetId(), principal.getSheetName(), allowId, admin);
        return "redirect:/users";
    }
}
