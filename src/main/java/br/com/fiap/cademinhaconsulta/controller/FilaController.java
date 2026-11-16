package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.FilaRequest;
import br.com.fiap.cademinhaconsulta.dto.FilaResponse;
import br.com.fiap.cademinhaconsulta.dto.FilaUpdateRequest;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.dto.PrioridadeRequest;
import br.com.fiap.cademinhaconsulta.enums.StatusFila;
import br.com.fiap.cademinhaconsulta.service.FilaService;
import br.com.fiap.cademinhaconsulta.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/filas")
@RequiredArgsConstructor
@Tag(name = "Filas", description = "Solicitacoes de consulta: CRUD do cidadao e gestao pelo GESTOR")
public class FilaController {

    private final FilaService filaService;

    @PostMapping
    @Operation(summary = "Cria uma solicitacao (CIDADAO). 409 se ja houver uma AGUARDANDO para a especialidade")
    public ResponseEntity<FilaResponse> criar(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid FilaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(filaService.criar(TokenService.idUsuario(jwt), request));
    }

    @GetMapping("/minhas")
    @Operation(summary = "Minhas solicitacoes, com posicaoEstimada (CIDADAO; paginado, padrao: dataEntrada,desc)")
    public PaginaResponse<FilaResponse> listarMinhas(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 10, sort = "dataEntrada", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return filaService.listarMinhas(TokenService.idUsuario(jwt), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma solicitacao (dono ou GESTOR)")
    public FilaResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return filaService.buscar(id, TokenService.idUsuario(jwt), TokenService.ehGestor(jwt));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Troca a unidade da solicitacao (dono; so AGUARDANDO)")
    public FilaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @RequestBody @Valid FilaUpdateRequest request) {
        return filaService.atualizar(id, TokenService.idUsuario(jwt), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desiste da solicitacao: status CANCELADA (dono; so AGUARDANDO)")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        filaService.cancelar(id, TokenService.idUsuario(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista as solicitacoes com filtros opcionais (GESTOR; paginado, padrao: prioridade,desc e dataEntrada,asc)")
    public PaginaResponse<FilaResponse> listar(
            @RequestParam(required = false) StatusFila status,
            @RequestParam(required = false) Long especialidade,
            @RequestParam(required = false) Long unidade,
            // Padrao = ordem real da fila: mais prioritario primeiro, depois o mais antigo
            @ParameterObject @PageableDefault(size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "prioridade", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "dataEntrada", direction = Sort.Direction.ASC)})
            Pageable pageable) {
        return filaService.listar(status, especialidade, unidade, pageable);
    }

    @PatchMapping("/{id}/prioridade")
    @Operation(summary = "Altera a prioridade, de 1 a 5 (GESTOR; so AGUARDANDO)")
    public FilaResponse alterarPrioridade(@PathVariable Long id, @RequestBody @Valid PrioridadeRequest request) {
        return filaService.alterarPrioridade(id, request);
    }
}
