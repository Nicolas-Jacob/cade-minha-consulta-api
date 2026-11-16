package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusConsulta;
import br.com.fiap.cademinhaconsulta.model.Consulta;
import br.com.fiap.cademinhaconsulta.model.Medico;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ConsultaResponse(
        Long id,
        Long idFila,
        StatusConsulta status,
        LocalDateTime dataHora,
        long diasAteConsulta,
        ResumoDTO especialidade,
        MedicoConsulta medico,
        UnidadeConsulta unidade,
        // So para o GESTOR; para o cidadao o campo nem aparece no JSON
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PacienteConsulta paciente
) {

    public record MedicoConsulta(Long id, String nome, String crm) {
    }

    public record UnidadeConsulta(Long id, String nome, String endereco) {
    }

    public record PacienteConsulta(Long id, String nome, String telefone) {
    }

    // endereco: ja formatado ("Logradouro, 110 - Bairro, Cidade/UF") ou null
    // paciente: null quando quem pede e CIDADAO
    public static ConsultaResponse de(Consulta consulta, String enderecoUnidade, PacienteConsulta paciente) {
        Medico medico = consulta.getMedico();
        return new ConsultaResponse(
                consulta.getId(),
                consulta.getFila().getId(),
                consulta.getStatus(),
                consulta.getDataHora(),
                diasAte(consulta.getDataHora()),
                new ResumoDTO(consulta.getFila().getEspecialidade().getId(),
                        consulta.getFila().getEspecialidade().getNome()),
                new MedicoConsulta(medico.getId(), medico.getNome() + " " + medico.getSobrenome(),
                        medico.getCrm().replace(" ", "")),
                new UnidadeConsulta(consulta.getUnidade().getId(), consulta.getUnidade().getNome(), enderecoUnidade),
                paciente);
    }

    public static PacienteConsulta paciente(Usuario usuario, String telefone) {
        return new PacienteConsulta(usuario.getId(), usuario.getNome() + " " + usuario.getSobrenome(), telefone);
    }

    // Dias de calendario entre hoje e a data da consulta; consultas passadas ficam com 0
    private static long diasAte(LocalDateTime dataHora) {
        return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), dataHora.toLocalDate()));
    }
}
