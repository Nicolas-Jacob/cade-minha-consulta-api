package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusConsulta;

import java.time.LocalDateTime;

// Unicos dados da consulta que podem chegar ao modelo de IA (minimizacao de dados, LGPD).
// De proposito NAO tem CPF, telefone, e-mail, endereco nem sobrenome do paciente.
// O idConsulta so e usado na resposta da API; nao vai para o prompt.
public record DadosOrientacao(
        Long idConsulta,
        String primeiroNome,
        String especialidade,
        String descricaoEspecialidade,
        LocalDateTime dataHora,
        long diasAteConsulta,
        String unidade,
        // Endereco da UNIDADE de saude (formatado) ou null
        String enderecoUnidade,
        StatusConsulta status
) {
}
