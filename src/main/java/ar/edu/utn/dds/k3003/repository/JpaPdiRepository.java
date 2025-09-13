package ar.edu.utn.dds.k3003.repository;

import ar.edu.utn.dds.k3003.model.PdI;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("deploy")
public interface JpaPdiRepository extends JpaRepository<PdI, Long>, PdIRepository {
    List<PdI> findByHechoId(String hechoId);
}
