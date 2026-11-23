package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.MascotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MascotaServiceImpl implements MascotaService {

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Mascota registrarMascota(Mascota mascota, Long propietarioId) {
        validarDatosMascota(mascota);
        Usuario propietario = usuarioRepository.findById(propietarioId)
                .orElseThrow(() -> new IllegalArgumentException("Propietario no encontrado"));

        if (propietario.getRol() != Rol.CLIENTE) {
            throw new IllegalArgumentException("El propietario debe tener rol CLIENTE");
        }
        mascota.setPropietario(propietario);
        return mascotaRepository.save(mascota);
    }

    @Override
    public Optional<Mascota> obtenerMascotaPorId(Long id) {
        return mascotaRepository.findById(id);
    }

    @Override
    public Mascota actualizarMascota(Long id, Mascota mascotaActualizada) {
        return mascotaRepository.findById(id).map(mascota -> {
            validarDatosMascota(mascotaActualizada);
            mascota.setNombre(mascotaActualizada.getNombre());
            mascota.setEspecie(mascotaActualizada.getEspecie());
            mascota.setRaza(mascotaActualizada.getRaza());
            mascota.setFechaNacimiento(mascotaActualizada.getFechaNacimiento());
            mascota.setSexo(mascotaActualizada.getSexo());
            return mascotaRepository.save(mascota);
        }).orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
    }

    private void validarDatosMascota(Mascota mascota) {
        if (mascota.getNombre() == null || mascota.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la mascota es obligatorio");
        }
        if (mascota.getEspecie() == null || mascota.getEspecie().isBlank()) {
            throw new IllegalArgumentException("La especie de la mascota es obligatoria");
        }
    }

    @Override
    public void eliminarMascota(Long id) {
        mascotaRepository.deleteById(id);
    }

    @Override
    public List<Mascota> listarMascotasPorPropietario(Long propietarioId) {
        return mascotaRepository.findByPropietarioId(propietarioId);
    }
}
