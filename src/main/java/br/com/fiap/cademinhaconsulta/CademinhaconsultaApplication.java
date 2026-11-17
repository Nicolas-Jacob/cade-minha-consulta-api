package br.com.fiap.cademinhaconsulta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class CademinhaconsultaApplication {

	public static void main(String[] args) {
		// O contrato usa datas no horario de Brasilia, e as regras usam LocalDateTime.now() (consultas futuras,
		// data de entrada na fila, presenca). O Azure App Service Linux roda em UTC: sem isto, tudo ficaria 3h adiantado.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(CademinhaconsultaApplication.class, args);
	}

}
