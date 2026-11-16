package br.com.fiap.cademinhaconsulta.service;

import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.Map;

// Chama PKG_GS_GESTAO_CONSULTAS.PRC_AGENDAR_CONSULTA(p_id_medico IN, p_id_consulta OUT).
// A procedure faz COMMIT interno, entao NADA aqui pode ser @Transactional.
// Erros -20001 a -20007 sobem como DataAccessException e o GlobalExceptionHandler converte em HTTP.
@Service
public class AgendamentoService {

    private final SimpleJdbcCall agendarConsulta;

    public AgendamentoService(DataSource dataSource) {
        // A procedure tem overload (1 e 2 parametros): sem ler os metadados e com os parametros
        // declarados, o Spring chama a versao de 2 parametros, que devolve o ID da consulta criada
        this.agendarConsulta = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_GS_GESTAO_CONSULTAS")
                .withProcedureName("PRC_AGENDAR_CONSULTA")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_MEDICO", Types.NUMERIC),
                        new SqlOutParameter("P_ID_CONSULTA", Types.NUMERIC));
    }

    // Agenda o proximo paciente da fila do medico e devolve o id da consulta criada
    public Long agendarProximo(Long idMedico) {
        Map<String, Object> saida = agendarConsulta.execute(Map.of("P_ID_MEDICO", idMedico));
        return ((Number) saida.get("P_ID_CONSULTA")).longValue();
    }
}
