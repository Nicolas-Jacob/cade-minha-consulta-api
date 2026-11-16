package br.com.fiap.cademinhaconsulta.messaging;

import br.com.fiap.cademinhaconsulta.exception.ErroOracle;
import br.com.fiap.cademinhaconsulta.service.AgendamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

// Recebe vaga.liberada e agenda o proximo paciente compativel da fila do medico (PRC_AGENDAR_CONSULTA).
// Erros sao so registrados no log e a mensagem e descartada (sem reprocessar em loop).
@Slf4j
@Component
@RequiredArgsConstructor
public class VagaLiberadaConsumer {

    private final AgendamentoService agendamentoService;

    @RabbitListener(queues = RabbitConfig.FILA_VAGA_LIBERADA)
    public void receber(VagaLiberadaEvent evento) {
        log.info("Evento vaga.liberada recebido: {}", evento);
        try {
            Long idConsulta = agendamentoService.agendarProximo(evento.idMedico());
            log.info("Vaga realocada: consulta {} agendada para o proximo da fila do medico {}",
                    idConsulta, evento.idMedico());
        } catch (Exception ex) {
            SQLException erro = ErroOracle.buscar(ex);
            if (erro != null && erro.getErrorCode() == ErroOracle.FILA_VAZIA) {
                log.info("Fila vazia: nenhum paciente aguardando para o medico {}. {}",
                        evento.idMedico(), ErroOracle.mensagem(erro));
            } else {
                log.error("Falha ao realocar a vaga da consulta {} (medico {}). Mensagem descartada.",
                        evento.idConsulta(), evento.idMedico(), ex);
            }
        }
    }
}
