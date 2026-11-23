package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.*;
import com.petsalud.backendpetsalud.repository.*;
import com.petsalud.backendpetsalud.service.CitaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaTest {
    private static final LocalDateTime HORA = LocalDateTime.of(2030, 5, 15, 10, 30);
    private static final String MOTIVO = "Control";
    private final Usuario cliente = Usuario.builder().id(1L).rol(Rol.CLIENTE).build();
    private final Usuario veterinario = Usuario.builder().id(2L).rol(Rol.VETERINARIO).build();
    private final Mascota mascota = Mascota.builder().id(3L).propietario(cliente).build();

    @Mock private CitaRepository citas;
    @Mock private UsuarioRepository usuarios;
    @Mock private MascotaRepository mascotas;
    @InjectMocks private CitaServiceImpl servicio;

    @Test
    void agendaConEstadoInicialYEntidadesRegistradas() {
        prepararHorario();
        when(citas.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cita cita = agendar();

        assertAll(
                () -> assertEquals("Agendada", cita.getEstado()),
                () -> assertSame(mascota, cita.getMascota()),
                () -> assertSame(cliente, cita.getMascota().getPropietario()),
                () -> assertSame(veterinario, cita.getVeterinario()),
                () -> assertEquals(HORA, cita.getFechaHora()),
                () -> assertEquals(MOTIVO, cita.getMotivo()));
        verify(usuarios).findById(1L);
        verify(citas).save(same(cita));
    }

    @Test
    void rechazaMascotaInexistente() {
        when(mascotas.findById(3L)).thenReturn(Optional.empty());
        verificarRechazo("Mascota no encontrada");
    }

    @Test
    void rechazaMascotaSinPropietario() {
        mascota.setPropietario(null);
        when(mascotas.findById(3L)).thenReturn(Optional.of(mascota));
        verificarRechazo("La mascota debe tener un propietario registrado");
    }

    @Test
    void rechazaPropietarioSinIdentificador() {
        cliente.setId(null);
        when(mascotas.findById(3L)).thenReturn(Optional.of(mascota));
        verificarRechazo("La mascota debe tener un propietario registrado");
    }

    @Test
    void rechazaPropietarioInexistente() {
        when(mascotas.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarios.findById(1L)).thenReturn(Optional.empty());
        verificarRechazo("Propietario no encontrado");
    }

    @Test
    void rechazaPropietarioQueNoEsCliente() {
        cliente.setRol(Rol.ADMIN);
        prepararPropietario();
        verificarRechazo("El propietario debe tener rol CLIENTE");
    }

    @Test
    void rechazaVeterinarioInexistente() {
        prepararPropietario();
        when(usuarios.findById(2L)).thenReturn(Optional.empty());
        verificarRechazo("Veterinario no encontrado");
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ADMIN", "CLIENTE"})
    void rechazaAsignadoSinRolVeterinario(Rol rol) {
        veterinario.setRol(rol);
        prepararHorario();
        verificarRechazo("El usuario asignado debe tener rol VETERINARIO");
    }

    @Test
    void rechazaHorarioOcupadoParaElMismoVeterinario() {
        prepararHorario();
        when(citas.existsByVeterinarioIdAndFechaHoraAndEstadoNot(2L, HORA, "Cancelada"))
                .thenReturn(true);
        verificarRechazo("El veterinario ya tiene una cita en ese horario");
    }

    @Test
    void actualizaEstadoYLuegoPermiteConsultarLaCita() {
        Cita cita = Cita.builder().id(5L).estado("Agendada").build();
        when(citas.findById(5L)).thenReturn(Optional.of(cita));
        when(citas.save(same(cita))).thenReturn(cita);

        assertSame(cita, servicio.actualizarEstadoCita(5L, "Completada"));
        assertEquals("Completada", servicio.obtenerCitaPorId(5L).orElseThrow().getEstado());
        verify(citas).save(same(cita));
    }

    @Test
    void rechazaActualizarCitaInexistente() {
        when(citas.findById(5L)).thenReturn(Optional.empty());
        assertEquals("Cita no encontrada", assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoCita(5L, "Completada")).getMessage());
        verify(citas, never()).save(any());
    }

    @Test
    void cancelaCitaPorId() {
        servicio.cancelarCita(5L);
        verify(citas).deleteById(5L);
    }

    @Test
    void consultaCitasConLosFiltrosSolicitados() {
        List<Cita> resultado = List.of(Cita.builder().fechaHora(HORA).build());
        when(citas.findByMascotaId(3L)).thenReturn(resultado);
        when(citas.findByVeterinarioId(2L)).thenReturn(resultado);
        when(citas.findByFechaHoraBetween(HORA.minusHours(1), HORA.plusHours(1))).thenReturn(resultado);
        assertSame(resultado, servicio.listarCitasPorMascota(3L));
        assertSame(resultado, servicio.listarCitasPorVeterinario(2L));
        assertSame(resultado, servicio.listarCitasEntreFechas(HORA.minusHours(1), HORA.plusHours(1)));
    }

    private void prepararPropietario() {
        when(mascotas.findById(3L)).thenReturn(Optional.of(mascota));
        when(usuarios.findById(1L)).thenReturn(Optional.of(cliente));
    }

    private void prepararHorario() {
        prepararPropietario();
        when(usuarios.findById(2L)).thenReturn(Optional.of(veterinario));
    }

    private Cita agendar() {
        return servicio.agendarCita(3L, 2L, HORA, MOTIVO);
    }

    private void verificarRechazo(String mensaje) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, this::agendar).getMessage());
        verify(citas, never()).save(any());
    }
}

