package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusMedico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MedicoRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
        String nome,

        @NotBlank(message = "Sobrenome e obrigatorio")
        @Size(max = 100, message = "Sobrenome deve ter no maximo 100 caracteres")
        String sobrenome,

        @NotBlank(message = "CRM e obrigatorio")
        @Size(max = 10, message = "CRM deve ter no maximo 10 caracteres")
        String crm,

        @NotBlank(message = "UF do CRM e obrigatoria")
        @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "UF do CRM invalida")
        String ufCrm,

        @NotEmpty(message = "Informe ao menos uma especialidade")
        List<@NotNull(message = "Id de especialidade nao pode ser nulo") Long> idsEspecialidades,

        @NotEmpty(message = "Informe ao menos uma unidade")
        List<@NotNull(message = "Id de unidade nao pode ser nulo") Long> idsUnidades,

        // Opcional. POST: padrao ATIVO. PUT: se vier, e aplicado (permite reativar).
        StatusMedico status
) {
}
