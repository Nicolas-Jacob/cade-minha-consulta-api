package br.com.fiap.cademinhaconsulta.model;

import br.com.fiap.cademinhaconsulta.enums.Credencial;
import br.com.fiap.cademinhaconsulta.enums.Sexo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "TB_GS_USUARIO")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    @SequenceGenerator(name = "usuario_seq", sequenceName = "TB_GS_USUARIO_id_usuario_SEQ", allocationSize = 1)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "nome_usuario", nullable = false, length = 100)
    private String nome;

    @Column(name = "sobrenome_usuario", nullable = false, length = 100)
    private String sobrenome;

    @Column(name = "cpf", nullable = false, length = 11, unique = true)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo_usuario", nullable = false, length = 1)
    private Sexo sexo;

    // Coluna DATE que guarda so a data de nascimento (sem hora)
    @Column(name = "data_nasc", nullable = false)
    private LocalDate dataNascimento;

    // Hash BCrypt
    @Column(name = "senha", nullable = false, length = 100)
    private String senha;

    @Column(name = "login", nullable = false, length = 20)
    private String login;

    @Column(name = "nome_mae", nullable = false, length = 100)
    private String nomeMae;

    @Column(name = "nome_pai", length = 100)
    private String nomePai;

    @Enumerated(EnumType.STRING)
    @Column(name = "credencial", nullable = false, length = 10)
    private Credencial credencial;
}
