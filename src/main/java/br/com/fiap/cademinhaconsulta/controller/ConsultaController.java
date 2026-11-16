package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.ConsultaResponse;
import br.com.fiap.cademinhaconsulta.dto.OrientacaoResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.service.ConsultaService;
import br.com.fiap.cademinhaconsulta.service.OrientacaoService;
import br.com.fiap.cademinhaconsulta.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas", description = "Consultas do cidadao: listar, confirmar, liberar a vaga e orientacoes da IA")
public class ConsultaController {

    private final ConsultaService consultaService;
    private final OrientacaoService orientacaoService;

    @GetMapping("/minhas")
    @Operation(summary = "Minhas consultas (CIDADAO; paginado). futuras: padrao dataHora,asc. historico: padrao dataHora,desc")
    public PaginaResponse<ConsultaResponse> listarMinhas(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "futuras") @Pattern(regexp = "futuras|historico") String periodo,
            // Sem sort aqui: a ordem padrao depende do periodo (ver ConsultaService)
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return consultaService.listarMinhas(TokenService.idUsuario(jwt), "futuras".equals(periodo), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma consulta (dono ou GESTOR; o GESTOR recebe tambem o paciente)")
    public ConsultaResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return consultaService.buscar(id, TokenService.idUsuario(jwt), TokenService.ehGestor(jwt));
    }

    @GetMapping("/{id}/orientacoes")
    @Operation(summary = "Orientacoes da consulta escritas pela IA (dono ou GESTOR). "
            + "Se a IA estiver indisponivel, devolve um texto padrao com origem PADRAO")
    public OrientacaoResponse orientacoes(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orientacaoService.gerar(id, TokenService.idUsuario(jwt), TokenService.ehGestor(jwt));
    }

    @PatchMapping("/{id}/confirmar")
    @Operation(summary = "Confirma presenca: AGENDADA -> CONFIRMADA (dono; so consulta futura, senao 409)")
    public ConsultaResponse confirmar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return consultaService.confirmar(id, TokenService.idUsuario(jwt));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Libera a vaga: CANCELADA e publica vaga.liberada (dono; AGENDADA/CONFIRMADA futura, senao 409)")
    public ConsultaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return consultaService.cancelar(id, TokenService.idUsuario(jwt));
    }
}
