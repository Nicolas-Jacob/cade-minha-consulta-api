-- Package que agrupa as procedures relacionadas ao agendamento de consultas.
CREATE OR REPLACE PACKAGE PKG_GS_GESTAO_CONSULTAS AS
    -- Versao para a API: agenda e devolve o ID da consulta criada.
    PROCEDURE PRC_AGENDAR_CONSULTA
    (
        p_id_medico   IN  TB_GS_MEDICO.id_medico%TYPE,
        p_id_consulta OUT TB_GS_CONSULTA.id_consulta%TYPE
    );

    -- Versao simples para executar no SQL Developer.
    PROCEDURE PRC_AGENDAR_CONSULTA
    (
        p_id_medico IN TB_GS_MEDICO.id_medico%TYPE
    );

    -- Lista consultas futuras; por padrao, considera os proximos 15 dias.
    PROCEDURE PRC_CONSULTAS_PROXIMAS
    (
        p_consultas OUT SYS_REFCURSOR,
        p_dias      IN  PLS_INTEGER DEFAULT 15
    );
END PKG_GS_GESTAO_CONSULTAS;
/

-- Corpo do package: contem o codigo que executa cada procedure.
CREATE OR REPLACE PACKAGE BODY PKG_GS_GESTAO_CONSULTAS AS
    -- Procedure que escolhe a pessoa da fila e grava o agendamento.
    PROCEDURE PRC_AGENDAR_CONSULTA
(
    p_id_medico IN TB_GS_MEDICO.id_medico%TYPE,
    p_id_consulta OUT TB_GS_CONSULTA.id_consulta%TYPE
)
IS
    v_status_medico       TB_GS_MEDICO.status_medico%TYPE;
    v_id_consulta         TB_GS_CONSULTA.id_consulta%TYPE;
    v_id_fila             TB_GS_FILA.id_fila%TYPE;
    v_id_usuario          TB_GS_USUARIO.id_usuario%TYPE;
    v_data_hora           TB_GS_CONSULTA.data_hora_consulta%TYPE;
    v_fila_mais_antiga    TB_GS_FILA%ROWTYPE;
    v_fila_escolhida      TB_GS_FILA%ROWTYPE;
    v_data_limite         DATE;
    v_dia                 DATE;
    v_horario             DATE;
    v_quantidade          PLS_INTEGER;
    v_encontrou_horario   BOOLEAN := FALSE;

    e_medico_nao_encontrado EXCEPTION;
    e_medico_inativo        EXCEPTION;
    e_fila_vazia            EXCEPTION;
BEGIN
    -- Guarda um ponto de retorno caso alguma etapa do agendamento falhe.
    SAVEPOINT sp_prc_agendar_consulta;

    -- 1. Confere se o medico existe e bloqueia o registro durante o agendamento.
    -- Bloqueia o medico durante a operacao para evitar dois agendamentos
    -- simultaneos no mesmo horario por esta procedure.
    BEGIN
        SELECT status_medico
        INTO v_status_medico
        FROM TB_GS_MEDICO
        WHERE id_medico = p_id_medico
        FOR UPDATE;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE e_medico_nao_encontrado;
    END;

    IF v_status_medico <> 'ATIVO' THEN
        RAISE e_medico_inativo;
    END IF;

    -- 2. Busca a fila mais antiga compativel com o medico.
    -- Seleciona a fila mais antiga compativel com a unidade e especialidade
    -- atendidas pelo medico informado.
    BEGIN
        SELECT *
        INTO v_fila_mais_antiga
        FROM (
            SELECT f.*
            FROM TB_GS_FILA f
            WHERE f.status_fila = 'AGUARDANDO'
              AND EXISTS (
                    SELECT 1
                    FROM TB_GS_MEDICO_ESPECIALIDADE me
                    WHERE me.id_medico = p_id_medico
                      AND me.id_especialidade = f.id_especialidade
              )
              AND EXISTS (
                    SELECT 1
                    FROM TB_GS_MEDICO_UNIDADE mu
                    WHERE mu.id_medico = p_id_medico
                      AND mu.id_unidade = f.id_unidade
              )
            ORDER BY f.data_entrada, f.id_fila
        )
        WHERE ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE e_fila_vazia;
    END;

    v_fila_escolhida := v_fila_mais_antiga;

    -- 3. Aplica a regra de prioridade sem deixar a fila mais antiga esperando demais.
    -- Prioridade maior (numero maior) so pode passar a fila se estiver
    -- aguardando ha mais de 7 dias e a pessoa mais antiga ha menos de 14 dias.
    -- Com 14 dias ou mais, a pessoa mais antiga permanece na frente.
    IF v_fila_mais_antiga.data_entrada > SYSDATE - 14 THEN
        BEGIN
            SELECT *
            INTO v_fila_escolhida
            FROM (
                SELECT f.*
                FROM TB_GS_FILA f
                WHERE f.status_fila = 'AGUARDANDO'
                  AND f.data_entrada < SYSDATE - 7
                  AND f.prioridade_fila > v_fila_mais_antiga.prioridade_fila
                  AND EXISTS (
                        SELECT 1
                        FROM TB_GS_MEDICO_ESPECIALIDADE me
                        WHERE me.id_medico = p_id_medico
                          AND me.id_especialidade = f.id_especialidade
                  )
                  AND EXISTS (
                        SELECT 1
                        FROM TB_GS_MEDICO_UNIDADE mu
                        WHERE mu.id_medico = p_id_medico
                          AND mu.id_unidade = f.id_unidade
                  )
                ORDER BY f.prioridade_fila DESC, f.data_entrada, f.id_fila
            )
            WHERE ROWNUM = 1;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                v_fila_escolhida := v_fila_mais_antiga;
        END;
    END IF;

    -- Bloqueia a fila escolhida e confirma que ainda esta aguardando.
    BEGIN
        SELECT id_fila
        INTO v_id_fila
        FROM TB_GS_FILA
        WHERE id_fila = v_fila_escolhida.id_fila
          AND status_fila = 'AGUARDANDO'
        FOR UPDATE;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20004, 'A fila escolhida foi alterada. Execute novamente.');
    END;

    v_id_usuario := v_fila_escolhida.id_usuario;

    -- 4. Procura um horario livre em dia util, com pelo menos cinco dias de antecedencia.
    -- O paciente precisa de pelo menos cinco dias para ser avisado.
    -- A busca comeca em SYSDATE + 5 e segue em dias uteis, das 08h as 17h.
    -- O limite e derivado da ultima consulta agendada do medico, sem prazo fixo.
    SELECT NVL(MAX(TRUNC(data_hora_consulta)), TRUNC(SYSDATE + 5)) + 7
    INTO v_data_limite
    FROM TB_GS_CONSULTA
    WHERE id_medico = p_id_medico
      AND status_consulta IN ('AGENDADA', 'CONFIRMADA')
      AND data_hora_consulta >= SYSDATE + 5;

    v_dia := TRUNC(SYSDATE + 5);

    WHILE v_dia <= v_data_limite LOOP
        IF TO_CHAR(v_dia, 'DY', 'NLS_DATE_LANGUAGE=ENGLISH') NOT IN ('SAT', 'SUN') THEN
            FOR v_hora IN 8..17 LOOP
                v_horario := v_dia + (v_hora / 24);

                IF v_horario >= SYSDATE + 5 THEN
                    SELECT COUNT(*)
                    INTO v_quantidade
                    FROM TB_GS_CONSULTA c
                    WHERE c.id_medico = p_id_medico
                      AND c.status_consulta IN ('AGENDADA', 'CONFIRMADA')
                      AND c.data_hora_consulta < v_horario + (1 / 24)
                      AND c.data_hora_consulta + (1 / 24) > v_horario;

                    IF v_quantidade = 0 THEN
                        v_data_hora := v_horario;
                        v_encontrou_horario := TRUE;
                        EXIT;
                    END IF;
                END IF;
            END LOOP;
        END IF;

        EXIT WHEN v_encontrou_horario;
        v_dia := v_dia + 1;
    END LOOP;

    IF NOT v_encontrou_horario THEN
        RAISE_APPLICATION_ERROR(-20005, 'Nao foi encontrado horario comercial livre para o medico.');
    END IF;

    -- 5. Cria a consulta e depois tira o paciente da fila de espera.
    INSERT INTO TB_GS_CONSULTA
        (id_consulta, id_fila, id_usuario, id_medico, id_unidade,
         status_consulta, data_hora_consulta)
    VALUES
        (NULL, v_fila_escolhida.id_fila, v_fila_escolhida.id_usuario,
         p_id_medico, v_fila_escolhida.id_unidade, 'AGENDADA', v_data_hora)
    RETURNING id_consulta INTO v_id_consulta;

    UPDATE TB_GS_FILA
    SET status_fila = 'AGENDADA',
        data_saida = SYSDATE
    WHERE id_fila = v_fila_escolhida.id_fila
      AND status_fila = 'AGUARDANDO';

    IF SQL%ROWCOUNT = 0 THEN
        RAISE_APPLICATION_ERROR(-20006, 'Nao foi possivel atualizar o status da fila.');
    END IF;

    -- Confirma a consulta e a atualizacao da fila.
    -- Devolve o ID para a API antes de confirmar a transacao.
    p_id_consulta := v_id_consulta;

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Consulta agendada: ' || v_id_consulta);
    DBMS_OUTPUT.PUT_LINE('Fila: ' || v_id_fila || ' | Usuario: ' || v_id_usuario);
    DBMS_OUTPUT.PUT_LINE('Medico: ' || p_id_medico);
    DBMS_OUTPUT.PUT_LINE('Data: ' || TO_CHAR(v_data_hora, 'DD/MM/YYYY HH24:MI'));

EXCEPTION
    WHEN e_medico_nao_encontrado THEN
        ROLLBACK TO sp_prc_agendar_consulta;
        RAISE_APPLICATION_ERROR(-20001, 'Medico nao encontrado.');
    WHEN e_medico_inativo THEN
        ROLLBACK TO sp_prc_agendar_consulta;
        RAISE_APPLICATION_ERROR(-20002, 'O medico informado esta inativo.');
    WHEN e_fila_vazia THEN
        ROLLBACK TO sp_prc_agendar_consulta;
        RAISE_APPLICATION_ERROR(-20003, 'Nao ha pacientes aguardando para as especialidades e unidades deste medico.');
    WHEN OTHERS THEN
        ROLLBACK TO sp_prc_agendar_consulta;
        RAISE;
END;
    -- Mantem a chamada de um parametro para os testes e execucao manual.
    PROCEDURE PRC_AGENDAR_CONSULTA
    (
        p_id_medico IN TB_GS_MEDICO.id_medico%TYPE
    )
    IS
        v_id_consulta TB_GS_CONSULTA.id_consulta%TYPE;
    BEGIN
        PRC_AGENDAR_CONSULTA(p_id_medico, v_id_consulta);
    END PRC_AGENDAR_CONSULTA;
    -- Lista os proximos atendimentos e os dados para contato.
    PROCEDURE PRC_CONSULTAS_PROXIMAS
(
    p_consultas OUT SYS_REFCURSOR,
    p_dias      IN  PLS_INTEGER
)
IS
    v_total_consultas PLS_INTEGER;

    e_periodo_invalido EXCEPTION;
    e_sem_consultas    EXCEPTION;
BEGIN
    -- Confere se o periodo recebido e valido.
    IF p_dias IS NULL OR p_dias <= 0 THEN
        RAISE e_periodo_invalido;
    END IF;

    -- Verifica se ha consultas futuras dentro dos dias escolhidos.
    SELECT COUNT(*)
    INTO v_total_consultas
    FROM TB_GS_CONSULTA c
    WHERE c.status_consulta IN ('AGENDADA', 'CONFIRMADA')
      AND c.data_hora_consulta > SYSDATE
      AND c.data_hora_consulta <= SYSDATE + p_dias;

    IF v_total_consultas = 0 THEN
        RAISE e_sem_consultas;
    END IF;

    -- Abre o cursor com os dados que a equipe precisa visualizar.
    OPEN p_consultas FOR
        SELECT
            c.id_consulta,
            c.id_fila,
            c.status_consulta,
            c.data_hora_consulta,
            GREATEST(CEIL(c.data_hora_consulta - SYSDATE), 0) AS dias_ate_consulta,
            c.id_usuario,
            u.nome_usuario,
            u.sobrenome_usuario,
            contato.telefone_contato AS telefone_paciente,
            contato.email AS email_paciente,
            c.id_medico,
            m.nome_medico,
            m.sobrenome_medico,
            m.crm,
            e.id_especialidade,
            e.nome_especialidade,
            c.id_unidade,
            un.nome_unidade,
            endereco.logradouro,
            endereco.numero_end,
            endereco.complemento_end,
            endereco.CEP,
            endereco.bairro_end,
            endereco.cidade_end,
            endereco.UF_end
        FROM TB_GS_CONSULTA c
        JOIN TB_GS_USUARIO u
          ON u.id_usuario = c.id_usuario
        JOIN TB_GS_MEDICO m
          ON m.id_medico = c.id_medico
        JOIN TB_GS_FILA f
          ON f.id_fila = c.id_fila
        JOIN TB_GS_ESPECIALIDADE e
          ON e.id_especialidade = f.id_especialidade
        JOIN TB_GS_UNIDADE un
          ON un.id_unidade = c.id_unidade
        LEFT JOIN (
            SELECT id_usuario, id_contato
            FROM (
                SELECT
                    rel.id_usuario,
                    rel.id_contato,
                    ROW_NUMBER() OVER (
                        PARTITION BY rel.id_usuario
                        ORDER BY CASE WHEN ct.obs_contato = 'PROPRIO' THEN 1 ELSE 2 END,
                                 rel.id_contato
                    ) AS ordem
                FROM TB_GS_US_CONTATO_ENDERECO rel
                JOIN TB_GS_CONTATO ct
                  ON ct.id_contato = rel.id_contato
            )
            WHERE ordem = 1
        ) contato_paciente
          ON contato_paciente.id_usuario = c.id_usuario
        LEFT JOIN TB_GS_CONTATO contato
          ON contato.id_contato = contato_paciente.id_contato
        LEFT JOIN (
            SELECT id_unidade, id_endereco
            FROM (
                SELECT
                    rel.id_unidade,
                    rel.id_endereco,
                    ROW_NUMBER() OVER (
                        PARTITION BY rel.id_unidade
                        ORDER BY rel.id_endereco
                    ) AS ordem
                FROM TB_GS_UN_CONTATO_ENDERECO rel
            )
            WHERE ordem = 1
        ) endereco_unidade
          ON endereco_unidade.id_unidade = un.id_unidade
        LEFT JOIN TB_GS_ENDERECO endereco
          ON endereco.id_endereco = endereco_unidade.id_endereco
        WHERE c.status_consulta IN ('AGENDADA', 'CONFIRMADA')
          AND c.data_hora_consulta > SYSDATE
          AND c.data_hora_consulta <= SYSDATE + p_dias
        ORDER BY c.data_hora_consulta, c.id_consulta;
EXCEPTION
    WHEN e_periodo_invalido THEN
        RAISE_APPLICATION_ERROR(-20020, 'O periodo deve ser maior que zero dias.');
    WHEN e_sem_consultas THEN
        RAISE_APPLICATION_ERROR(-20021, 'Nao ha consultas agendadas ou confirmadas dentro do periodo informado.');
    WHEN OTHERS THEN
        IF p_consultas%ISOPEN THEN
            CLOSE p_consultas;
        END IF;
        RAISE;
END;
END PKG_GS_GESTAO_CONSULTAS;
/







