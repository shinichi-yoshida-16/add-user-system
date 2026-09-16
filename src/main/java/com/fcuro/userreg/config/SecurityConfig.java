package com.fcuro.userreg.config;

import com.fcuro.userreg.security.SheetAuthenticationDetailsSource;
import com.fcuro.userreg.security.SheetAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SheetAuthenticationProvider sheetAuthenticationProvider;
    private final SheetAuthenticationDetailsSource sheetAuthenticationDetailsSource;

    public SecurityConfig(SheetAuthenticationProvider sheetAuthenticationProvider,
                           SheetAuthenticationDetailsSource sheetAuthenticationDetailsSource) {
        this.sheetAuthenticationProvider = sheetAuthenticationProvider;
        this.sheetAuthenticationDetailsSource = sheetAuthenticationDetailsSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(sheetAuthenticationProvider)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/api/users/register").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .authenticationDetailsSource(sheetAuthenticationDetailsSource)
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
