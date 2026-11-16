-- Package com funcoes de consulta do paciente e de preparacao de dados para a IA.
CREATE OR REPLACE PACKAGE PKG_GS_CONSULTA_DADOS AS
    -- Retorna as consultas futuras do usuario informado.
    FUNCTION FN_MINHAS_CONSULTAS
    (
        p_id_usuario IN TB_GS_USUARIO.id_usuario%TYPE
    )
    RETURN SYS_REFCURSOR;

    -- Retorna atributos das consultas futuras para o modelo de faltas.
    FUNCTION FN_DADOS_PREVISAO_FALTA
    (
        p_dias IN PLS_INTEGER DEFAULT 15
    )
    RETURN SYS_REFCURSOR;
END PKG_GS_CONSULTA_DADOS;
/

-- Corpo do package: implementa as duas funcoes declaradas acima.
CREATE OR REPLACE PACKAGE BODY PKG_GS_CONSULTA_DADOS AS
    -- Consulta os agendamentos futuros e os dados necessarios para a tela do paciente.
    FUNCTION FN_MINHAS_CONSULTAS
(
    p_id_usuario IN TB_GS_USUARIO.id_usuario%TYPE
)
RETURN SYS_REFCURSOR
IS
    v_consultas       SYS_REFCURSOR;
    v_id_usuario      TB_GS_USUARIO.id_usuario%TYPE;
    v_total_consultas PLS_INTEGER;

    e_id_usuario_nulo       EXCEPTION;
    e_usuario_nao_encontrado EXCEPTION;
    e_sem_consultas          EXCEPTION;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE e_id_usuario_nulo;
    END IF;

    BEGIN
        SELECT id_usuario
        INTO v_id_usuario
        FROM TB_GS_USUARIO
        WHERE id_usuario = p_id_usuario;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE e_usuario_nao_encontrado;
    END;

    SELECT COUNT(*)
    INTO v_total_consultas
    FROM TB_GS_CONSULTA c
    WHERE c.id_usuario = v_id_usuario
      AND c.status_consulta IN ('AGENDADA', 'CONFIRMADA')
      AND c.data_hora_consulta > SYSDATE;

    IF v_total_consultas = 0 THEN
        RAISE e_sem_consultas;
    END IF;

    OPEN v_consultas FOR
        SELECT
            c.id_consulta,
            c.id_fila,
            c.status_consulta,
            c.data_hora_consulta,
            TO_CHAR(c.data_hora_consulta, 'DD/MM/YYYY HH24:MI:SS') AS data_hora_formatada,
            GREATEST(CEIL(c.data_hora_consulta - SYSDATE), 0) AS dias_ate_consulta,
            m.id_medico,
            m.nome_medico,
            m.sobrenome_medico,
            m.crm,
            m.uf_crm,
            e.id_especialidade,
            e.nome_especialidade,
            u.id_unidade,
            u.nome_unidade,
            en.logradouro,
            en.numero_end,
            en.complemento_end,
            en.CEP,
            en.bairro_end,
            en.cidade_end,
            en.UF_end
        FROM TB_GS_CONSULTA c
        JOIN TB_GS_MEDICO m
          ON m.id_medico = c.id_medico
        JOIN TB_GS_FILA f
          ON f.id_fila = c.id_fila
        JOIN TB_GS_ESPECIALIDADE e
          ON e.id_especialidade = f.id_especialidade
        JOIN TB_GS_UNIDADE u
          ON u.id_unidade = c.id_unidade
        LEFT JOIN TB_GS_UN_CONTATO_ENDERECO uce
          ON uce.id_unidade = u.id_unidade
        LEFT JOIN TB_GS_ENDERECO en
          ON en.id_endereco = uce.id_endereco
        WHERE c.id_usuario = v_id_usuario
          AND c.status_consulta IN ('AGENDADA', 'CONFIRMADA')
          AND c.data_hora_consulta > SYSDATE
        ORDER BY c.data_hora_consulta;

    RETURN v_consultas;
EXCEPTION
    WHEN e_id_usuario_nulo THEN
        RAISE_APPLICATION_ERROR(-20010, 'Informe o ID do usuario.');
    WHEN e_usuario_nao_encontrado THEN
        RAISE_APPLICATION_ERROR(-20011, 'Usuario nao encontrado.');
    WHEN e_sem_consultas THEN
        RAISE_APPLICATION_ERROR(-20012, 'O usuario nao possui consultas futuras agendadas ou confirmadas.');
    WHEN OTHERS THEN
        IF v_consultas%ISOPEN THEN
            CLOSE v_consultas;
        END IF;
        RAISE;
END FN_MINHAS_CONSULTAS;

    -- Prepara dados das consultas futuras e o historico do paciente para a IA.
    FUNCTION FN_DADOS_PREVISAO_FALTA
(
    p_dias IN PLS_INTEGER
)
RETURN SYS_REFCURSOR
IS
    v_dados SYS_REFCURSOR;
    e_periodo_invalido EXCEPTION;
BEGIN
    -- O periodo deve ser positivo para a busca de consultas futuras.
    IF p_dias IS NULL OR p_dias <= 0 THEN
        RAISE e_periodo_invalido;
    END IF;

    -- Cada linha do resultado representa uma consulta que ainda vai acontecer.
    -- Os campos retornados sao variaveis que podem ser usadas pelo modelo.
    OPEN v_dados FOR
        SELECT
            c.id_consulta,
            c.id_usuario,
            c.data_hora_consulta AS data_consulta,
            TO_CHAR(c.data_hora_consulta, 'DD/MM/YYYY HH24:MI:SS') AS data_hora_formatada,
            c.status_consulta AS status_agendamento,
            GREATEST(CEIL(c.data_hora_consulta - SYSDATE), 0) AS dias_ate_consulta,
            TO_CHAR(c.data_hora_consulta, 'DY', 'NLS_DATE_LANGUAGE=ENGLISH') AS dia_semana,
            TO_NUMBER(TO_CHAR(c.data_hora_consulta, 'HH24')) AS hora_consulta,
            CASE WHEN TO_NUMBER(TO_CHAR(c.data_hora_consulta, 'HH24')) < 12 THEN 'MANHA' ELSE 'TARDE' END AS turno_consulta,
            TRUNC(MONTHS_BETWEEN(c.data_hora_consulta, u.data_nasc) / 12) AS idade_paciente,
            f.prioridade_fila,
            f.id_especialidade,
            e.nome_especialidade,
            c.id_unidade,
            un.nome_unidade,
            NVL(h.qtd_faltas, 0) AS qtd_faltas_historico,
            NVL(h.qtd_presencas, 0) AS qtd_presencas_historico,
            NVL(h.qtd_cancelamentos, 0) AS qtd_cancelamentos_historico,
            CASE
                WHEN NVL(h.qtd_faltas, 0) + NVL(h.qtd_presencas, 0) = 0 THEN 0
                ELSE ROUND(
                    NVL(h.qtd_faltas, 0) * 100 /
                    (NVL(h.qtd_faltas, 0) + NVL(h.qtd_presencas, 0)),
                    2
                )
            END AS taxa_faltas_historica_pct
        FROM TB_GS_CONSULTA c
        JOIN TB_GS_USUARIO u
          ON u.id_usuario = c.id_usuario
        JOIN TB_GS_FILA f
          ON f.id_fila = c.id_fila
        JOIN TB_GS_ESPECIALIDADE e
          ON e.id_especialidade = f.id_especialidade
        JOIN TB_GS_UNIDADE un
          ON un.id_unidade = c.id_unidade
        LEFT JOIN (
            SELECT
                h.id_usuario,
                COUNT(CASE
                          WHEN h.status_consulta = 'FALTA'
                           AND h.data_hora_consulta < SYSDATE THEN 1
                      END) AS qtd_faltas,
                COUNT(CASE
                          WHEN h.status_consulta = 'PRESENTE'
                           AND h.data_hora_consulta < SYSDATE THEN 1
                      END) AS qtd_presencas,
                COUNT(CASE
                          WHEN h.status_consulta = 'CANCELADA' THEN 1
                      END) AS qtd_cancelamentos
            FROM TB_GS_CONSULTA h
            GROUP BY h.id_usuario
        ) h
          ON h.id_usuario = c.id_usuario
        WHERE c.status_consulta IN ('AGENDADA', 'CONFIRMADA')
          AND c.data_hora_consulta > SYSDATE
          AND c.data_hora_consulta <= SYSDATE + p_dias
        ORDER BY c.data_hora_consulta, c.id_consulta;

    RETURN v_dados;
EXCEPTION
    WHEN e_periodo_invalido THEN
        RAISE_APPLICATION_ERROR(-20030, 'O periodo para buscar consultas deve ser maior que zero dias.');
    WHEN OTHERS THEN
        IF v_dados%ISOPEN THEN
            CLOSE v_dados;
        END IF;
        RAISE;
END FN_DADOS_PREVISAO_FALTA;
END PKG_GS_CONSULTA_DADOS;
/


