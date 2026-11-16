package br.com.fiap.cademinhaconsulta.model;

import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TB_GS_UNIDADE")
@Getter
@Setter
@NoArgsConstructor
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unidade_seq")
    @SequenceGenerator(name = "unidade_seq", sequenceName = "TB_GS_UNIDADE_id_unidade_SEQ", allocationSize = 1)
    @Column(name = "id_unidade")
    private Long id;

    @Column(name = "nome_unidade", nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_unidade", nullable = false, length = 10)
    private StatusUnidade status;
}
