package com.agendaja.backend.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveTratarEmailDuplicadoRetornando409() {
        var ex = new EmailJaCadastradoException("E-mail já cadastrado no sistema.");

        var response = handler.tratarEmailDuplicado(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("E-mail já cadastrado no sistema.", response.getBody().get("mensagem"));
    }

    @Test
    void deveTratarErroDeValidacaoMantendoPrimeiroErroDeCadaCampo() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "email", "O e-mail é obrigatório"));
        bindingResult.addError(new FieldError("dto", "email", "Formato de e-mail inválido"));
        bindingResult.addError(new FieldError("dto", "nome", "O nome é obrigatório"));

        var ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var response = handler.tratarValidacao(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Dados inválidos.", response.getBody().get("mensagem"));

        @SuppressWarnings("unchecked")
        var erros = (Map<String, String>) response.getBody().get("erros");
        assertEquals("O e-mail é obrigatório", erros.get("email"));
        assertEquals("O nome é obrigatório", erros.get("nome"));
    }

    @Test
    void deveRetornar409QuandoViolacaoForDeAvaliacaoDuplicada() {
        var constraintEx = new ConstraintViolationException(
                "avaliação duplicada",
                new SQLException("duplicate key"),
                "uk_avaliacoes_cliente_estabelecimento"
        );
        var dataEx = new DataIntegrityViolationException("Erro de integridade", constraintEx);

        var response = handler.tratarViolacaoIntegridade(dataEx);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Você já avaliou este estabelecimento.", response.getBody().get("mensagem"));
    }

    @Test
    void deveRelancarExcecaoQuandoConstraintViolationForDeOutraRestricao() {
        var constraintEx = new ConstraintViolationException(
                "outra constraint",
                new SQLException("duplicate key"),
                "uk_usuarios_email"
        );
        var dataEx = new DataIntegrityViolationException("Erro de integridade", constraintEx);

        var lancada = assertThrows(
                DataIntegrityViolationException.class,
                () -> handler.tratarViolacaoIntegridade(dataEx)
        );
        assertSame(dataEx, lancada);
    }

    @Test
    void deveRelancarExcecaoQuandoNaoHouverConstraintViolationNaCadeiaDeCausas() {
        var dataEx = new DataIntegrityViolationException(
                "Erro genérico de banco",
                new RuntimeException("Causa comum")
        );

        var lancada = assertThrows(
                DataIntegrityViolationException.class,
                () -> handler.tratarViolacaoIntegridade(dataEx)
        );
        assertSame(dataEx, lancada);
    }
}
