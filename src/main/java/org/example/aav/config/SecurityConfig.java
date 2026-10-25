package org.example.aav.config;

import org.example.aav.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  org.springframework.security.core.session.SessionRegistry sessionRegistry() {
    return new org.springframework.security.core.session.SessionRegistryImpl();
  }

  @Bean
  org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
    return new org.springframework.security.web.session.HttpSessionEventPublisher();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return username ->
        users
            .findByUsername(username.toLowerCase(java.util.Locale.ROOT))
            .map(
                u ->
                    User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/account", "/account/**")
                    .authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/products")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/", "/products", "/reviews", "/login", "/register", "/css/**", "/images/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .sessionManagement(session -> session.maximumSessions(-1).sessionRegistry(sessionRegistry()))
        .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/account", true).permitAll())
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; img-src 'self' data:; style-src 'self'; font-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
        .build();
  }
}
