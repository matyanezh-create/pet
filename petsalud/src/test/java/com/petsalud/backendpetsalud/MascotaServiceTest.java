package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.MascotaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {
    private final Usuario cliente = Usuario.builder().id(1L).rol(Rol.CLIENTE).build();
    private final Mascota luna = Mascota.builder().id(2L).nombre("Luna").especie("Canina").build();
    @Mock private MascotaRepository mascotas;
    @Mock private UsuarioRepository usuarios;
    @InjectMocks private MascotaServiceImpl servicio;

    @Test
    void registraYConsultaVariasMascotasDelMismoCliente() {
        Mascota oliver = Mascota.builder().id(3L).nombre("Oliver").especie("Felina").build();
        when(usuarios.findById(1L)).thenReturn(Optional.of(cliente));
        when(mascotas.save(luna)).thenReturn(luna);
        when(mascotas.save(oliver)).thenReturn(oliver);
        when(mascotas.findByPropietarioId(1L)).thenReturn(List.of(luna, oliver));

        assertSame(luna, servicio.registrarMascota(luna, 1L));
        assertSame(oliver, servicio.registrarMascota(oliver, 1L));
        List<Mascota> asociadas = servicio.listarMascotasPorPropietario(1L);

        assertEquals(List.of("Luna", "Oliver"), asociadas.stream().map(Mascota::getNombre).toList());
        assertEquals(List.of("Canina", "Felina"), asociadas.stream().map(Mascota::getEspecie).toList());
        asociadas.forEach(mascota -> assertSame(cliente, mascota.getPropietario()));
        verify(mascotas).save(luna);
        verify(mascotas).save(oliver);
        verify(mascotas).findByPropietarioId(1L);
    }

    @Test
    void rechazaPropietarioInexistente() {
        when(usuarios.findById(1L)).thenReturn(Optional.empty());
        verificarRegistroInvalido("Propietario no encontrado");
    }

    @Test
    void rechazaPropietarioSinRolCliente() {
        cliente.setRol(Rol.ADMIN);
        when(usuarios.findById(1L)).thenReturn(Optional.of(cliente));
        verificarRegistroInvalido("El propietario debe tener rol CLIENTE");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaNombreInvalido(String nombre) {
        luna.setNombre(nombre);
        verificarRegistroInvalido("El nombre de la mascota es obligatorio");
        verifyNoInteractions(usuarios);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaEspecieInvalida(String especie) {
        luna.setEspecie(especie);
        verificarRegistroInvalido("La especie de la mascota es obligatoria");
        verifyNoInteractions(usuarios);
    }

    @Test
    void actualizaDatosDeMascotaYConservaSuPropietario() {
        luna.setPropietario(cliente);
        Mascota cambio = Mascota.builder().nombre("Luna Azul").especie("Canina")
                .raza("Mestiza").sexo("Hembra").fechaNacimiento(LocalDate.of(2021, 4, 5)).build();
        when(mascotas.findById(2L)).thenReturn(Optional.of(luna));
        when(mascotas.save(same(luna))).thenReturn(luna);

        Mascota resultado = servicio.actualizarMascota(2L, cambio);

        assertAll(
                () -> assertEquals(cambio.getNombre(), resultado.getNombre()),
                () -> assertEquals(cambio.getEspecie(), resultado.getEspecie()),
                () -> assertEquals(cambio.getRaza(), resultado.getRaza()),
                () -> assertEquals(cambio.getSexo(), resultado.getSexo()),
                () -> assertEquals(cambio.getFechaNacimiento(), resultado.getFechaNacimiento()),
                () -> assertSame(cliente, resultado.getPropietario()));
        assertSame(resultado, servicio.obtenerMascotaPorId(2L).orElseThrow());
        servicio.eliminarMascota(2L);
        verify(mascotas).deleteById(2L);
    }

    @Test
    void rechazaActualizarMascotaInexistente() {
        when(mascotas.findById(2L)).thenReturn(Optional.empty());
        assertEquals("Mascota no encontrada", assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarMascota(2L, luna)).getMessage());
        verify(mascotas, never()).save(any());
    }

    private void verificarRegistroInvalido(String mensaje) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarMascota(luna, 1L)).getMessage());
        verify(mascotas, never()).save(any());
    }
}

