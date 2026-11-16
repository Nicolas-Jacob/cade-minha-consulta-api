package br.com.fiap.cademinhaconsulta.model;

import br.com.fiap.cademinhaconsulta.enums.StatusFila;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_GS_FILA")
@Getter
@Setter
@NoArgsConstructor
public class Fila {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fila_seq")
    @SequenceGenerator(name = "fila_seq", sequenceName = "TB_GS_FILA_id_fila_SEQ", allocationSize = 1)
    @Column(name = "id_fila")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidade", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidade", nullable = false)
    private Especialidade especialidade;

    // Numero maior = mais prioritario
    @Column(name = "prioridade_fila", nullable = false)
    private Long prioridade;

    @Column(name = "data_entrada", nullable = false)
    private LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_fila", nullable = false, length = 12)
    private StatusFila status;
}
