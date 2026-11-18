package com.agendaja.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClienteAtualizacaoDTOTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void finalizar() {
        factory.close();
    }

    @Test
    void deveNormalizarCamposQuandoPreenchidos() {
        var dto = new ClienteAtualizacaoDTO(
                "  Maria Silva  ",
                "(84) 9 9999-8888",
                "  MARIA@EMAIL.COM  ",
                "data:image/jpeg;base64,abc"
        );

        assertEquals("Maria Silva", dto.nome());
        assertEquals("84999998888", dto.telefone());
        assertEquals("maria@email.com", dto.email());
        assertEquals("data:image/jpeg;base64,abc", dto.fotoUrl());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void devePermitirInstanciacaoComCamposNulosERejeitarNaValidacao() {
        var dto = new ClienteAtualizacaoDTO(null, null, null, null);

        assertNull(dto.nome());
        assertNull(dto.telefone());
        assertNull(dto.email());
        assertNull(dto.fotoUrl());
        assertEquals(3, validator.validate(dto).size());
    }
}
