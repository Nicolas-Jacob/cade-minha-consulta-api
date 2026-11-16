package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.AgendamentoRequest;
import br.com.fiap.cademinhaconsulta.dto.ConsultaResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.dto.PresencaRequest;
import br.com.fiap.cademinhaconsulta.service.AgendamentoService;
import br.com.fiap.cademinhaconsulta.service.ConsultaService;
import br.com.fiap.cademinhaconsulta.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gestor")
@RequiredArgsConstructor
@Tag(name = "Gestor", description = "Agendamento pela fila, proximas consultas e registro de presenca (GESTOR)")
public class GestorController {

    private final AgendamentoService agendamentoService;
    private final ConsultaService consultaService;

    // A procedure faz COMMIT proprio; a consulta criada e lida depois, numa transacao separada
    @PostMapping("/agendamentos")
    @Operation(summary = "Agenda o proximo paciente da fila do medico (PRC_AGENDAR_CONSULTA). 201 com a consulta criada")
    public ResponseEntity<ConsultaResponse> agendar(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody @Valid AgendamentoRequest request) {
        Long idConsulta = agendamentoService.agendarProximo(request.idMedico());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultaService.buscar(idConsulta, TokenService.idUsuario(jwt), true));
    }

    @GetMapping("/consultas/proximas")
    @Operation(summary = "Consultas AGENDADA/CONFIRMADA nos proximos dias, com o paciente (paginado, padrao: dataHora,asc)")
    public PaginaResponse<ConsultaResponse> listarProximas(
            @RequestParam(defaultValue = "15") @Min(1) @Max(365) int dias,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return consultaService.listarProximas(dias, pageable);
    }

    @PatchMapping("/consultas/{id}/presenca")
    @Operation(summary = "Registra PRESENTE ou FALTA (so AGENDADA/CONFIRMADA com data passada, senao 409)")
    public ConsultaResponse registrarPresenca(@PathVariable Long id, @RequestBody @Valid PresencaRequest request) {
        return consultaService.registrarPresenca(id, request);
    }
}
