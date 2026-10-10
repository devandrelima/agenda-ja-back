package com.agendaja.backend.controller;

import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.service.EstabelecimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estabelecimentos")
@RequiredArgsConstructor
public class EstabelecimentoController {

    private final EstabelecimentoService service;

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid EstabelecimentoCadastroDTO dto) {
        service.cadastrarEstabelecimentoEGestor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}