package br.com.fiap.cademinhaconsulta.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// O ConsultaService publica o VagaLiberadaEvent dentro da transacao do cancelamento.
// Este listener so roda DEPOIS do commit: se a transacao falhar, nada vai para o RabbitMQ.
@Slf4j
@Component
@RequiredArgsConstructor
public class VagaLiberadaPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(VagaLiberadaEvent evento) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CONSULTAS, RabbitConfig.ROUTING_KEY_VAGA_LIBERADA,
                    evento);
            log.info("Evento vaga.liberada publicado: {}", evento);
        } catch (Exception ex) {
            // O cancelamento ja foi confirmado no banco; a vaga so nao sera realocada automaticamente
            log.error("Falha ao publicar vaga.liberada da consulta {} (RabbitMQ indisponivel?). "
                    + "O cancelamento foi mantido, mas a vaga nao sera realocada automaticamente: {}",
                    evento.idConsulta(), ex.getMessage());
        }
    }
}
