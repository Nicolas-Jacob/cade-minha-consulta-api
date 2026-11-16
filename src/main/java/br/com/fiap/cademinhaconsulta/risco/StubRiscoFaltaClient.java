package br.com.fiap.cademinhaconsulta.risco;

import java.util.List;

// Usado enquanto o modelo de ML nao esta integrado: devolve risco e nivel null para todas as consultas
public class StubRiscoFaltaClient implements RiscoFaltaClient {

    @Override
    public List<PrevisaoRisco> prever(List<DadosPrevisaoFalta> atributos) {
        return atributos.stream()
                .map(dados -> new PrevisaoRisco(dados.idConsulta(), null, null))
                .toList();
    }
}
