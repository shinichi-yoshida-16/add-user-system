package com.fcuro.userreg.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        boolean disabled = false;
        boolean serviceError = false;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            disabled = exception instanceof DisabledException;
            serviceError = exception instanceof AuthenticationServiceException;
        }
        model.addAttribute("disabled", disabled);
        model.addAttribute("serviceError", serviceError);
        return "login";
    }
}
