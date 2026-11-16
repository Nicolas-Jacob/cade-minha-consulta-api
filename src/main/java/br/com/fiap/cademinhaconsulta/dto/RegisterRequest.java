package br.com.fiap.cademinhaconsulta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
        String nome,

        @NotBlank(message = "Sobrenome e obrigatorio")
        @Size(max = 100, message = "Sobrenome deve ter no maximo 100 caracteres")
        String sobrenome,

        @NotBlank(message = "CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 digitos")
        String cpf,

        @NotBlank(message = "Sexo e obrigatorio")
        @Pattern(regexp = "[FM]", message = "Sexo deve ser F ou M")
        String sexo,

        @NotNull(message = "Data de nascimento e obrigatoria")
        @Past(message = "Data de nascimento deve estar no passado")
        LocalDate dataNascimento,

        @NotBlank(message = "Nome da mae e obrigatorio")
        @Size(max = 100, message = "Nome da mae deve ter no maximo 100 caracteres")
        String nomeMae,

        @Size(max = 100, message = "Nome do pai deve ter no maximo 100 caracteres")
        String nomePai,

        @NotBlank(message = "Login e obrigatorio")
        @Size(max = 20, message = "Login deve ter no maximo 20 caracteres")
        String login,

        // BCrypt aceita no maximo 72 bytes
        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres")
        String senha,

        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 digitos")
        String telefone,

        @Email(message = "Email invalido")
        @Size(max = 100, message = "Email deve ter no maximo 100 caracteres")
        String email,

        @NotNull(message = "Endereco e obrigatorio")
        @Valid
        EnderecoDTO endereco
) {
}
