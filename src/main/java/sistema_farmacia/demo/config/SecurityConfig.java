package sistema_farmacia.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import sistema_farmacia.demo.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/css/**", "/js/**", "/img/**", "/h2-console/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                // ── Solo ADMIN: configuración crítica y administración ──────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/config", "/api/config/**").hasRole("ADMIN")          // stock mín/deseable
                .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMIN")          // gestión usuarios/roles
                .requestMatchers("/auditoria").hasRole("ADMIN")
                .requestMatchers("/importacion-siciap", "/api/importacion/siciap/**").hasRole("ADMIN") // asociación SICIAP

                // ── Gestión de medicamentos (crear/editar/eliminar): solo ADMIN ────
                .requestMatchers("/medicamentos/guardar",
                                 "/medicamentos/actualizar",
                                 "/medicamentos/eliminar/**").hasRole("ADMIN")

                // ── Operativo: ADMIN + FARMACEUTICO ────────────────────────────────
                .requestMatchers("/importacion", "/api/importacion/preview",
                                 "/api/importacion/confirmar").hasAnyRole("ADMIN", "FARMACEUTICO")

                // Ver catálogo, validar PIN, resto: cualquier usuario autenticado
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout((logout) -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
