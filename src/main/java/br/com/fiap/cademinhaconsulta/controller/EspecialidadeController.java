package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.EspecialidadeRequest;
import br.com.fiap.cademinhaconsulta.dto.EspecialidadeResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.service.EspecialidadeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Leitura para qualquer usuario logado; escrita so GESTOR")
public class EspecialidadeController {

    private final EspecialidadeService especialidadeService;

    @GetMapping
    @Operation(summary = "Lista as especialidades (paginado, padrao: nome,asc)")
    public PaginaResponse<EspecialidadeResponse> listar(
            @ParameterObject @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return especialidadeService.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma especialidade")
    public EspecialidadeResponse buscar(@PathVariable Long id) {
        return especialidadeService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma especialidade (GESTOR)")
    public ResponseEntity<EspecialidadeResponse> criar(@RequestBody @Valid EspecialidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadeService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma especialidade (GESTOR)")
    public EspecialidadeResponse atualizar(@PathVariable Long id, @RequestBody @Valid EspecialidadeRequest request) {
        return especialidadeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma especialidade (GESTOR). 409 se estiver em uso por medico ou fila")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        especialidadeService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
