package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.enums.Credencial;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long expiracaoSegundos;

    public TokenService(JwtEncoder jwtEncoder, @Value("${jwt.expiration-seconds}") long expiracaoSegundos) {
        this.jwtEncoder = jwtEncoder;
        this.expiracaoSegundos = expiracaoSegundos;
    }

    // Claims do contrato: sub = login, id = id_usuario, credencial = CIDADAO ou GESTOR
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cade-minha-consulta-api")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracaoSegundos))
                .subject(usuario.getLogin())
                .claim("id", usuario.getId())
                .claim("credencial", usuario.getCredencial().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // Claim "id" do token recebido (id_usuario)
    public static Long idUsuario(Jwt jwt) {
        Number id = jwt.getClaim("id");
        return id.longValue();
    }

    // Claim "credencial" do token recebido
    public static boolean ehGestor(Jwt jwt) {
        return Credencial.GESTOR.name().equals(jwt.getClaimAsString("credencial"));
    }
}
