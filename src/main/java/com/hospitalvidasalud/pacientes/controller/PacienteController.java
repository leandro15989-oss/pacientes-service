
package com.hospitalvidasalud.pacientes.controller;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;

import com.hospitalvidasalud.pacientes.model.Paciente;
import com.hospitalvidasalud.pacientes.repository.PacienteRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteRepository pacienteRepository;

    public PacienteController(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    // Listar todos los pacientes
    @GetMapping
    public Flux<Paciente> listar() {
        return pacienteRepository.findAll();
    }

    // Buscar paciente por ID
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Paciente>> buscarPorId(
            @PathVariable Long id) {

        return pacienteRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Buscar paciente por numero de documento
    @GetMapping("/documento/{numeroDocumento}")
    public Mono<ResponseEntity<Paciente>> buscarPorDocumento(
            @PathVariable String numeroDocumento) {

        return pacienteRepository
                .findByNumeroDocumento(numeroDocumento)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Registrar paciente
    @PostMapping
    public Mono<ResponseEntity<?>> registrar(
            @RequestBody Paciente paciente) {

        // Validar campos obligatorios
        if (paciente.getTipoDocumento() == null
                || paciente.getTipoDocumento().isBlank()
                || paciente.getNumeroDocumento() == null
                || paciente.getNumeroDocumento().isBlank()
                || paciente.getNombres() == null
                || paciente.getNombres().isBlank()
                || paciente.getApellidos() == null
                || paciente.getApellidos().isBlank()) {

            return Mono.just(
                    ResponseEntity.badRequest()
                            .body(Map.of(
                                    "mensaje",
                                    "El tipo de documento, numero de documento, nombres y apellidos son obligatorios"
                            ))
            );
        }

        paciente.setId(null);
        paciente.setEstado("ACTIVO");

        return pacienteRepository
                .findByNumeroDocumento(paciente.getNumeroDocumento())
                .<ResponseEntity<?>>map(existente ->
                        ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                        "mensaje",
                                        "Ya existe un paciente con ese numero de documento"
                                ))
                )
                .switchIfEmpty(
                        pacienteRepository.save(paciente)
                                .<ResponseEntity<?>>map(guardado ->
                                        ResponseEntity.status(HttpStatus.CREATED)
                                                .body(guardado)
                                )
                )
                // Manejar documentos duplicados de R2DBC y Spring
                .onErrorResume(error ->
                        error instanceof DuplicateKeyException
                        || error instanceof R2dbcDataIntegrityViolationException
                        ? Mono.just(
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of(
                                                "mensaje",
                                                "El numero de documento ya esta registrado"
                                        ))
                        )
                        : Mono.error(error)
                );
    }

    // Cambiar estado del paciente
    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<?>> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos) {

        String estado = datos.get("estado");

        if (estado == null
                || (!estado.equals("ACTIVO")
                && !estado.equals("INACTIVO"))) {

            return Mono.just(
                    ResponseEntity.badRequest()
                            .body(Map.of(
                                    "mensaje",
                                    "El estado debe ser ACTIVO o INACTIVO"
                            ))
            );
        }

        return pacienteRepository.findById(id)
                .flatMap(paciente -> {
                    paciente.setEstado(estado);
                    return pacienteRepository.save(paciente);
                })
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}