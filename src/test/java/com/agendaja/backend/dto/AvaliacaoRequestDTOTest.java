package com.agendaja.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoRequestDTOTest {
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
    void deveRemoverEspacosDoComentarioQuandoPreenchido() {
        var dto = new AvaliacaoRequestDTO(5, "  Excelente atendimento!  ");

        assertEquals("Excelente atendimento!", dto.comentario());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void deveManterComentarioNuloNaInstanciacaoERejeitarNaValidacao() {
        var dto = new AvaliacaoRequestDTO(5, null);

        assertNull(dto.comentario());
        assertFalse(validator.validate(dto).isEmpty());
    }
}
