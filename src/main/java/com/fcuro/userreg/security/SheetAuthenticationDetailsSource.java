package com.fcuro.userreg.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
public class SheetAuthenticationDetailsSource
        implements AuthenticationDetailsSource<HttpServletRequest, SheetAuthenticationDetails> {

    @Override
    public SheetAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new SheetAuthenticationDetails(context);
    }
}
