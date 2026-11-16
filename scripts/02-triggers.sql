-- Auditoria explicita das tabelas CitizenTech.
-- Execute depois de recriar o DDL, incluindo os status CONFIRMADA e os campos credencial e data_saida.
-- Cada trigger abaixo monitora somente a tabela indicada. A tabela de auditoria nao e monitorada.

-- A tabela TB_GS_AUDITORIA_DML fica fora da auditoria para evitar recursao.
-- A tabela de pacientes deste modelo e TB_GS_USUARIO.

CREATE OR REPLACE TRIGGER TRG_AUD_CONSULTA
AFTER INSERT OR UPDATE OR DELETE ON TB_GS_CONSULTA
FOR EACH ROW
DECLARE
    v_operacao         VARCHAR2(10);
    v_valores_antigos VARCHAR2(4000);
    v_valores_novos   VARCHAR2(4000);
BEGIN
    IF INSERTING THEN
        v_operacao := 'INSERT';
        v_valores_antigos := NULL;
        v_valores_novos := SUBSTR(
        'id_consulta=' || NVL(TO_CHAR(:NEW.id_consulta), '<NULL>') || ' | ' || 
        'id_fila=' || NVL(TO_CHAR(:NEW.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:NEW.id_usuario), '<NULL>') || ' | ' || 
        'id_medico=' || NVL(TO_CHAR(:NEW.id_medico), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:NEW.id_unidade), '<NULL>') || ' | ' || 
        'status_consulta=' || NVL(TO_CHAR(:NEW.status_consulta), '<NULL>') || ' | ' || 
        'data_hora_consulta=' || NVL(TO_CHAR(:NEW.data_hora_consulta, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>'), 1, 4000);
    ELSIF UPDATING THEN
        v_operacao := 'UPDATE';
        v_valores_antigos := SUBSTR(
        'id_consulta=' || NVL(TO_CHAR(:OLD.id_consulta), '<NULL>') || ' | ' || 
        'id_fila=' || NVL(TO_CHAR(:OLD.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:OLD.id_usuario), '<NULL>') || ' | ' || 
        'id_medico=' || NVL(TO_CHAR(:OLD.id_medico), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:OLD.id_unidade), '<NULL>') || ' | ' || 
        'status_consulta=' || NVL(TO_CHAR(:OLD.status_consulta), '<NULL>') || ' | ' || 
        'data_hora_consulta=' || NVL(TO_CHAR(:OLD.data_hora_consulta, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>'), 1, 4000);
        v_valores_novos := SUBSTR(
        'id_consulta=' || NVL(TO_CHAR(:NEW.id_consulta), '<NULL>') || ' | ' || 
        'id_fila=' || NVL(TO_CHAR(:NEW.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:NEW.id_usuario), '<NULL>') || ' | ' || 
        'id_medico=' || NVL(TO_CHAR(:NEW.id_medico), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:NEW.id_unidade), '<NULL>') || ' | ' || 
        'status_consulta=' || NVL(TO_CHAR(:NEW.status_consulta), '<NULL>') || ' | ' || 
        'data_hora_consulta=' || NVL(TO_CHAR(:NEW.data_hora_consulta, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>'), 1, 4000);
    ELSIF DELETING THEN
        v_operacao := 'DELETE';
        v_valores_antigos := SUBSTR(
        'id_consulta=' || NVL(TO_CHAR(:OLD.id_consulta), '<NULL>') || ' | ' || 
        'id_fila=' || NVL(TO_CHAR(:OLD.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:OLD.id_usuario), '<NULL>') || ' | ' || 
        'id_medico=' || NVL(TO_CHAR(:OLD.id_medico), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:OLD.id_unidade), '<NULL>') || ' | ' || 
        'status_consulta=' || NVL(TO_CHAR(:OLD.status_consulta), '<NULL>') || ' | ' || 
        'data_hora_consulta=' || NVL(TO_CHAR(:OLD.data_hora_consulta, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>'), 1, 4000);
        v_valores_novos := NULL;
    END IF;

    INSERT INTO TB_GS_AUDITORIA_DML
        (nome_tabela, usuario_banco, tipo_operacao, data_hora, valores_anteriores, valores_novos)
    VALUES ('TB_GS_CONSULTA', SYS_CONTEXT('USERENV', 'SESSION_USER'), v_operacao, SYSDATE, v_valores_antigos, v_valores_novos);
END;
/

CREATE OR REPLACE TRIGGER TRG_AUD_FILA
AFTER INSERT OR UPDATE OR DELETE ON TB_GS_FILA
FOR EACH ROW
DECLARE
    v_operacao         VARCHAR2(10);
    v_valores_antigos VARCHAR2(4000);
    v_valores_novos   VARCHAR2(4000);
BEGIN
    IF INSERTING THEN
        v_operacao := 'INSERT';
        v_valores_antigos := NULL;
        v_valores_novos := SUBSTR(
        'id_fila=' || NVL(TO_CHAR(:NEW.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:NEW.id_usuario), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:NEW.id_unidade), '<NULL>') || ' | ' || 
        'id_especialidade=' || NVL(TO_CHAR(:NEW.id_especialidade), '<NULL>') || ' | ' || 
        'prioridade_fila=' || NVL(TO_CHAR(:NEW.prioridade_fila), '<NULL>') || ' | ' || 
        'data_entrada=' || NVL(TO_CHAR(:NEW.data_entrada, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'data_saida=' || NVL(TO_CHAR(:NEW.data_saida, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'status_fila=' || NVL(:NEW.status_fila, '<NULL>'), 1, 4000);
    ELSIF UPDATING THEN
        v_operacao := 'UPDATE';
        v_valores_antigos := SUBSTR(
        'id_fila=' || NVL(TO_CHAR(:OLD.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:OLD.id_usuario), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:OLD.id_unidade), '<NULL>') || ' | ' || 
        'id_especialidade=' || NVL(TO_CHAR(:OLD.id_especialidade), '<NULL>') || ' | ' || 
        'prioridade_fila=' || NVL(TO_CHAR(:OLD.prioridade_fila), '<NULL>') || ' | ' || 
        'data_entrada=' || NVL(TO_CHAR(:OLD.data_entrada, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'data_saida=' || NVL(TO_CHAR(:OLD.data_saida, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'status_fila=' || NVL(:OLD.status_fila, '<NULL>'), 1, 4000);
        v_valores_novos := SUBSTR(
        'id_fila=' || NVL(TO_CHAR(:NEW.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:NEW.id_usuario), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:NEW.id_unidade), '<NULL>') || ' | ' || 
        'id_especialidade=' || NVL(TO_CHAR(:NEW.id_especialidade), '<NULL>') || ' | ' || 
        'prioridade_fila=' || NVL(TO_CHAR(:NEW.prioridade_fila), '<NULL>') || ' | ' || 
        'data_entrada=' || NVL(TO_CHAR(:NEW.data_entrada, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'data_saida=' || NVL(TO_CHAR(:NEW.data_saida, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'status_fila=' || NVL(:NEW.status_fila, '<NULL>'), 1, 4000);
    ELSIF DELETING THEN
        v_operacao := 'DELETE';
        v_valores_antigos := SUBSTR(
        'id_fila=' || NVL(TO_CHAR(:OLD.id_fila), '<NULL>') || ' | ' || 
        'id_usuario=' || NVL(TO_CHAR(:OLD.id_usuario), '<NULL>') || ' | ' || 
        'id_unidade=' || NVL(TO_CHAR(:OLD.id_unidade), '<NULL>') || ' | ' || 
        'id_especialidade=' || NVL(TO_CHAR(:OLD.id_especialidade), '<NULL>') || ' | ' || 
        'prioridade_fila=' || NVL(TO_CHAR(:OLD.prioridade_fila), '<NULL>') || ' | ' || 
        'data_entrada=' || NVL(TO_CHAR(:OLD.data_entrada, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'data_saida=' || NVL(TO_CHAR(:OLD.data_saida, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' ||
        'status_fila=' || NVL(:OLD.status_fila, '<NULL>'), 1, 4000);
        v_valores_novos := NULL;
    END IF;

    INSERT INTO TB_GS_AUDITORIA_DML
        (nome_tabela, usuario_banco, tipo_operacao, data_hora, valores_anteriores, valores_novos)
    VALUES ('TB_GS_FILA', SYS_CONTEXT('USERENV', 'SESSION_USER'), v_operacao, SYSDATE, v_valores_antigos, v_valores_novos);
END;
/

CREATE OR REPLACE TRIGGER TRG_AUD_USUARIO
AFTER INSERT OR UPDATE OR DELETE ON TB_GS_USUARIO
FOR EACH ROW
DECLARE
    v_operacao         VARCHAR2(10);
    v_valores_antigos VARCHAR2(4000);
    v_valores_novos   VARCHAR2(4000);
BEGIN
    IF INSERTING THEN
        v_operacao := 'INSERT';
        v_valores_antigos := NULL;
        v_valores_novos := SUBSTR(
        'id_usuario=' || NVL(TO_CHAR(:NEW.id_usuario), '<NULL>') || ' | ' || 
        'nome_usuario=' || NVL(TO_CHAR(:NEW.nome_usuario), '<NULL>') || ' | ' || 
        'sobrenome_usuario=' || NVL(TO_CHAR(:NEW.sobrenome_usuario), '<NULL>') || ' | ' || 
        'cpf=' || NVL(TO_CHAR(:NEW.cpf), '<NULL>') || ' | ' || 
        'sexo_usuario=' || NVL(TO_CHAR(:NEW.sexo_usuario), '<NULL>') || ' | ' || 
        'data_nasc=' || NVL(TO_CHAR(:NEW.data_nasc, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' || 
        'login=' || NVL(TO_CHAR(:NEW.login), '<NULL>') || ' | ' || 
        'credencial=' || NVL(TO_CHAR(:NEW.credencial), '<NULL>') || ' | ' || 
        'nome_mae=' || NVL(TO_CHAR(:NEW.nome_mae), '<NULL>') || ' | ' || 
        'nome_pai=' || NVL(TO_CHAR(:NEW.nome_pai), '<NULL>'), 1, 4000);
    ELSIF UPDATING THEN
        v_operacao := 'UPDATE';
        v_valores_antigos := SUBSTR(
        'id_usuario=' || NVL(TO_CHAR(:OLD.id_usuario), '<NULL>') || ' | ' || 
        'nome_usuario=' || NVL(TO_CHAR(:OLD.nome_usuario), '<NULL>') || ' | ' || 
        'sobrenome_usuario=' || NVL(TO_CHAR(:OLD.sobrenome_usuario), '<NULL>') || ' | ' || 
        'cpf=' || NVL(TO_CHAR(:OLD.cpf), '<NULL>') || ' | ' || 
        'sexo_usuario=' || NVL(TO_CHAR(:OLD.sexo_usuario), '<NULL>') || ' | ' || 
        'data_nasc=' || NVL(TO_CHAR(:OLD.data_nasc, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' || 
        'login=' || NVL(TO_CHAR(:OLD.login), '<NULL>') || ' | ' || 
        'credencial=' || NVL(TO_CHAR(:OLD.credencial), '<NULL>') || ' | ' || 
        'nome_mae=' || NVL(TO_CHAR(:OLD.nome_mae), '<NULL>') || ' | ' || 
        'nome_pai=' || NVL(TO_CHAR(:OLD.nome_pai), '<NULL>'), 1, 4000);
        v_valores_novos := SUBSTR(
        'id_usuario=' || NVL(TO_CHAR(:NEW.id_usuario), '<NULL>') || ' | ' || 
        'nome_usuario=' || NVL(TO_CHAR(:NEW.nome_usuario), '<NULL>') || ' | ' || 
        'sobrenome_usuario=' || NVL(TO_CHAR(:NEW.sobrenome_usuario), '<NULL>') || ' | ' || 
        'cpf=' || NVL(TO_CHAR(:NEW.cpf), '<NULL>') || ' | ' || 
        'sexo_usuario=' || NVL(TO_CHAR(:NEW.sexo_usuario), '<NULL>') || ' | ' || 
        'data_nasc=' || NVL(TO_CHAR(:NEW.data_nasc, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' || 
        'login=' || NVL(TO_CHAR(:NEW.login), '<NULL>') || ' | ' || 
        'credencial=' || NVL(TO_CHAR(:NEW.credencial), '<NULL>') || ' | ' || 
        'nome_mae=' || NVL(TO_CHAR(:NEW.nome_mae), '<NULL>') || ' | ' || 
        'nome_pai=' || NVL(TO_CHAR(:NEW.nome_pai), '<NULL>'), 1, 4000);
    ELSIF DELETING THEN
        v_operacao := 'DELETE';
        v_valores_antigos := SUBSTR(
        'id_usuario=' || NVL(TO_CHAR(:OLD.id_usuario), '<NULL>') || ' | ' || 
        'nome_usuario=' || NVL(TO_CHAR(:OLD.nome_usuario), '<NULL>') || ' | ' || 
        'sobrenome_usuario=' || NVL(TO_CHAR(:OLD.sobrenome_usuario), '<NULL>') || ' | ' || 
        'cpf=' || NVL(TO_CHAR(:OLD.cpf), '<NULL>') || ' | ' || 
        'sexo_usuario=' || NVL(TO_CHAR(:OLD.sexo_usuario), '<NULL>') || ' | ' || 
        'data_nasc=' || NVL(TO_CHAR(:OLD.data_nasc, 'YYYY-MM-DD HH24:MI:SS'), '<NULL>') || ' | ' || 
        'login=' || NVL(TO_CHAR(:OLD.login), '<NULL>') || ' | ' || 
        'credencial=' || NVL(TO_CHAR(:OLD.credencial), '<NULL>') || ' | ' || 
        'nome_mae=' || NVL(TO_CHAR(:OLD.nome_mae), '<NULL>') || ' | ' || 
        'nome_pai=' || NVL(TO_CHAR(:OLD.nome_pai), '<NULL>'), 1, 4000);
        v_valores_novos := NULL;
    END IF;

    INSERT INTO TB_GS_AUDITORIA_DML
        (nome_tabela, usuario_banco, tipo_operacao, data_hora, valores_anteriores, valores_novos)
    VALUES ('TB_GS_USUARIO', SYS_CONTEXT('USERENV', 'SESSION_USER'), v_operacao, SYSDATE, v_valores_antigos, v_valores_novos);
END;
/


-- Regra de negocio: consultas AGENDADA ou CONFIRMADA, e cancelamentos futuros, precisam manter data futura.
CREATE OR REPLACE TRIGGER TRG_GS_CONSULTA_FUTURA
BEFORE INSERT OR UPDATE ON TB_GS_CONSULTA
FOR EACH ROW
BEGIN
    IF :NEW.status_consulta IN ('AGENDADA', 'CONFIRMADA', 'CANCELADA') AND :NEW.data_hora_consulta <= SYSDATE THEN
        RAISE_APPLICATION_ERROR(-20007, 'A data e hora da consulta agendada, confirmada ou cancelada devem ser futuras.');
    END IF;
END;
/






