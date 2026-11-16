package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.enums.StatusFila;
import br.com.fiap.cademinhaconsulta.model.Fila;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface FilaRepository extends JpaRepository<Fila, Long> {

    @EntityGraph(attributePaths = {"especialidade", "unidade"})
    Page<Fila> findByUsuario_Id(Long idUsuario, Pageable pageable);

    // Filtros opcionais do gestor: parametro null = sem filtro
    @EntityGraph(attributePaths = {"especialidade", "unidade"})
    @Query("""
            select f from Fila f
            where (:status is null or f.status = :status)
              and (:idEspecialidade is null or f.especialidade.id = :idEspecialidade)
              and (:idUnidade is null or f.unidade.id = :idUnidade)
            """)
    Page<Fila> buscar(@Param("status") StatusFila status,
                      @Param("idEspecialidade") Long idEspecialidade,
                      @Param("idUnidade") Long idUnidade,
                      Pageable pageable);

    boolean existsByUsuario_IdAndEspecialidade_IdAndStatus(Long idUsuario, Long idEspecialidade, StatusFila status);

    boolean existsByEspecialidade_Id(Long idEspecialidade);

    // Base da posicaoEstimada: filas AGUARDANDO na mesma especialidade e unidade que entraram antes
    @Query("""
            select count(f) from Fila f
            where f.status = br.com.fiap.cademinhaconsulta.enums.StatusFila.AGUARDANDO
              and f.especialidade.id = :idEspecialidade
              and f.unidade.id = :idUnidade
              and f.dataEntrada < :dataEntrada
            """)
    long contarAFrente(@Param("idEspecialidade") Long idEspecialidade,
                       @Param("idUnidade") Long idUnidade,
                       @Param("dataEntrada") LocalDateTime dataEntrada);
}
