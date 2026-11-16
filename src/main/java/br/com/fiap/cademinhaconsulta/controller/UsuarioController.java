package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.UsuarioResponse;
import br.com.fiap.cademinhaconsulta.dto.UsuarioUpdateRequest;
import br.com.fiap.cademinhaconsulta.service.TokenService;
import br.com.fiap.cademinhaconsulta.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Dados do usuario logado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Dados do usuario do token")
    public UsuarioResponse buscarMe(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.buscar(TokenService.idUsuario(jwt));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza nome, contato e endereco do usuario do token")
    public UsuarioResponse atualizarMe(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody @Valid UsuarioUpdateRequest request) {
        return usuarioService.atualizar(TokenService.idUsuario(jwt), request);
    }
}
