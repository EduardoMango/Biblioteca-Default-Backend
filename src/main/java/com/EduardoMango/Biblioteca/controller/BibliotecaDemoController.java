package com.EduardoMango.Biblioteca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BibliotecaDemoController {

    @GetMapping("/libros/publico/catalogo")
    public ResponseEntity<Map<String, Object>> getCatalogoPublico() {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Catálogo público accesible para cualquier usuario o visitante anónimo",
                "libros", List.of(
                        Map.of("id", 1, "titulo", "El Aleph", "autor", "Jorge Luis Borges", "disponible", true),
                        Map.of("id", 2, "titulo", "Cien Años de Soledad", "autor", "Gabriel García Márquez", "disponible", true),
                        Map.of("id", 3, "titulo", "Rayuela", "autor", "Julio Cortázar", "disponible", false)
                )
        ));
    }

    @GetMapping("/socio/mis-prestamos")
    @PreAuthorize("hasAnyRole('SOCIO', 'BIBLIOTECARIO')")
    public ResponseEntity<Map<String, Object>> getMisPrestamos(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Listado de préstamos del socio",
                "usuario", authentication.getName(),
                "prestamos", List.of(
                        Map.of("idPrestamo", 101, "libro", "El Aleph", "fechaPrestamo", "2026-08-15", "estado", "ACTIVO")
                )
        ));
    }

    @PostMapping("/prestamos/solicitar")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<Map<String, Object>> solicitarPrestamo(
            @RequestParam(defaultValue = "1") Long libroId,
            Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Solicitud de préstamo enviada exitosamente para revisión",
                "solicitante", authentication.getName(),
                "libroId", libroId,
                "estado", "PENDIENTE_APROBACION"
        ));
    }

    @PostMapping("/prestamos/gestion/aprobar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Map<String, Object>> aprobarPrestamo(
            @RequestParam(defaultValue = "101") Long prestamoId,
            Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Préstamo aprobado satisfactoriamente por el bibliotecario",
                "bibliotecario", authentication.getName(),
                "prestamoId", prestamoId,
                "estado", "APROBADO"
        ));
    }

    @GetMapping("/biblioteca/admin/panel")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Map<String, Object>> getPanelAdmin(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Panel de administración y estadísticas de la biblioteca",
                "administrador", authentication.getName(),
                "totalLibros", 1540,
                "prestamosActivos", 42,
                "sociosRegistrados", 320
        ));
    }
}

