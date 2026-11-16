package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusFila;
import br.com.fiap.cademinhaconsulta.model.Fila;

import java.time.LocalDateTime;

public record FilaResponse(
        Long id,
        StatusFila status,
        Long prioridade,
        ResumoDTO especialidade,
        ResumoDTO unidade,
        LocalDateTime dataEntrada,
        LocalDateTime dataSaida,
        // Calculada so para AGUARDANDO; null nos demais status
        Long posicaoEstimada
) {

    public static FilaResponse de(Fila fila, Long posicaoEstimada) {
        return new FilaResponse(
                fila.getId(),
                fila.getStatus(),
                fila.getPrioridade(),
                new ResumoDTO(fila.getEspecialidade().getId(), fila.getEspecialidade().getNome()),
                new ResumoDTO(fila.getUnidade().getId(), fila.getUnidade().getNome()),
                fila.getDataEntrada(),
                fila.getDataSaida(),
                posicaoEstimada);
    }
}
