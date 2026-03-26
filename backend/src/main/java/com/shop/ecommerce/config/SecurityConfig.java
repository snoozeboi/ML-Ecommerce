package com.shop.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

//

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // Every http request passes through chain of security filters before reaching the controllers, this defines the chain of security filters.
        http
                .cors(Customizer.withDefaults()) // Enables cross origin resource sharing, frontend and backend run on different ports, without this the browser would block these requests because of different ports.
                .authorizeHttpRequests(authz -> authz // Authorize http Requests, defines who can access what. right now everything is authorized for everyone. The project runs on our personal PCs therefor for testing we allowed everything for everyone.
                        .requestMatchers("/", "/health", "/h2-console/**", "/admin/**", "/auth/**").permitAll()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable()) // Disable CSRF for H2 console, Cross site request forgery protection, we do not need it during development.
                // .headers(headers -> headers.frameOptions().disable()) // Allow H2 console frames
                .formLogin(form -> form.disable()) // Spring provides its own default login page, we do not need it because we use our own login system.
                .httpBasic(basic -> basic.disable()) // Disable HTTP basic auth popup.
                .logout(logout -> logout.permitAll()); // Allows logout endpoint without authentication issues.

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {  // Defines browser level access rules.
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(allowedOrigins.split("\\s*,\\s*"))); // This allows localhost:3000 to be the only one to access the backend based on @Value("${app.cors.allowed-origins}")
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); // Allowed methods.
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","X-User-Email")); // Frontend allowed to send these headers, important for JSON body, custom headers.
        cfg.setExposedHeaders(List.of("Content-Disposition")); // Used for file downloads
        cfg.setAllowCredentials(true); // Allows Cookies, Authorization headers.
        cfg.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}

// Current flow : Incoming request -> Security filter chain -> Authorization Decisions -> Controller