package br.com.fiap.cademinhaconsulta.controller;

import br.com.fiap.cademinhaconsulta.dto.MedicoRequest;
import br.com.fiap.cademinhaconsulta.dto.MedicoResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.service.MedicoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
@Tag(name = "Medicos", description = "CRUD de medicos (so GESTOR)")
public class MedicoController {

    private final MedicoService medicoService;

    @GetMapping
    @Operation(summary = "Lista os medicos, com filtro opcional por especialidade e unidade (paginado, padrao: nome,asc)")
    public PaginaResponse<MedicoResponse> listar(
            @RequestParam(required = false) Long especialidade,
            @RequestParam(required = false) Long unidade,
            @ParameterObject @PageableDefault(size = 10, sort = {"nome", "sobrenome"}, direction = Sort.Direction.ASC)
            Pageable pageable) {
        return medicoService.listar(especialidade, unidade, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um medico")
    public MedicoResponse buscar(@PathVariable Long id) {
        return medicoService.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria um medico. 409 se ja existir o mesmo CRM na mesma UF")
    public ResponseEntity<MedicoResponse> criar(@RequestBody @Valid MedicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um medico, suas especialidades e unidades. status opcional permite reativar")
    public MedicoResponse atualizar(@PathVariable Long id, @RequestBody @Valid MedicoRequest request) {
        return medicoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa um medico")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        medicoService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
