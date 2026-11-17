package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.config.JwtConfig;
import br.com.fiap.cademinhaconsulta.config.SecurityConfig;
import br.com.fiap.cademinhaconsulta.enums.Credencial;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Teste unitario do JWT (secao 4 do contrato), sem Spring, sem banco e sem broker:
// usa o JwtConfig, o TokenService e o JwtAuthenticationConverter reais.
class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final long EXPIRACAO_SEGUNDOS = 28800;

    private TokenService tokenService;
    private JwtDecoder jwtDecoder;
    private JwtAuthenticationConverter conversor;

    @BeforeEach
    void preparar() {
        JwtConfig jwtConfig = new JwtConfig(SEGREDO);
        tokenService = new TokenService(jwtConfig.jwtEncoder(), EXPIRACAO_SEGUNDOS);
        jwtDecoder = jwtConfig.jwtDecoder();
        // O handler de erros so e usado na SecurityFilterChain, nao no conversor
        conversor = new SecurityConfig(null).jwtAuthenticationConverter();
    }

    @Test
    void tokenDoGestorTemAsClaimsDoContratoEOPapelGestor() {
        Jwt jwt = jwtDecoder.decode(tokenService.gerarToken(usuario(1L, "gestor", Credencial.GESTOR)));

        assertThat(jwt.getSubject()).isEqualTo("gestor");
        assertThat(TokenService.idUsuario(jwt)).isEqualTo(1L);
        assertThat(jwt.getClaimAsString("credencial")).isEqualTo("GESTOR");
        assertThat(TokenService.ehGestor(jwt)).isTrue();
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(EXPIRACAO_SEGUNDOS));
        assertThat(papeis(jwt)).containsExactly("ROLE_GESTOR");
    }

    @Test
    void tokenDoCidadaoTemOPapelCidadao() {
        Jwt jwt = jwtDecoder.decode(tokenService.gerarToken(usuario(42L, "cidadao", Credencial.CIDADAO)));

        assertThat(TokenService.idUsuario(jwt)).isEqualTo(42L);
        assertThat(TokenService.ehGestor(jwt)).isFalse();
        assertThat(papeis(jwt)).containsExactly("ROLE_CIDADAO");
    }

    @Test
    void tokenAssinadoComOutraChaveERejeitado() {
        JwtConfig outraChave = new JwtConfig("outro-segredo-tambem-com-mais-de-32-caracteres");
        String token = new TokenService(outraChave.jwtEncoder(), EXPIRACAO_SEGUNDOS)
                .gerarToken(usuario(1L, "gestor", Credencial.GESTOR));

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void segredoComMenosDe32CaracteresERejeitado() {
        assertThatThrownBy(() -> new JwtConfig("curto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private Usuario usuario(Long id, String login, Credencial credencial) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setLogin(login);
        usuario.setCredencial(credencial);
        return usuario;
    }

    // So as authorities de papel (ROLE_*). O Spring Security 7 tambem adiciona FACTOR_BEARER (fator de autenticacao).
    private List<String> papeis(Jwt jwt) {
        return conversor.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }
}
