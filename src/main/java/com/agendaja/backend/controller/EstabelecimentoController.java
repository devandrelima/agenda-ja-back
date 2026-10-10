package com.agendaja.backend.controller;

import com.agendaja.backend.dto.EstabelecimentoCadastroDTO;
import com.agendaja.backend.service.EstabelecimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/estabelecimentos")
@RequiredArgsConstructor
public class EstabelecimentoController {

    private final EstabelecimentoService service;


    @Operation(
            summary = "Cadastrar estabelecimento e gestor",
            description = "Cria um estabelecimento e seu gestor responsável."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cadastro realizado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado"
            )
    })
    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid EstabelecimentoCadastroDTO dto) {
        service.cadastrarEstabelecimentoEGestor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}