package ru.aav.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import ru.aav.account.*;

/** Сессионная авторизация: открытый каталог, личные корзины/заказы и CSRF-защита всех POST. */
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }
    @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmail(AccountService.normalizeEmail(email))
                .map(a -> User.withUsername(a.getEmail()).password(a.getPasswordHash()).roles("CUSTOMER").build())
                .orElseThrow(() -> new UsernameNotFoundException("Неверные данные для входа"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/products", "/styles.css", "/account.css", "/account.js", "/images/**", "/login", "/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/register").anonymous()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").usernameParameter("email")
                    .defaultSuccessUrl("/account", false).failureUrl("/login?error").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        // CSRF и защита от фиксации сессии остаются включены по умолчанию.
        return http.build();
    }
}
