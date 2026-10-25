package org.example.aav.config;

import org.example.aav.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Этап 6: настраиваем вход и права доступа, чтобы защитить личные данные и действия администратора. Учебная последовательность. */
@Configuration
public class SecurityConfig {
  /**
   * Создаёт общий реестр сеансов пользователей для управления входами.
   */
  @Bean
  org.springframework.security.core.session.SessionRegistry sessionRegistry() {
    return new org.springframework.security.core.session.SessionRegistryImpl();
  }

  /**
   * Сообщает Spring Security о создании и завершении сеансов браузера.
   */
  @Bean
  org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
    return new org.springframework.security.web.session.HttpSessionEventPublisher();
  }

  /**
   * Создаёт BCrypt для хеширования паролей с параметром сложности 12.
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * Объясняет Spring Security, как найти пользователя и его права в базе.
   * @param users доступ к учётным записям в базе данных
   */
  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    // username — логин, который Spring Security проверяет при входе.
    return username ->
        users
            .findByUsername(username.toLowerCase(java.util.Locale.ROOT))
            .map(
                // u — найденный в базе аккаунт.
                u ->
                    User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
  }

  /**
   * Задаёт права доступа к адресам, вход, выход, сеансы и защитные заголовки.
   * @param http настройки защиты HTTP-запросов
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            // auth — настройки доступа к адресам сайта.
            auth ->
                auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/account", "/account/**")
                    .authenticated()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/products")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/", "/products", "/reviews", "/login", "/register", "/css/**", "/js/**", "/images/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        // session — сеанс входа или настройки сеансов.
        .sessionManagement(session -> session.maximumSessions(-1).sessionRegistry(sessionRegistry()))
        // form — настройки страницы входа.
        .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/account", true).permitAll())
        .logout(
            // logout — настройки выхода из аккаунта.
            logout ->
                logout
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .headers(
            // headers — настройки защитных HTTP-заголовков.
            headers ->
                headers.contentSecurityPolicy(
                    // csp — правила разрешённых источников содержимого страницы.
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; img-src 'self' data:; style-src 'self'; font-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
        .build();
  }
}
