package br.com.fiap.cademinhaconsulta.risco;

import java.util.List;

// Ponto de integracao com o modelo de ML de risco de falta (feito por outro integrante).
// Hoje: StubRiscoFaltaClient. Depois: um cliente HTTP (RestClient) que envia os atributos ao servico do modelo.
// A implementacao usada e escolhida no RiscoFaltaConfig pela propriedade risco-falta.url.
public interface RiscoFaltaClient {

    // Uma previsao por consulta recebida, identificada pelo idConsulta
    List<PrevisaoRisco> prever(List<DadosPrevisaoFalta> atributos);
}
