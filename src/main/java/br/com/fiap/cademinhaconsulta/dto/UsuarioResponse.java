package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.Credencial;
import br.com.fiap.cademinhaconsulta.enums.Sexo;

import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        String nome,
        String sobrenome,
        String cpf,
        Sexo sexo,
        LocalDate dataNascimento,
        String login,
        Credencial credencial,
        String telefone,
        String email,
        EnderecoDTO endereco
) {
}
