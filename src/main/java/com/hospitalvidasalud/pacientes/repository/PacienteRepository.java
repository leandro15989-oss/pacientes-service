package com.hospitalvidasalud.pacientes.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import com.hospitalvidasalud.pacientes.model.Paciente;
import reactor.core.publisher.Mono;

public interface PacienteRepository
        extends ReactiveCrudRepository<Paciente, Long> {

    Mono<Paciente> findByNumeroDocumento(String numeroDocumento);

    Mono<Boolean> existsByNumeroDocumento(String numeroDocumento);
}