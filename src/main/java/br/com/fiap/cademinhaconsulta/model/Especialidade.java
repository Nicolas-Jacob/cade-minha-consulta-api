package br.com.fiap.cademinhaconsulta.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TB_GS_ESPECIALIDADE")
@Getter
@Setter
@NoArgsConstructor
public class Especialidade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "especialidade_seq")
    @SequenceGenerator(name = "especialidade_seq", sequenceName = "TB_GS_ESPECIALIDADE_id_especia", allocationSize = 1)
    @Column(name = "id_especialidade")
    private Long id;

    @Column(name = "nome_especialidade", nullable = false, length = 50)
    private String nome;

    @Column(name = "descricao_especialidade", nullable = false, length = 200)
    private String descricao;
}
