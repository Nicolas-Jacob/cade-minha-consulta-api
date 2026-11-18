package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.RiscoFaltaResponse;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import br.com.fiap.cademinhaconsulta.repository.ConsultaRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioRepository;
import br.com.fiap.cademinhaconsulta.risco.ComplementoPrevisao;
import br.com.fiap.cademinhaconsulta.risco.DadosPrevisaoFalta;
import br.com.fiap.cademinhaconsulta.risco.PrevisaoRisco;
import br.com.fiap.cademinhaconsulta.risco.RiscoFaltaClient;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Risco de falta das proximas consultas (GESTOR). Os atributos vem da funcao
// PKG_GS_CONSULTA_DADOS.FN_DADOS_PREVISAO_FALTA(p_dias), que devolve um SYS_REFCURSOR,
// e a previsao vem do RiscoFaltaClient (servico de IA vaga-certa-ia, ou o stub com risco e nivel null).
@Service
public class RiscoFaltaService {

    // Linha do cursor -> atributos do modelo (as colunas do Oracle vem em maiusculas)
    private static final RowMapper<DadosPrevisaoFalta> MAPEADOR = (rs, numeroLinha) -> new DadosPrevisaoFalta(
            rs.getLong("ID_CONSULTA"),
            rs.getLong("ID_USUARIO"),
            rs.getObject("DATA_CONSULTA", LocalDateTime.class),
            rs.getString("STATUS_AGENDAMENTO"),
            rs.getInt("DIAS_ATE_CONSULTA"),
            rs.getString("DIA_SEMANA").trim(),
            rs.getInt("HORA_CONSULTA"),
            rs.getString("TURNO_CONSULTA"),
            rs.getInt("IDADE_PACIENTE"),
            rs.getInt("PRIORIDADE_FILA"),
            rs.getLong("ID_ESPECIALIDADE"),
            rs.getString("NOME_ESPECIALIDADE"),
            rs.getLong("ID_UNIDADE"),
            rs.getString("NOME_UNIDADE"),
            rs.getInt("QTD_FALTAS_HISTORICO"),
            rs.getInt("QTD_PRESENCAS_HISTORICO"),
            rs.getInt("QTD_CANCELAMENTOS_HISTORICO"),
            rs.getDouble("TAXA_FALTAS_HISTORICA_PCT"));

    // Maximo de itens num IN do Oracle
    private static final int LIMITE_IN = 1000;

    private final SimpleJdbcCall dadosPrevisaoFalta;
    private final RiscoFaltaClient riscoFaltaClient;
    private final UsuarioRepository usuarioRepository;
    private final ConsultaRepository consultaRepository;

    public RiscoFaltaService(DataSource dataSource, RiscoFaltaClient riscoFaltaClient,
                             UsuarioRepository usuarioRepository, ConsultaRepository consultaRepository) {
        this.riscoFaltaClient = riscoFaltaClient;
        this.usuarioRepository = usuarioRepository;
        this.consultaRepository = consultaRepository;
        // Funcao: o retorno (cursor) e declarado primeiro, depois o parametro de entrada
        this.dadosPrevisaoFalta = new SimpleJdbcCall(dataSource)
                .withCatalogName("PKG_GS_CONSULTA_DADOS")
                .withFunctionName("FN_DADOS_PREVISAO_FALTA")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter("RETORNO", Types.REF_CURSOR, MAPEADOR),
                        new SqlParameter("P_DIAS", Types.INTEGER));
    }

    // Consultas AGENDADA/CONFIRMADA nos proximos "dias" dias, ordenadas pela data da consulta
    @SuppressWarnings("unchecked")
    public List<RiscoFaltaResponse> listar(int dias) {
        List<DadosPrevisaoFalta> atributos = (List<DadosPrevisaoFalta>) dadosPrevisaoFalta
                .execute(Map.of("P_DIAS", dias))
                .get("RETORNO");
        if (atributos.isEmpty()) {
            return List.of();
        }

        Map<Long, PrevisaoRisco> previsoes = riscoFaltaClient.prever(atributos, buscarComplementos(atributos)).stream()
                .collect(Collectors.toMap(PrevisaoRisco::idConsulta, Function.identity(), (a, b) -> a));
        // O nome do paciente nao vem do cursor nem vai para o modelo: so aparece na resposta para o GESTOR
        Map<Long, String> pacientes = usuarioRepository
                .findAllById(atributos.stream().map(DadosPrevisaoFalta::idUsuario).distinct().toList()).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u.getNome() + " " + u.getSobrenome()));

        return atributos.stream()
                .sorted(Comparator.comparing(DadosPrevisaoFalta::dataConsulta)
                        .thenComparing(DadosPrevisaoFalta::idConsulta))
                .map(dados -> {
                    PrevisaoRisco previsao = previsoes.get(dados.idConsulta());
                    return new RiscoFaltaResponse(
                            dados.idConsulta(),
                            pacientes.get(dados.idUsuario()),
                            dados.dataConsulta(),
                            dados.especialidade(),
                            previsao != null ? previsao.risco() : null,
                            previsao != null ? previsao.nivel() : null);
                })
                .toList();
    }

    // Sexo e entrada na fila (o cursor nao traz), em blocos de ate 1000 ids por consulta
    private Map<Long, ComplementoPrevisao> buscarComplementos(List<DadosPrevisaoFalta> atributos) {
        List<Long> ids = atributos.stream().map(DadosPrevisaoFalta::idConsulta).distinct().toList();
        Map<Long, ComplementoPrevisao> complementos = new HashMap<>();
        for (int i = 0; i < ids.size(); i += LIMITE_IN) {
            consultaRepository.buscarComplementosPrevisao(ids.subList(i, Math.min(i + LIMITE_IN, ids.size())))
                    .forEach(complemento -> complementos.put(complemento.idConsulta(), complemento));
        }
        return complementos;
    }
}
