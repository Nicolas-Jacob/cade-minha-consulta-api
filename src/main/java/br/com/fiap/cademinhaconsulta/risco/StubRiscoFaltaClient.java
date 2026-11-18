package br.com.fiap.cademinhaconsulta.risco;

import java.util.List;
import java.util.Map;

// Usado quando RISCO_FALTA_URL esta vazia: devolve risco e nivel null para todas as consultas
public class StubRiscoFaltaClient implements RiscoFaltaClient {

    @Override
    public List<PrevisaoRisco> prever(List<DadosPrevisaoFalta> atributos, Map<Long, ComplementoPrevisao> complementos) {
        return atributos.stream()
                .map(dados -> new PrevisaoRisco(dados.idConsulta(), null, null))
                .toList();
    }
}
