package br.com.fiap.cademinhaconsulta.risco;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Corpo do POST /v1/risco-falta/lote: de 1 a 500 itens por chamada
public record LoteRiscoRequest(@JsonProperty("consultas") List<ItemRiscoRequest> consultas) {
}
