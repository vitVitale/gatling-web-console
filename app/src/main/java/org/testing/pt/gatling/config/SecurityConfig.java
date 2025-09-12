package org.testing.pt.gatling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Unified security configuration for the Gatling Web Console application.
 * Provides dual authentication:
 * - HTTP Basic Authentication for API endpoints (/api/*)
 * - Session-based authentication for web interface
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Value("${api.security.username:admin}")
    private String username;
    
    @Value("${api.security.password:password}")
    private String password;
    
    /**
     * Security filter chain for API endpoints.
     * Uses HTTP Basic Authentication for /api/* endpoints.
     * 
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain for API endpoints
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/**").authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }
    
    /**
     * Security filter chain for web interface.
     * Uses session-based authentication with login forms for web pages.
     * 
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain for web interface
     * @throws Exception if an error occurs
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(csrf -> csrf.disable()) // Disabled for simplicity, can be enabled if needed
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        
        return http.build();
    }
    
    /**
     * Creates an in-memory user details manager with a single user.
     * This user can authenticate via both Basic Auth (API) and form login (web).
     * 
     * @return the user details manager
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("USER")
                .build();
        
        return new InMemoryUserDetailsManager(user);
    }
    
    /**
     * Creates a password encoder for encoding passwords.
     * 
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}