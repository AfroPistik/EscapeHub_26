package com.team.config;

import com.team.common.error.ErrorCode;
import com.team.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())

                // ΑΠΟΦΑΣΗ CSRF: απενεργοποιημένο προς το παρόν (Sprint 1).
// Γιατί: το API καταναλώνεται από ξεχωριστό frontend και το project είναι ακόμα σε φάση demo.
// Κίνδυνος: η ταυτοποίηση γίνεται με session cookie (JSESSIONID), άρα ένα ξένο site θα
// μπορούσε να στείλει αίτημα εκ μέρους συνδεδεμένου χρήστη.
// Επόμενο βήμα πριν από πραγματική χρήση: ρύθμιση SameSite στο session cookie
// ή ενεργοποίηση CSRF token. Να επανεξεταστεί από την ομάδα.

                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 401 στην κοινή μορφή σφάλματος (ErrorResponse). Το GlobalExceptionHandler δεν φτάνει
    // εδώ, γιατί το σφάλμα προκύπτει μέσα στα φίλτρα ασφαλείας.
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            ErrorResponse body = ErrorResponse.of(ErrorCode.UNAUTHORIZED, "Authentication required");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format(
                    "{\"code\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    body.code(), body.message(), body.timestamp()));
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // TODO: επιβεβαίωσε το origin του frontend
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // χρειάζεται για το cookie JSESSIONID
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}