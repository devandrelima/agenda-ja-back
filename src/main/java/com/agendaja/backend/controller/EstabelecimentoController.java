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
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/estabelecimentos")
@RequiredArgsConstructor
public class EstabelecimentoController {

    private final EstabelecimentoService service;


    @Operation(
            summary = "Cadastrar estabelecimento e gestor",
            description = """
                Cadastra um estabelecimento e seu gestor responsável.
        
                Endpoint público: não exige autenticação.
        
                Exige proteção CSRF:
                1. Obtenha o token em GET /api/auth/csrf.
                2. Preserve o cookie JSESSIONID recebido.
                3. Envie o token no header X-CSRF-TOKEN.
        
                O campo imagemUrl é opcional.
                O upload de imagens será implementado futuramente.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Estabelecimento e gestor cadastrados"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Token CSRF ausente ou inválido"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado"
            )
    })
    @Parameter(
            name = "X-CSRF-TOKEN",
            description = "Token obtido em GET /api/auth/csrf",
            required = true,
            in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER
    )
    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid EstabelecimentoCadastroDTO dto) {
        service.cadastrarEstabelecimentoEGestor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}