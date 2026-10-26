package com.resqhub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomAuthenticationProvider authenticationProvider;

    public SecurityConfig(CustomAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.authenticationProvider(authenticationProvider);
        return authBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:;")
                )
                .frameOptions(frame -> frame.deny())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/assets/**",
                    "/favicon.ico",
                    "/login",
                    "/signup",
                    "/volunteer-registration",
                    "/forgot-password",
                    "/donate",
                    "/setup",
                    "/error"
                ).permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/app/users/**").hasRole("ADMIN")
                .requestMatchers("/app/disasters/**", "/app/victims/**", "/app/requests/**", "/app/teams/**", "/app/volunteers/**")
                    .hasAnyRole("ADMIN", "RESCUE_OFFICER")
                .requestMatchers("/app/shelters/**", "/app/smart-allocation/**", "/app/resources/**", "/app/food/**", "/app/donations/**")
                    .hasAnyRole("ADMIN", "CAMP_MANAGER")
                .requestMatchers("/app/reports/**")
                    .hasAnyRole("ADMIN", "RESCUE_OFFICER", "CAMP_MANAGER")
                .requestMatchers("/app/my-tasks/**")
                    .hasRole("VOLUNTEER")
                .requestMatchers("/app/report-emergency/**")
                    .hasRole("CITIZEN")
                .requestMatchers("/app/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/app/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "RESQHUBSESSION")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
            );

        return http.build();
    }
}
