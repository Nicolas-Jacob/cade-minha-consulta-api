package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
        String nome,

        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 digitos")
        String telefone,

        @Email(message = "Email invalido")
        @Size(max = 100, message = "Email deve ter no maximo 100 caracteres")
        String email,

        @NotNull(message = "Endereco e obrigatorio")
        @Valid
        EnderecoDTO endereco,

        // Opcional. POST: padrao ATIVO. PUT: se vier, e aplicado (permite reativar).
        StatusUnidade status
) {
}
