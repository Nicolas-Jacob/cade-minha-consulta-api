package br.com.fiap.cademinhaconsulta.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ErroSegurancaHandler erroSegurancaHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Catalogo: leitura para qualquer logado; escrita e medicos so GESTOR
                        .requestMatchers(HttpMethod.GET, "/api/especialidades/**", "/api/unidades/**").authenticated()
                        .requestMatchers("/api/especialidades/**", "/api/unidades/**", "/api/medicos/**").hasRole("GESTOR")
                        // Fila: /minhas antes de /{id}. GET /api/filas/{id} (dono ou GESTOR) e validado no FilaService
                        .requestMatchers(HttpMethod.GET, "/api/filas/minhas").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.POST, "/api/filas").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.PUT, "/api/filas/*").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.DELETE, "/api/filas/*").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.GET, "/api/filas").hasRole("GESTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/filas/*/prioridade").hasRole("GESTOR")
                        // Consultas: GET /api/consultas/{id} (dono ou GESTOR) e validado no ConsultaService
                        .requestMatchers(HttpMethod.GET, "/api/consultas/minhas").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.PATCH, "/api/consultas/*/confirmar", "/api/consultas/*/cancelar")
                        .hasRole("CIDADAO")
                        .requestMatchers("/api/gestor/**").hasRole("GESTOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(erroSegurancaHandler)
                        .accessDeniedHandler(erroSegurancaHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(erroSegurancaHandler)
                        .accessDeniedHandler(erroSegurancaHandler));
        return http.build();
    }

    // Claim "credencial" (CIDADAO ou GESTOR) vira a authority ROLE_CIDADAO ou ROLE_GESTOR
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("credencial");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS liberado para o app mobile e o Swagger
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
