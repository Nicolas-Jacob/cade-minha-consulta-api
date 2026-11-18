package br.com.fiap.cademinhaconsulta.risco;

import java.util.List;
import java.util.Map;

// Ponto de integracao com o modelo de ML de risco de falta (servico vaga-certa-ia, feito por outro integrante).
// Implementacoes: StubRiscoFaltaClient (risco null) e HttpRiscoFaltaClient (RestClient para o servico).
// A implementacao usada e escolhida no RiscoFaltaConfig pela propriedade risco-falta.url.
public interface RiscoFaltaClient {

    // Uma previsao por consulta recebida, identificada pelo idConsulta. complementos: sexo e entrada na fila por
    // idConsulta (dados que o cursor nao traz).
    List<PrevisaoRisco> prever(List<DadosPrevisaoFalta> atributos, Map<Long, ComplementoPrevisao> complementos);
}
