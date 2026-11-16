package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.dto.UnidadeRequest;
import br.com.fiap.cademinhaconsulta.dto.UnidadeResponse;
import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import br.com.fiap.cademinhaconsulta.service.TokenService;
import br.com.fiap.cademinhaconsulta.service.UnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/unidades")
@RequiredArgsConstructor
@Tag(name = "Unidades", description = "Leitura para qualquer usuario logado (CIDADAO so ve ATIVO); escrita so GESTOR")
public class UnidadeController {

    private final UnidadeService unidadeService;

    @GetMapping
    @Operation(summary = "Lista as unidades (paginado, padrao: nome,asc). "
            + "status: so para o GESTOR. especialidade: so unidades com medico ATIVO dessa especialidade")
    public PaginaResponse<UnidadeResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusUnidade status,
            @RequestParam(required = false) Long especialidade,
            @ParameterObject @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return unidadeService.listar(TokenService.ehGestor(jwt), status, especialidade, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma unidade (CIDADAO recebe 404 se ela estiver INATIVO)")
    public UnidadeResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return unidadeService.buscar(id, TokenService.ehGestor(jwt));
    }

    @PostMapping
    @Operation(summary = "Cria uma unidade com contato e endereco (GESTOR)")
    public ResponseEntity<UnidadeResponse> criar(@RequestBody @Valid UnidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadeService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma unidade, seu contato e endereco (GESTOR). status opcional permite reativar")
    public UnidadeResponse atualizar(@PathVariable Long id, @RequestBody @Valid UnidadeRequest request) {
        return unidadeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa uma unidade (GESTOR)")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        unidadeService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
