-- Gerado por Oracle SQL Developer Data Modeler 24.3.1.351.0831
--   em:        2026-11-12 23:12:27 BRT
--   site:      Oracle Database 11g
--   tipo:      Oracle Database 11g

DROP TRIGGER TB_GS_AUDITORIA_DML_id_auditor;
DROP TRIGGER TB_GS_CONSULTA_id_consulta_TRG;
DROP TRIGGER TB_GS_CONTATO_id_contato_TRG;
DROP TRIGGER TB_GS_ENDERECO_id_endereco_TRG;
DROP TRIGGER TB_GS_ESPECIALIDADE_id_especia;
DROP TRIGGER TB_GS_FILA_id_fila_TRG;
DROP TRIGGER TB_GS_MEDICO_id_medico_TRG;
DROP TRIGGER TB_GS_UNIDADE_id_unidade_TRG;
DROP TRIGGER TB_GS_USUARIO_id_usuario_TRG;

DROP SEQUENCE TB_GS_AUDITORIA_DML_id_auditor;
DROP SEQUENCE TB_GS_CONSULTA_id_consulta_SEQ;
DROP SEQUENCE TB_GS_CONTATO_id_contato_SEQ;
DROP SEQUENCE TB_GS_ENDERECO_id_endereco_SEQ;
DROP SEQUENCE TB_GS_ESPECIALIDADE_id_especia;
DROP SEQUENCE TB_GS_FILA_id_fila_SEQ;
DROP SEQUENCE TB_GS_MEDICO_id_medico_SEQ;
DROP SEQUENCE TB_GS_UNIDADE_id_unidade_SEQ;
DROP SEQUENCE TB_GS_USUARIO_id_usuario_SEQ;

-- Gerado por Oracle SQL Developer Data Modeler 24.3.1.351.0831
--   em:        2026-11-15 21:05:15 BRT
--   site:      Oracle Database 11g
--   tipo:      Oracle Database 11g



DROP TABLE TB_GS_AUDITORIA_DML CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_CONSULTA CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_CONTATO CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_ENDERECO CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_ESPECIALIDADE CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_FILA CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_MEDICO CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_MEDICO_ESPECIALIDADE CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_MEDICO_UNIDADE CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_UN_CONTATO_ENDERECO CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_UNIDADE CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_US_CONTATO_ENDERECO CASCADE CONSTRAINTS 
;

DROP TABLE TB_GS_USUARIO CASCADE CONSTRAINTS 
;

-- predefined type, no DDL - MDSYS.SDO_GEOMETRY

-- predefined type, no DDL - XMLTYPE

CREATE TABLE TB_GS_AUDITORIA_DML 
    ( 
     id_auditoria       INTEGER  NOT NULL , 
     nome_tabela        VARCHAR2 (50 CHAR) , 
     usuario_banco      VARCHAR2 (100 CHAR) , 
     tipo_operacao      VARCHAR2 (10 CHAR) , 
     data_hora          DATE , 
     valores_anteriores VARCHAR2 (4000 CHAR) , 
     valores_novos      VARCHAR2 (4000 CHAR) 
    ) 
;

ALTER TABLE TB_GS_AUDITORIA_DML 
    ADD CONSTRAINT tb_gs_auditoria_dml PRIMARY KEY ( id_auditoria ) ;

CREATE TABLE TB_GS_CONSULTA 
    ( 
     id_consulta        INTEGER  NOT NULL , 
     id_fila            INTEGER  NOT NULL , 
     id_usuario         INTEGER  NOT NULL , 
     id_medico          INTEGER  NOT NULL , 
     id_unidade         INTEGER  NOT NULL , 
     status_consulta    VARCHAR2 (10 CHAR)  NOT NULL , 
     data_hora_consulta DATE  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_CONSULTA 
    ADD CONSTRAINT tb_gs_consulta_staatus_ck_ 
    CHECK (status_consulta IN ('PRESENTE', 'FALTA', 'AGENDADA', 'CANCELADA', 'CONFIRMADA'))
;
ALTER TABLE TB_GS_CONSULTA 
    ADD CONSTRAINT tb_gs_consulta_pk PRIMARY KEY ( id_consulta ) ;

CREATE TABLE TB_GS_CONTATO 
    ( 
     id_contato       INTEGER  NOT NULL , 
     telefone_contato VARCHAR2 (11 CHAR)  NOT NULL , 
     email            VARCHAR2 (100 CHAR) , 
     obs_contato      VARCHAR2 (10 CHAR) 
    ) 
;

ALTER TABLE TB_GS_CONTATO 
    ADD CONSTRAINT tb_gs_contato_obs_ck 
    CHECK (obs_contato IN ('PROPRIO', 'EMERGENCIA'))
;
ALTER TABLE TB_GS_CONTATO 
    ADD CONSTRAINT tb_gs_contato_pk PRIMARY KEY ( id_contato ) ;

CREATE TABLE TB_GS_ENDERECO 
    ( 
     id_endereco     INTEGER  NOT NULL , 
     logradouro      VARCHAR2 (100 CHAR)  NOT NULL , 
     numero_end      INTEGER  NOT NULL , 
     complemento_end VARCHAR2 (20 CHAR) , 
     CEP             CHAR (8 CHAR)  NOT NULL , 
     bairro_end      VARCHAR2 (100 CHAR)  NOT NULL , 
     cidade_end      VARCHAR2 (50 CHAR)  NOT NULL , 
     UF_end          CHAR (2 CHAR)  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_ENDERECO 
    ADD CONSTRAINT tb_gs_endereco_uf_ck 
    CHECK (UF_end IN ('AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO','MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'))
;
ALTER TABLE TB_GS_ENDERECO 
    ADD CONSTRAINT tb_gs_endereco_pk PRIMARY KEY ( id_endereco ) ;

CREATE TABLE TB_GS_ESPECIALIDADE 
    ( 
     id_especialidade        INTEGER  NOT NULL , 
     nome_especialidade      VARCHAR2 (50 CHAR)  NOT NULL , 
     descricao_especialidade VARCHAR2 (200 CHAR)  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_ESPECIALIDADE 
    ADD CONSTRAINT tb_gs_expecialidade_pk PRIMARY KEY ( id_especialidade ) ;

CREATE TABLE TB_GS_FILA 
    ( 
     id_fila          INTEGER  NOT NULL , 
     id_usuario       INTEGER  NOT NULL , 
     id_unidade       INTEGER  NOT NULL , 
     id_especialidade INTEGER  NOT NULL , 
     prioridade_fila  INTEGER  NOT NULL , 
     data_entrada     DATE  NOT NULL , 
     data_saida       DATE , 
     status_fila      VARCHAR2 (12 CHAR)  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_FILA 
    ADD CONSTRAINT tb_gs_fila_status_ck 
    CHECK (status_fila IN ('AGUARDANDO', 'CANCELADA', 'AGENDADA'))
;
ALTER TABLE TB_GS_FILA 
    ADD CONSTRAINT tg_ggs_fila_pk PRIMARY KEY ( id_fila ) ;

CREATE TABLE TB_GS_MEDICO 
    ( 
     id_medico        INTEGER  NOT NULL , 
     nome_medico      VARCHAR2 (100 CHAR)  NOT NULL , 
     sobrenome_medico VARCHAR2 (100 CHAR)  NOT NULL , 
     crm              CHAR (10 CHAR)  NOT NULL , 
     uf_crm           CHAR (2 CHAR)  NOT NULL , 
     status_medico    VARCHAR2 (7 CHAR)  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_MEDICO 
    ADD CONSTRAINT tb_gs_medico_uf_crm_ck 
    CHECK (uf_crm IN ('AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO','MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'))
;


ALTER TABLE TB_GS_MEDICO 
    ADD CONSTRAINT tb_gs_medico_status_ck 
    CHECK (status_medico IN ('ATIVO', 'INATIVO'))
;
ALTER TABLE TB_GS_MEDICO 
    ADD CONSTRAINT tb_gs_medico_pk PRIMARY KEY ( id_medico ) ;

CREATE TABLE TB_GS_MEDICO_ESPECIALIDADE 
    ( 
     id_medico        INTEGER  NOT NULL , 
     id_especialidade INTEGER  NOT NULL 
    ) 
;

CREATE TABLE TB_GS_MEDICO_UNIDADE 
    ( 
     id_medico  INTEGER  NOT NULL , 
     id_unidade INTEGER  NOT NULL 
    ) 
;

CREATE TABLE TB_GS_UN_CONTATO_ENDERECO 
    ( 
     id_unidade  INTEGER  NOT NULL , 
     id_contato  INTEGER  NOT NULL , 
     id_endereco INTEGER  NOT NULL 
    ) 
;

CREATE TABLE TB_GS_UNIDADE 
    ( 
     id_unidade     INTEGER  NOT NULL , 
     nome_unidade   VARCHAR2 (100 CHAR)  NOT NULL , 
     status_unidade VARCHAR2 (10 CHAR)  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_UNIDADE 
    ADD CONSTRAINT tb_gs_unidade_status_ck 
    CHECK (status_unidade IN ('ATIVO', 'INATIVO'))
;
ALTER TABLE TB_GS_UNIDADE 
    ADD CONSTRAINT tb_gs_unidade_pk PRIMARY KEY ( id_unidade ) ;

CREATE TABLE TB_GS_US_CONTATO_ENDERECO 
    ( 
     id_usuario  INTEGER  NOT NULL , 
     id_contato  INTEGER  NOT NULL , 
     id_endereco INTEGER  NOT NULL 
    ) 
;

CREATE TABLE TB_GS_USUARIO 
    ( 
     id_usuario        INTEGER  NOT NULL , 
     nome_usuario      VARCHAR2 (100 CHAR)  NOT NULL , 
     sobrenome_usuario VARCHAR2 (100 CHAR)  NOT NULL , 
     cpf               CHAR (11 CHAR)  NOT NULL , 
     sexo_usuario      CHAR (1 CHAR)  NOT NULL , 
     data_nasc         DATE  NOT NULL , 
     senha             VARCHAR2 (100 CHAR)  NOT NULL , 
     login             VARCHAR2 (20 CHAR)  NOT NULL , 
     nome_mae          VARCHAR2 (100 CHAR)  NOT NULL , 
     nome_pai          VARCHAR2 (100) , 
     credencial        VARCHAR2 (10 CHAR)  NOT NULL 
    ) 
;

ALTER TABLE TB_GS_USUARIO 
    ADD CONSTRAINT tb_gs_usuario_sexo_ck 
    CHECK (sexo_usuario IN ('F', 'M'))
;


ALTER TABLE TB_GS_USUARIO 
    ADD CONSTRAINT tb_gs_usuario_credencial_ck 
    CHECK (credencial IN ('CIDADAO', 'GESTOR'))
;
ALTER TABLE TB_GS_USUARIO 
    ADD CONSTRAINT tb_gs_usuario_pk PRIMARY KEY ( id_usuario ) ;

ALTER TABLE TB_GS_USUARIO 
    ADD CONSTRAINT tb_gs_usuario_cpf_uc UNIQUE ( cpf ) ;

ALTER TABLE TB_GS_US_CONTATO_ENDERECO 
    ADD CONSTRAINT tb_gs__us_cont_end_id_cont_fk FOREIGN KEY 
    ( 
     id_contato
    ) 
    REFERENCES TB_GS_CONTATO 
    ( 
     id_contato
    ) 
;

ALTER TABLE TB_GS_CONSULTA 
    ADD CONSTRAINT tb_gs_consulta_id_fila_fk FOREIGN KEY 
    ( 
     id_fila
    ) 
    REFERENCES TB_GS_FILA 
    ( 
     id_fila
    ) 
;

ALTER TABLE TB_GS_CONSULTA 
    ADD CONSTRAINT tb_gs_consulta_id_med_fk FOREIGN KEY 
    ( 
     id_medico
    ) 
    REFERENCES TB_GS_MEDICO 
    ( 
     id_medico
    ) 
;

ALTER TABLE TB_GS_CONSULTA 
    ADD CONSTRAINT tb_gs_consulta_id_un_fk FOREIGN KEY 
    ( 
     id_unidade
    ) 
    REFERENCES TB_GS_UNIDADE 
    ( 
     id_unidade
    ) 
;

ALTER TABLE TB_GS_CONSULTA 
    ADD CONSTRAINT tb_gs_consulta_id_usuario_fk FOREIGN KEY 
    ( 
     id_usuario
    ) 
    REFERENCES TB_GS_USUARIO 
    ( 
     id_usuario
    ) 
;

ALTER TABLE TB_GS_FILA 
    ADD CONSTRAINT tb_gs_fila_id_esp_fk FOREIGN KEY 
    ( 
     id_especialidade
    ) 
    REFERENCES TB_GS_ESPECIALIDADE 
    ( 
     id_especialidade
    ) 
;

ALTER TABLE TB_GS_FILA 
    ADD CONSTRAINT tb_gs_fila_id_un_fk FOREIGN KEY 
    ( 
     id_unidade
    ) 
    REFERENCES TB_GS_UNIDADE 
    ( 
     id_unidade
    ) 
;

ALTER TABLE TB_GS_FILA 
    ADD CONSTRAINT tb_gs_fila_id_us_fk FOREIGN KEY 
    ( 
     id_usuario
    ) 
    REFERENCES TB_GS_USUARIO 
    ( 
     id_usuario
    ) 
;

ALTER TABLE TB_GS_MEDICO_ESPECIALIDADE 
    ADD CONSTRAINT tb_gs_med_esp_id_esp_fk FOREIGN KEY 
    ( 
     id_medico
    ) 
    REFERENCES TB_GS_MEDICO 
    ( 
     id_medico
    ) 
;

ALTER TABLE TB_GS_MEDICO_ESPECIALIDADE 
    ADD CONSTRAINT tb_gs_med_esp_id_esp_fkv2 FOREIGN KEY 
    ( 
     id_especialidade
    ) 
    REFERENCES TB_GS_ESPECIALIDADE 
    ( 
     id_especialidade
    ) 
;

ALTER TABLE TB_GS_MEDICO_UNIDADE 
    ADD CONSTRAINT tb_gs_med_un_id_med_fk FOREIGN KEY 
    ( 
     id_medico
    ) 
    REFERENCES TB_GS_MEDICO 
    ( 
     id_medico
    ) 
;

ALTER TABLE TB_GS_MEDICO_UNIDADE 
    ADD CONSTRAINT tb_gs_med_un_id_un_fk FOREIGN KEY 
    ( 
     id_unidade
    ) 
    REFERENCES TB_GS_UNIDADE 
    ( 
     id_unidade
    ) 
;

ALTER TABLE TB_GS_UN_CONTATO_ENDERECO 
    ADD CONSTRAINT tb_gs_un_cont_end_id_cont_fk FOREIGN KEY 
    ( 
     id_contato
    ) 
    REFERENCES TB_GS_CONTATO 
    ( 
     id_contato
    ) 
;

ALTER TABLE TB_GS_UN_CONTATO_ENDERECO 
    ADD CONSTRAINT tb_gs_un_cont_end_id_end_fk FOREIGN KEY 
    ( 
     id_endereco
    ) 
    REFERENCES TB_GS_ENDERECO 
    ( 
     id_endereco
    ) 
;

ALTER TABLE TB_GS_UN_CONTATO_ENDERECO 
    ADD CONSTRAINT tb_gs_un_cont_end_id_un_fk FOREIGN KEY 
    ( 
     id_unidade
    ) 
    REFERENCES TB_GS_UNIDADE 
    ( 
     id_unidade
    ) 
;

ALTER TABLE TB_GS_US_CONTATO_ENDERECO 
    ADD CONSTRAINT tb_gs_us_cont_end_id_end_fk FOREIGN KEY 
    ( 
     id_endereco
    ) 
    REFERENCES TB_GS_ENDERECO 
    ( 
     id_endereco
    ) 
;

ALTER TABLE TB_GS_US_CONTATO_ENDERECO 
    ADD CONSTRAINT tb_gs_us_cont_end_id_us_fk FOREIGN KEY 
    ( 
     id_usuario
    ) 
    REFERENCES TB_GS_USUARIO 
    ( 
     id_usuario
    ) 
;

CREATE SEQUENCE TB_GS_AUDITORIA_DML_id_auditor 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_AUDITORIA_DML_id_auditor 
BEFORE INSERT ON TB_GS_AUDITORIA_DML 
FOR EACH ROW 
WHEN (NEW.id_auditoria IS NULL) 
BEGIN 
    :NEW.id_auditoria := TB_GS_AUDITORIA_DML_id_auditor.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_CONSULTA_id_consulta_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_CONSULTA_id_consulta_TRG 
BEFORE INSERT ON TB_GS_CONSULTA 
FOR EACH ROW 
WHEN (NEW.id_consulta IS NULL) 
BEGIN 
    :NEW.id_consulta := TB_GS_CONSULTA_id_consulta_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_CONTATO_id_contato_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_CONTATO_id_contato_TRG 
BEFORE INSERT ON TB_GS_CONTATO 
FOR EACH ROW 
WHEN (NEW.id_contato IS NULL) 
BEGIN 
    :NEW.id_contato := TB_GS_CONTATO_id_contato_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_ENDERECO_id_endereco_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_ENDERECO_id_endereco_TRG 
BEFORE INSERT ON TB_GS_ENDERECO 
FOR EACH ROW 
WHEN (NEW.id_endereco IS NULL) 
BEGIN 
    :NEW.id_endereco := TB_GS_ENDERECO_id_endereco_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_ESPECIALIDADE_id_especia 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_ESPECIALIDADE_id_especia 
BEFORE INSERT ON TB_GS_ESPECIALIDADE 
FOR EACH ROW 
WHEN (NEW.id_especialidade IS NULL) 
BEGIN 
    :NEW.id_especialidade := TB_GS_ESPECIALIDADE_id_especia.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_FILA_id_fila_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_FILA_id_fila_TRG 
BEFORE INSERT ON TB_GS_FILA 
FOR EACH ROW 
WHEN (NEW.id_fila IS NULL) 
BEGIN 
    :NEW.id_fila := TB_GS_FILA_id_fila_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_MEDICO_id_medico_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_MEDICO_id_medico_TRG 
BEFORE INSERT ON TB_GS_MEDICO 
FOR EACH ROW 
WHEN (NEW.id_medico IS NULL) 
BEGIN 
    :NEW.id_medico := TB_GS_MEDICO_id_medico_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_UNIDADE_id_unidade_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_UNIDADE_id_unidade_TRG 
BEFORE INSERT ON TB_GS_UNIDADE 
FOR EACH ROW 
WHEN (NEW.id_unidade IS NULL) 
BEGIN 
    :NEW.id_unidade := TB_GS_UNIDADE_id_unidade_SEQ.NEXTVAL; 
END;
/

CREATE SEQUENCE TB_GS_USUARIO_id_usuario_SEQ 
START WITH 1 
    NOCACHE 
    ORDER ;

CREATE OR REPLACE TRIGGER TB_GS_USUARIO_id_usuario_TRG 
BEFORE INSERT ON TB_GS_USUARIO 
FOR EACH ROW 
WHEN (NEW.id_usuario IS NULL) 
BEGIN 
    :NEW.id_usuario := TB_GS_USUARIO_id_usuario_SEQ.NEXTVAL; 
END;
/



-- Relatório do Resumo do Oracle SQL Developer Data Modeler: 
-- 
-- CREATE TABLE                            13
-- CREATE INDEX                             0
-- ALTER TABLE                             36
-- CREATE VIEW                              0
-- ALTER VIEW                               0
-- CREATE PACKAGE                           0
-- CREATE PACKAGE BODY                      0
-- CREATE PROCEDURE                         0
-- CREATE FUNCTION                          0
-- CREATE TRIGGER                           9
-- ALTER TRIGGER                            0
-- CREATE COLLECTION TYPE                   0
-- CREATE STRUCTURED TYPE                   0
-- CREATE STRUCTURED TYPE BODY              0
-- CREATE CLUSTER                           0
-- CREATE CONTEXT                           0
-- CREATE DATABASE                          0
-- CREATE DIMENSION                         0
-- CREATE DIRECTORY                         0
-- CREATE DISK GROUP                        0
-- CREATE ROLE                              0
-- CREATE ROLLBACK SEGMENT                  0
-- CREATE SEQUENCE                          9
-- CREATE MATERIALIZED VIEW                 0
-- CREATE MATERIALIZED VIEW LOG             0
-- CREATE SYNONYM                           0
-- CREATE TABLESPACE                        0
-- CREATE USER                              0
-- 
-- DROP TABLESPACE                          0
-- DROP DATABASE                            0
-- 
-- REDACTION POLICY                         0
-- 
-- ORDS DROP SCHEMA                         0
-- ORDS ENABLE SCHEMA                       0
-- ORDS ENABLE OBJECT                       0
-- 
-- ERRORS                                   0
-- WARNINGS                                 0
