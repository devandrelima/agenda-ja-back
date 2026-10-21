package com.agendaja.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception
    {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository()
    {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception
    {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(
                        new HttpSessionCsrfTokenRepository()
                )
        )
        .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
        )
        .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/csrf",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/estabelecimentos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("GESTOR")
                .anyRequest().authenticated()
        )
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                        (request, response, exception) ->
                                response.sendError(
                                        HttpStatus.UNAUTHORIZED.value()
                                )
                )
                .accessDeniedHandler(
                        (request, response, exception) ->
                                response.sendError(
                                        HttpStatus.FORBIDDEN.value()
                                )
                )
        )
        .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(
                        (request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())
                )
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
        );

        return http.build();
    }
}
