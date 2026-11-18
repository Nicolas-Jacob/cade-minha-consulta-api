package br.com.fiap.cademinhaconsulta.risco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Um item da resposta do servico de IA. Os demais campos (sinais, recomendacao) nao sao usados pela API.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemRiscoResponse(
        @JsonProperty("consulta_id") String consultaId,
        // 0 a 1
        @JsonProperty("probabilidade_falta") Double probabilidadeFalta,
        // "baixo", "medio" ou "alto", ja com os limiares por faixa etaria do modelo
        @JsonProperty("faixa_risco") String faixaRisco,
        @JsonProperty("modelo") String modelo
) {
}
