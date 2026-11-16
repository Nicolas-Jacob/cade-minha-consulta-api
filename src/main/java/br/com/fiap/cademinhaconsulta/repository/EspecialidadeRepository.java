package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {
}
