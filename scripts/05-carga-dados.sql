-- ===================================================================================================================
-- REQUISITO: Carga e Povoamento de Dados com Diversidade Estatistica
-- DESCRICAO: Script de carga inicial para o sistema de gestao de unidades de saude.
--            Gera usuarios, contatos, enderecos, unidades, medicos, especialidades,
--            vinculos profissionais, filas de atendimento e consultas.
-- OBSERVACAO: Os identificadores sao gerados pelos gatilhos e sequencias do banco.
-- ===================================================================================================================


-- CADASTRO DE USUARIOS, CONTATOS E ENDERECOS
DECLARE
    TYPE t_lista IS TABLE OF VARCHAR2(100);

    v_nomes_m       t_lista := t_lista('Gabriel', 'Lucas', 'Marcos', 'Rafael', 'Rodrigo', 'Bruno', 'Thiago', 'Felipe', 'Andre', 'Ricardo', 'Fabio', 'Leonardo', 'Gustavo', 'Mateus', 'Joao', 'Victor');
    v_nomes_f       t_lista := t_lista('Ana', 'Julia', 'Fernanda', 'Beatriz', 'Camila', 'Larissa', 'Mariana', 'Leticia', 'Amanda', 'Priscila', 'Carolina', 'Debora', 'Bianca', 'Vanessa', 'Isabela', 'Sabrina');
    v_sobrenomes    t_lista := t_lista('Silva', 'Santos', 'Oliveira', 'Souza', 'Pereira', 'Costa', 'Carvalho', 'Almeida', 'Ferreira', 'Ribeiro', 'Mendes', 'Nascimento', 'Alves', 'Monteiro', 'Cardoso', 'Moraes');
    v_logradouros   t_lista := t_lista('Avenida Paulista', 'Rua Augusta', 'Rua da Consolacao', 'Avenida Brasil', 'Rua XV de Novembro', 'Avenida Independencia');
    v_bairros       t_lista := t_lista('Centro', 'Jardim America', 'Vila Nova', 'Bela Vista', 'Jardim Paulista', 'Vila Mariana');
    v_cidades       t_lista := t_lista('Sao Paulo', 'Campinas', 'Santos', 'Ribeirao Preto', 'Sao Jose dos Campos', 'Sorocaba');
    v_ufs           t_lista := t_lista('SP', 'SP', 'SP', 'SP', 'SP', 'SP');
    v_nomes_mae     t_lista := t_lista('Maria', 'Aparecida', 'Helena', 'Patricia', 'Sandra', 'Lucia');
    v_nomes_pai     t_lista := t_lista('Jose', 'Antonio', 'Paulo', 'Carlos', 'Roberto', 'Marcos');

    v_nome          VARCHAR2(100);
    v_sobrenome     VARCHAR2(100);
    v_sexo          CHAR(1);
    v_cpf           CHAR(11);
    v_login         VARCHAR2(20);
    v_telefone      VARCHAR2(11);
    v_email         VARCHAR2(100);
    v_complemento   VARCHAR2(20);
    v_indice        PLS_INTEGER;
    v_indice_loc    PLS_INTEGER;
    v_id_contato    INTEGER;
    v_id_endereco   INTEGER;
    v_id_usuario    INTEGER;
    v_logradouro    VARCHAR2(100);
    v_bairro        VARCHAR2(100);
    v_cidade        VARCHAR2(50);
    v_uf            CHAR(2);
    v_nome_mae      VARCHAR2(100);
    v_nome_pai      VARCHAR2(100);
BEGIN
    -- O primeiro usuario inserido recebe o ID 1 e e o gestor do sistema.
    INSERT INTO TB_GS_CONTATO (id_contato, telefone_contato, email, obs_contato)
    VALUES (NULL, '11990000000', 'gestor@saude.com', 'PROPRIO')
    RETURNING id_contato INTO v_id_contato;

    INSERT INTO TB_GS_ENDERECO (id_endereco, logradouro, numero_end, complemento_end, CEP, bairro_end, cidade_end, UF_end)
    VALUES (NULL, 'Avenida Paulista', 100, NULL, '01310000', 'Centro', 'Sao Paulo', 'SP')
    RETURNING id_endereco INTO v_id_endereco;

    INSERT INTO TB_GS_USUARIO (id_usuario, nome_usuario, sobrenome_usuario, cpf, sexo_usuario,
                               data_nasc, senha, login, nome_mae, nome_pai, credencial)
    VALUES (NULL, 'Gestor', 'CitizenTech', '99999999999', 'M',
            ADD_MONTHS(TRUNC(SYSDATE), -480), 'SenhaGestor2026', 'gestor',
            'Maria', 'Jose', 'GESTOR')
    RETURNING id_usuario INTO v_id_usuario;

    INSERT INTO TB_GS_US_CONTATO_ENDERECO (id_usuario, id_contato, id_endereco)
    VALUES (v_id_usuario, v_id_contato, v_id_endereco);

    FOR i IN 1..80 LOOP
        v_indice := TRUNC(DBMS_RANDOM.VALUE(1, v_nomes_m.COUNT + 1));
        v_indice_loc := TRUNC(DBMS_RANDOM.VALUE(1, v_logradouros.COUNT + 1));

        IF MOD(i, 2) = 0 THEN
            v_sexo := 'F';
            v_nome := v_nomes_f(TRUNC(DBMS_RANDOM.VALUE(1, v_nomes_f.COUNT + 1)));
        ELSE
            v_sexo := 'M';
            v_nome := v_nomes_m(v_indice);
        END IF;

        v_sobrenome := v_sobrenomes(TRUNC(DBMS_RANDOM.VALUE(1, v_sobrenomes.COUNT + 1))) || ' ' ||
                       v_sobrenomes(TRUNC(DBMS_RANDOM.VALUE(1, v_sobrenomes.COUNT + 1)));
        v_cpf := LPAD(i, 11, '0');
        IF i = 1 THEN
            v_login := 'cidadao';
        ELSE
            v_login := LOWER(SUBSTR(v_nome, 1, 1) || SUBSTR(v_sobrenome, 1, 1) || LPAD(i, 3, '0'));
        END IF;
        v_telefone := '119' || LPAD(TRUNC(DBMS_RANDOM.VALUE(0, 100000000)), 8, '0');
        v_email := LOWER(v_nome || '.' || REPLACE(v_sobrenome, ' ', '') || i || '@email.com');

        IF DBMS_RANDOM.VALUE(0, 1) > 0.65 THEN
            v_complemento := 'Apto ' || TRUNC(DBMS_RANDOM.VALUE(1, 151));
        ELSE
            v_complemento := NULL;
        END IF;

        v_logradouro := v_logradouros(v_indice_loc);
        v_bairro := v_bairros(v_indice_loc);
        v_cidade := v_cidades(v_indice_loc);
        v_uf := v_ufs(v_indice_loc);
        v_nome_mae := v_nomes_mae(TRUNC(DBMS_RANDOM.VALUE(1, v_nomes_mae.COUNT + 1)));
        v_nome_pai := v_nomes_pai(TRUNC(DBMS_RANDOM.VALUE(1, v_nomes_pai.COUNT + 1)));

        INSERT INTO TB_GS_CONTATO (id_contato, telefone_contato, email, obs_contato)
        VALUES (NULL, v_telefone, v_email, CASE WHEN MOD(i, 5) = 0 THEN 'EMERGENCIA' ELSE 'PROPRIO' END)
        RETURNING id_contato INTO v_id_contato;

        INSERT INTO TB_GS_ENDERECO (id_endereco, logradouro, numero_end, complemento_end, CEP, bairro_end, cidade_end, UF_end)
        VALUES (NULL, v_logradouro, TRUNC(DBMS_RANDOM.VALUE(1, 5000)), v_complemento,
                LPAD(TRUNC(DBMS_RANDOM.VALUE(0, 100000000)), 8, '0'), v_bairro, v_cidade, v_uf)
        RETURNING id_endereco INTO v_id_endereco;

        INSERT INTO TB_GS_USUARIO (id_usuario, nome_usuario, sobrenome_usuario, cpf, sexo_usuario,
                                   data_nasc, senha, login, nome_mae, nome_pai, credencial)
        VALUES (NULL, v_nome, v_sobrenome, v_cpf, v_sexo,
                ADD_MONTHS(TRUNC(SYSDATE), -TRUNC(DBMS_RANDOM.VALUE(216, 780))),
                CASE WHEN i = 1 THEN 'Senha123' ELSE 'Senha' || LPAD(i, 4, '0') END, v_login,
                v_nome_mae, v_nome_pai, 'CIDADAO')
        RETURNING id_usuario INTO v_id_usuario;

        INSERT INTO TB_GS_US_CONTATO_ENDERECO (id_usuario, id_contato, id_endereco)
        VALUES (v_id_usuario, v_id_contato, v_id_endereco);
    END LOOP;

    COMMIT;
END;
/

-- CADASTRO DE UNIDADES, ESPECIALIDADES, MEDICOS E VINCULOS
DECLARE
    TYPE t_lista IS TABLE OF VARCHAR2(100);
    TYPE t_ids IS TABLE OF INTEGER;

    v_unidades       t_lista := t_lista('Unidade Central', 'Unidade Vila Mariana', 'Unidade Pinheiros', 'Unidade Campinas', 'Unidade Santos', 'Unidade Ribeirao Preto');
    v_especialidades t_lista := t_lista('Clinica Geral', 'Cardiologia', 'Dermatologia', 'Ortopedia', 'Pediatria', 'Ginecologia', 'Oftalmologia', 'Neurologia', 'Endocrinologia', 'Psiquiatria', 'Urologia', 'Otorrinolaringologia');
    v_descricoes     t_lista := t_lista('Atendimento clinico geral e preventivo.', 'Diagnostico e tratamento de doencas cardiacas.', 'Avaliacao e tratamento de condicoes da pele.', 'Diagnostico e tratamento do sistema locomotor.', 'Atendimento medico para criancas e adolescentes.', 'Atendimento em saude da mulher.', 'Avaliacao e tratamento da saude ocular.', 'Diagnostico e tratamento de condicoes neurologicas.', 'Acompanhamento hormonal e metabolico.', 'Avaliacao e acompanhamento em saude mental.', 'Diagnostico e tratamento do sistema urinario.', 'Avaliacao de ouvido, nariz e garganta.');
    v_nomes_m       t_lista := t_lista('Marcos', 'Rafael', 'Bruno', 'Thiago', 'Felipe', 'Ricardo', 'Gustavo', 'Mateus', 'Andre', 'Paulo', 'Carlos', 'Roberto');
    v_nomes_f       t_lista := t_lista('Ana', 'Julia', 'Fernanda', 'Beatriz', 'Camila', 'Larissa', 'Mariana', 'Leticia', 'Amanda', 'Carolina', 'Bianca', 'Isabela');
    v_sobrenomes    t_lista := t_lista('Silva', 'Santos', 'Oliveira', 'Souza', 'Pereira', 'Costa', 'Carvalho', 'Almeida', 'Ferreira', 'Ribeiro', 'Mendes', 'Monteiro');
    v_logradouros   t_lista := t_lista('Avenida Paulista', 'Rua Augusta', 'Rua XV de Novembro', 'Avenida Brasil', 'Rua das Flores', 'Avenida Independencia');
    v_bairros       t_lista := t_lista('Centro', 'Jardim America', 'Vila Nova', 'Bela Vista', 'Jardim Paulista', 'Vila Mariana');
    v_cidades       t_lista := t_lista('Sao Paulo', 'Campinas', 'Santos', 'Ribeirao Preto', 'Sao Jose dos Campos', 'Sorocaba');
    v_ufs           t_lista := t_lista('SP', 'SP', 'SP', 'SP', 'SP', 'SP');
    v_ids_unidades  t_ids;
    v_ids_especial  t_ids;
    v_id_contato    INTEGER;
    v_id_endereco   INTEGER;
    v_id_unidade    INTEGER;
    v_id_especial   INTEGER;
    v_id_medico     INTEGER;
    v_id_un_sel     INTEGER;
    v_id_esp_sel    INTEGER;
    v_telefone      VARCHAR2(11);
    v_crm           CHAR(10);
    v_crm_existente PLS_INTEGER;
    v_nome          VARCHAR2(100);
    v_sobrenome     VARCHAR2(100);
    v_unidade_nome  VARCHAR2(100);
    v_nome_esp      VARCHAR2(50);
    v_desc_esp      VARCHAR2(200);
    v_logradouro    VARCHAR2(100);
    v_bairro        VARCHAR2(100);
    v_cidade        VARCHAR2(50);
    v_uf            CHAR(2);
    v_indice        PLS_INTEGER;
BEGIN
    FOR i IN 1..v_unidades.COUNT LOOP
        v_unidade_nome := v_unidades(i);
        v_logradouro := v_logradouros(i);
        v_bairro := v_bairros(i);
        v_cidade := v_cidades(i);
        v_uf := v_ufs(i);

        INSERT INTO TB_GS_UNIDADE (id_unidade, nome_unidade, status_unidade)
        VALUES (NULL, v_unidade_nome, 'ATIVO')
        RETURNING id_unidade INTO v_id_unidade;

        v_telefone := '113' || LPAD(i, 8, '0');
        INSERT INTO TB_GS_CONTATO (id_contato, telefone_contato, email, obs_contato)
        VALUES (NULL, v_telefone, 'contato.unidade' || i || '@saude.com', 'PROPRIO')
        RETURNING id_contato INTO v_id_contato;

        INSERT INTO TB_GS_ENDERECO (id_endereco, logradouro, numero_end, complemento_end, CEP, bairro_end, cidade_end, UF_end)
        VALUES (NULL, v_logradouro, 100 + (i * 10), NULL, LPAD(1000000 + i, 8, '0'),
                v_bairro, v_cidade, v_uf)
        RETURNING id_endereco INTO v_id_endereco;

        INSERT INTO TB_GS_UN_CONTATO_ENDERECO (id_unidade, id_contato, id_endereco)
        VALUES (v_id_unidade, v_id_contato, v_id_endereco);
    END LOOP;

    FOR i IN 1..v_especialidades.COUNT LOOP
        v_nome_esp := v_especialidades(i);
        v_desc_esp := v_descricoes(i);
        INSERT INTO TB_GS_ESPECIALIDADE (id_especialidade, nome_especialidade, descricao_especialidade)
        VALUES (NULL, v_nome_esp, v_desc_esp)
        RETURNING id_especialidade INTO v_id_especial;
    END LOOP;

    SELECT id_unidade BULK COLLECT INTO v_ids_unidades FROM TB_GS_UNIDADE ORDER BY id_unidade;
    SELECT id_especialidade BULK COLLECT INTO v_ids_especial FROM TB_GS_ESPECIALIDADE ORDER BY id_especialidade;

    FOR i IN 1..30 LOOP
        IF MOD(i, 2) = 0 THEN
            v_nome := v_nomes_f(TRUNC(DBMS_RANDOM.VALUE(1, v_nomes_f.COUNT + 1)));
        ELSE
            v_nome := v_nomes_m(TRUNC(DBMS_RANDOM.VALUE(1, v_nomes_m.COUNT + 1)));
        END IF;
        v_sobrenome := v_sobrenomes(TRUNC(DBMS_RANDOM.VALUE(1, v_sobrenomes.COUNT + 1)));
        v_indice := MOD(i - 1, v_ids_unidades.COUNT) + 1;
        v_id_un_sel := v_ids_unidades(v_indice);
        LOOP
            v_crm := 'CRM' || TO_CHAR(TRUNC(DBMS_RANDOM.VALUE(1000000, 10000000)));
            SELECT COUNT(*) INTO v_crm_existente FROM TB_GS_MEDICO WHERE crm = v_crm;
            EXIT WHEN v_crm_existente = 0;
        END LOOP;

        INSERT INTO TB_GS_MEDICO (id_medico, nome_medico, sobrenome_medico, crm, uf_crm, status_medico)
        VALUES (NULL, v_nome, v_sobrenome, v_crm, 'SP', CASE WHEN MOD(i, 10) = 0 THEN 'INATIVO' ELSE 'ATIVO' END)
        RETURNING id_medico INTO v_id_medico;

        INSERT INTO TB_GS_MEDICO_UNIDADE (id_medico, id_unidade)
        VALUES (v_id_medico, v_id_un_sel);

        v_id_esp_sel := v_ids_especial(MOD(i - 1, v_ids_especial.COUNT) + 1);
        INSERT INTO TB_GS_MEDICO_ESPECIALIDADE (id_medico, id_especialidade)
        VALUES (v_id_medico, v_id_esp_sel);

        v_id_esp_sel := v_ids_especial(MOD(i, v_ids_especial.COUNT) + 1);
        INSERT INTO TB_GS_MEDICO_ESPECIALIDADE (id_medico, id_especialidade)
        VALUES (v_id_medico, v_id_esp_sel);
    END LOOP;

    COMMIT;
END;
/
-- FILA E CONSULTAS: UMA FILA POR USUARIO/ESPECIALIDADE
DECLARE
    TYPE t_ids IS TABLE OF INTEGER;

    v_usuarios             t_ids;
    v_unidades             t_ids;
    v_especialidades       t_ids;
    v_id_usuario           INTEGER;
    v_id_unidade           INTEGER;
    v_id_especialidade     INTEGER;
    v_id_fila              INTEGER;
    v_id_medico            INTEGER;
    v_prioridade           INTEGER;
    v_status_consulta      VARCHAR2(10);
    v_status_futuro        VARCHAR2(10);
    v_data_consulta        DATE;
    v_data_hora_agendada   DATE;
    v_dia_agendado          DATE;
    v_encontrou_horario    BOOLEAN := FALSE;
    v_indice_unidade       PLS_INTEGER;
    v_indice_especial      PLS_INTEGER;
    v_indice_medico        PLS_INTEGER;
    v_tipo_historico       PLS_INTEGER;
    v_total_cancelados     PLS_INTEGER;
    v_total_agendados      PLS_INTEGER;
    v_total_confirmados    PLS_INTEGER;
BEGIN
    SELECT id_usuario BULK COLLECT INTO v_usuarios
    FROM TB_GS_USUARIO
    WHERE credencial = 'CIDADAO'
    ORDER BY id_usuario;

    SELECT id_unidade BULK COLLECT INTO v_unidades
    FROM TB_GS_UNIDADE
    ORDER BY id_unidade;

    SELECT id_especialidade BULK COLLECT INTO v_especialidades
    FROM TB_GS_ESPECIALIDADE
    ORDER BY id_especialidade;

    v_total_cancelados := TRUNC(v_usuarios.COUNT * 0.10);
    v_total_agendados := TRUNC(v_usuarios.COUNT * 0.20);
    v_total_confirmados := TRUNC(v_total_agendados * 0.25);

    -- Escolhe um horario comercial em dia util, com pelo menos cinco dias de antecedencia.
    v_dia_agendado := TRUNC(SYSDATE + 5);
    LOOP
        IF TO_CHAR(v_dia_agendado, 'DY', 'NLS_DATE_LANGUAGE=ENGLISH') NOT IN ('SAT', 'SUN') THEN
            FOR v_hora IN 8..17 LOOP
                v_data_hora_agendada := v_dia_agendado + (v_hora / 24);
                IF v_data_hora_agendada >= SYSDATE + 5 THEN
                    v_encontrou_horario := TRUE;
                    EXIT;
                END IF;
            END LOOP;
        END IF;
        EXIT WHEN v_encontrou_horario;
        v_dia_agendado := v_dia_agendado + 1;
    END LOOP;

    FOR i IN 1..v_usuarios.COUNT LOOP
        v_id_usuario := v_usuarios(i);
        v_indice_unidade := MOD(i - 1, v_unidades.COUNT) + 1;
        v_indice_especial := MOD(i - 1, v_especialidades.COUNT) + 1;
        v_id_unidade := v_unidades(v_indice_unidade);
        v_id_especialidade := v_especialidades(v_indice_especial);
        v_prioridade := TRUNC(DBMS_RANDOM.VALUE(1, 6));

        -- Cada paciente recebe uma unica fila para esta especialidade.
        INSERT INTO TB_GS_FILA
            (id_fila, id_usuario, id_unidade, id_especialidade, prioridade_fila,
             data_entrada, data_saida, status_fila)
        VALUES
            (NULL, v_id_usuario, v_id_unidade, v_id_especialidade, v_prioridade,
             SYSDATE - TRUNC(DBMS_RANDOM.VALUE(0, 31)), NULL, 'AGUARDANDO')
        RETURNING id_fila INTO v_id_fila;

        -- Busca medico ativo que atenda a especialidade e a unidade da fila.
        SELECT MIN(m.id_medico)
        INTO v_id_medico
        FROM TB_GS_MEDICO m
        JOIN TB_GS_MEDICO_ESPECIALIDADE me
          ON me.id_medico = m.id_medico
        WHERE m.status_medico = 'ATIVO'
          AND me.id_especialidade = v_id_especialidade
          AND EXISTS (
                SELECT 1
                FROM TB_GS_MEDICO_UNIDADE mu
                WHERE mu.id_medico = m.id_medico
                  AND mu.id_unidade = v_id_unidade
          );

        -- Historico ficticio: presente, falta ou cancelamento.
        v_tipo_historico := TRUNC(DBMS_RANDOM.VALUE(1, 4));
        IF v_tipo_historico = 1 THEN
            v_status_consulta := 'PRESENTE';
            v_data_consulta := TRUNC(SYSDATE) - TRUNC(DBMS_RANDOM.VALUE(1, 366)) +
                               (TRUNC(DBMS_RANDOM.VALUE(8, 18)) / 24);
        ELSIF v_tipo_historico = 2 THEN
            v_status_consulta := 'FALTA';
            v_data_consulta := TRUNC(SYSDATE) - TRUNC(DBMS_RANDOM.VALUE(1, 366)) +
                               (TRUNC(DBMS_RANDOM.VALUE(8, 18)) / 24);
        ELSE
            v_status_consulta := 'CANCELADA';
            v_data_consulta := TRUNC(SYSDATE) + TRUNC(DBMS_RANDOM.VALUE(1, 31)) +
                               (TRUNC(DBMS_RANDOM.VALUE(8, 18)) / 24);
        END IF;

        INSERT INTO TB_GS_CONSULTA
            (id_consulta, id_fila, id_usuario, id_medico, id_unidade,
             status_consulta, data_hora_consulta)
        VALUES
            (NULL, v_id_fila, v_id_usuario, v_id_medico, v_id_unidade,
             v_status_consulta, v_data_consulta);

        -- 20% dos pacientes recebem um agendamento futuro para a mesma fila/especialidade.
        -- O horario e compartilhado somente entre medicos diferentes.
        IF i <= v_total_agendados THEN
            v_indice_medico := CASE
                                   WHEN i > v_especialidades.COUNT THEN 2
                                   ELSE 1
                               END;

            SELECT id_medico
            INTO v_id_medico
            FROM (
                SELECT m.id_medico,
                       ROW_NUMBER() OVER (ORDER BY m.id_medico) AS ordem
                FROM TB_GS_MEDICO m
                JOIN TB_GS_MEDICO_ESPECIALIDADE me
                  ON me.id_medico = m.id_medico
                JOIN TB_GS_MEDICO_UNIDADE mu
                  ON mu.id_medico = m.id_medico
                WHERE m.status_medico = 'ATIVO'
                  AND me.id_especialidade = v_id_especialidade
                  AND mu.id_unidade = v_id_unidade
            )
            WHERE ordem = v_indice_medico;

            IF i > 1 AND i <= v_total_confirmados + 1 THEN
                v_status_futuro := 'CONFIRMADA';
            ELSE
                v_status_futuro := 'AGENDADA';
            END IF;

            INSERT INTO TB_GS_CONSULTA
                (id_consulta, id_fila, id_usuario, id_medico, id_unidade,
                 status_consulta, data_hora_consulta)
            VALUES
                (NULL, v_id_fila, v_id_usuario, v_id_medico, v_id_unidade,
                 v_status_futuro, v_data_hora_agendada);

            UPDATE TB_GS_FILA
            SET status_fila = 'AGENDADA',
                data_saida = SYSDATE
            WHERE id_fila = v_id_fila;

        -- 10% das filas restantes ficam canceladas; as demais continuam aguardando.
        ELSIF i > v_usuarios.COUNT - v_total_cancelados THEN
            UPDATE TB_GS_FILA
            SET status_fila = 'CANCELADA',
                data_saida = SYSDATE
            WHERE id_fila = v_id_fila;
        END IF;
    END LOOP;

    COMMIT;
END;
/

