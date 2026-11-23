package com.petsalud.backendpetsalud;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    private final Usuario usuario = Usuario.builder()
            .id(1L).nombre("Ana").email("ana@example.com").rol(Rol.CLIENTE).build();
    @Mock private UsuarioRepository usuarios;
    @InjectMocks private UsuarioServiceImpl servicio;

    @ParameterizedTest
    @EnumSource(Rol.class)
    void registraCadaRolPermitido(Rol rol) {
        usuario.setRol(rol);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.empty());
        when(usuarios.save(same(usuario))).thenReturn(usuario);
        assertSame(usuario, servicio.registrarUsuario(usuario));
        assertEquals(rol, usuario.getRol());
        verify(usuarios).save(same(usuario));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaNombreInvalido(String nombre) {
        usuario.setNombre(nombre);
        verificarRegistroInvalido("El nombre es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaEmailInvalido(String email) {
        usuario.setEmail(email);
        verificarRegistroInvalido("El email es obligatorio");
    }

    @Test
    void rechazaRolAusente() {
        usuario.setRol(null);
        verificarRegistroInvalido("El rol es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "PROVEEDOR"})
    void rechazaRepresentacionesDeRolAjenoAlEnum(String texto) {
        assertThrows(IllegalArgumentException.class, () -> Rol.valueOf(texto));
    }

    @Test
    void rechazaEmailDuplicado() {
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        assertEquals("El email ya está registrado", assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarUsuario(usuario)).getMessage());
        verify(usuarios, never()).save(any());
    }

    @Test
    void consultaListaYEliminaUsuarios() {
        List<Usuario> lista = List.of(usuario);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(usuarios.findAll()).thenReturn(lista);

        assertSame(usuario, servicio.obtenerUsuarioPorId(1L).orElseThrow());
        assertSame(usuario, servicio.obtenerUsuarioPorEmail(usuario.getEmail()).orElseThrow());
        assertSame(lista, servicio.listarUsuarios());
        servicio.eliminarUsuario(1L);
        verify(usuarios).deleteById(1L);
    }

    @Test
    void actualizaDatosDeContactoConservandoEmailYRol() {
        Usuario cambios = Usuario.builder().nombre("Ana María").apellido("Ríos")
                .telefono("123").direccion("Santiago").email("otro@example.com").rol(Rol.ADMIN).build();
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarios.save(same(usuario))).thenReturn(usuario);

        Usuario resultado = servicio.actualizarUsuario(1L, cambios);

        assertAll(
                () -> assertEquals("Ana María", resultado.getNombre()),
                () -> assertEquals("Ríos", resultado.getApellido()),
                () -> assertEquals("123", resultado.getTelefono()),
                () -> assertEquals("Santiago", resultado.getDireccion()),
                () -> assertEquals("ana@example.com", resultado.getEmail()),
                () -> assertEquals(Rol.CLIENTE, resultado.getRol()));
        verify(usuarios).save(same(usuario));
    }

    @Test
    void rechazaActualizarUsuarioInexistente() {
        when(usuarios.findById(1L)).thenReturn(Optional.empty());
        assertEquals("Usuario no encontrado", assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarUsuario(1L, usuario)).getMessage());
        verify(usuarios, never()).save(any());
    }

    private void verificarRegistroInvalido(String mensaje) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarUsuario(usuario)).getMessage());
        verifyNoInteractions(usuarios);
    }
}

