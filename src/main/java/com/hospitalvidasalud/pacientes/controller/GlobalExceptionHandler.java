
package com.hospitalvidasalud.pacientes.controller;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        DuplicateKeyException.class,
        R2dbcDataIntegrityViolationException.class
    })
    public ResponseEntity<Map<String, String>> manejarDocumentoDuplicado(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "mensaje",
                        "El numero de documento ya esta registrado"
                ));
    }
}
