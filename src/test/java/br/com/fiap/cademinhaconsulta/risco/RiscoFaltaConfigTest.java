package br.com.fiap.cademinhaconsulta.risco;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiscoFaltaConfigTest {

    private final RiscoFaltaConfig config = new RiscoFaltaConfig();

    @Test
    void urlVaziaUsaOStub() {
        assertThat(config.riscoFaltaClient("", "", 3000, 15000)).isInstanceOf(StubRiscoFaltaClient.class);
        assertThat(config.riscoFaltaClient("   ", "", 3000, 15000)).isInstanceOf(StubRiscoFaltaClient.class);
        assertThat(config.riscoFaltaClient(null, null, 3000, 15000)).isInstanceOf(StubRiscoFaltaClient.class);
    }

    @Test
    void urlPreenchidaUsaOClienteHttp() {
        assertThat(config.riscoFaltaClient("http://localhost:8000/", "", 3000, 15000))
                .isInstanceOf(HttpRiscoFaltaClient.class);
        assertThat(config.riscoFaltaClient("http://localhost:8000", "token", 3000, 15000))
                .isInstanceOf(HttpRiscoFaltaClient.class);
    }
}
