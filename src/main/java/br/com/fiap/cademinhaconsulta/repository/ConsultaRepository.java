package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.Consulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// Todas as buscas trazem junto o que o ConsultaResponse usa, para evitar N+1
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    @EntityGraph(attributePaths = {"fila", "fila.especialidade", "medico", "unidade", "usuario"})
    Optional<Consulta> findDetalhadaById(Long id);

    // periodo=futuras: AGENDADA/CONFIRMADA com data futura
    @EntityGraph(attributePaths = {"fila", "fila.especialidade", "medico", "unidade", "usuario"})
    @Query("""
            select c from Consulta c
            where c.usuario.id = :idUsuario
              and c.status in (br.com.fiap.cademinhaconsulta.enums.StatusConsulta.AGENDADA,
                               br.com.fiap.cademinhaconsulta.enums.StatusConsulta.CONFIRMADA)
              and c.dataHora > :agora
            """)
    Page<Consulta> buscarFuturas(@Param("idUsuario") Long idUsuario, @Param("agora") LocalDateTime agora,
                                 Pageable pageable);

    // periodo=historico: o complemento de futuras
    @EntityGraph(attributePaths = {"fila", "fila.especialidade", "medico", "unidade", "usuario"})
    @Query("""
            select c from Consulta c
            where c.usuario.id = :idUsuario
              and (c.status not in (br.com.fiap.cademinhaconsulta.enums.StatusConsulta.AGENDADA,
                                    br.com.fiap.cademinhaconsulta.enums.StatusConsulta.CONFIRMADA)
                   or c.dataHora <= :agora)
            """)
    Page<Consulta> buscarHistorico(@Param("idUsuario") Long idUsuario, @Param("agora") LocalDateTime agora,
                                   Pageable pageable);

    // Proximas consultas de todos os pacientes (GESTOR): AGENDADA/CONFIRMADA entre agora e o limite
    @EntityGraph(attributePaths = {"fila", "fila.especialidade", "medico", "unidade", "usuario"})
    @Query("""
            select c from Consulta c
            where c.status in (br.com.fiap.cademinhaconsulta.enums.StatusConsulta.AGENDADA,
                               br.com.fiap.cademinhaconsulta.enums.StatusConsulta.CONFIRMADA)
              and c.dataHora > :agora
              and c.dataHora <= :limite
            """)
    Page<Consulta> buscarProximas(@Param("agora") LocalDateTime agora, @Param("limite") LocalDateTime limite,
                                  Pageable pageable);
}
