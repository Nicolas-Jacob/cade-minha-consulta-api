package br.com.fiap.cademinhaconsulta.model;

import br.com.fiap.cademinhaconsulta.enums.StatusConsulta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_GS_CONSULTA")
@Getter
@Setter
@NoArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consulta_seq")
    @SequenceGenerator(name = "consulta_seq", sequenceName = "TB_GS_CONSULTA_id_consulta_SEQ", allocationSize = 1)
    @Column(name = "id_consulta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_fila", nullable = false)
    private Fila fila;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_medico", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidade", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_consulta", nullable = false, length = 10)
    private StatusConsulta status;

    // Validada pela trigger TRG_GS_CONSULTA_FUTURA (erro -20007)
    @Column(name = "data_hora_consulta", nullable = false)
    private LocalDateTime dataHora;
}
