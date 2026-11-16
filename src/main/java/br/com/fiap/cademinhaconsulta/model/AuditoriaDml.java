package br.com.fiap.cademinhaconsulta.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

// Somente leitura: preenchida pelas triggers TRG_AUD_* do banco
@Entity
@Immutable
@Table(name = "TB_GS_AUDITORIA_DML")
@Getter
@NoArgsConstructor
public class AuditoriaDml {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auditoria_seq")
    @SequenceGenerator(name = "auditoria_seq", sequenceName = "TB_GS_AUDITORIA_DML_id_auditor", allocationSize = 1)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "nome_tabela", length = 50)
    private String nomeTabela;

    @Column(name = "usuario_banco", length = 100)
    private String usuarioBanco;

    @Column(name = "tipo_operacao", length = 10)
    private String tipoOperacao;

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @Column(name = "valores_anteriores", length = 4000)
    private String valoresAnteriores;

    @Column(name = "valores_novos", length = 4000)
    private String valoresNovos;
}
