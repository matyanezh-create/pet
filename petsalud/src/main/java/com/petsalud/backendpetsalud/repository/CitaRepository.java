package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    boolean existsByVeterinarioIdAndFechaHoraAndEstadoNot(Long veterinarioId, LocalDateTime fechaHora, String estado);

    List<Cita> findByMascotaId(Long mascotaId);
    List<Cita> findByVeterinarioId(Long veterinarioId);
    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);
}

