package br.com.fiap.cademinhaconsulta.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Secao 8 do contrato. O RabbitAdmin do Spring declara exchange, fila e binding ao conectar no broker.
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_CONSULTAS = "consultas.exchange";
    public static final String FILA_VAGA_LIBERADA = "vaga.liberada.queue";
    public static final String ROUTING_KEY_VAGA_LIBERADA = "vaga.liberada";

    @Bean
    public TopicExchange consultasExchange() {
        return new TopicExchange(EXCHANGE_CONSULTAS);
    }

    @Bean
    public Queue vagaLiberadaQueue() {
        return new Queue(FILA_VAGA_LIBERADA, true);
    }

    @Bean
    public Binding vagaLiberadaBinding(Queue vagaLiberadaQueue, TopicExchange consultasExchange) {
        return BindingBuilder.bind(vagaLiberadaQueue).to(consultasExchange).with(ROUTING_KEY_VAGA_LIBERADA);
    }

    // Mensagens em JSON (Jackson). O Spring Boot usa este conversor no RabbitTemplate e nos listeners.
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
