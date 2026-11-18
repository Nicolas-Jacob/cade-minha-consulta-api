package br.com.fiap.cademinhaconsulta.risco;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

// Um item do POST /v1/risco-falta/lote do servico de IA (vaga-certa-ia). Campos em snake_case e tipos
// primitivos: o servico recusa o lote inteiro (422) se algum campo vier null. Nao tem nome, CPF, contato nem
// endereco do paciente. Montado pelo MapeadorRiscoFalta.
public record ItemRiscoRequest(
        @JsonProperty("consulta_id") String consultaId,
        @JsonProperty("idade") int idade,
        // "F" ou "M"
        @JsonProperty("genero") String genero,
        @JsonProperty("data_agendamento") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dataAgendamento,
        @JsonProperty("data_consulta") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dataConsulta,
        @JsonProperty("bolsa_familia") boolean bolsaFamilia,
        @JsonProperty("hipertensao") boolean hipertensao,
        @JsonProperty("diabetes") boolean diabetes,
        @JsonProperty("alcoolismo") boolean alcoolismo,
        @JsonProperty("deficiencia") boolean deficiencia,
        @JsonProperty("sms_recebido") boolean smsRecebido,
        @JsonProperty("consultas_anteriores") int consultasAnteriores,
        @JsonProperty("faltas_anteriores") int faltasAnteriores
) {
}
