package com.agendaja.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClienteCadastroDTOTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void configurarValidador() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void fecharValidador() {
		factory.close();
	}

	private ClienteCadastroDTO criarDto(
			String nome,
			String telefone,
			String email,
			String senha
	) {
		return new ClienteCadastroDTO(
				nome,
				telefone,
				email,
				senha
		);
	}

	private boolean contemErroNoCampo(
			Set<ConstraintViolation<ClienteCadastroDTO>> erros,
			String campo
	) {
		return erros.stream().anyMatch(
				erro -> erro.getPropertyPath()
						.toString()
						.equals(campo)
		);
	}

	@Test
	void deveAceitarCadastroValido() {

		var dto = criarDto(
				"Maria Silva",
				"84999999999",
				"maria@email.com",
				"SenhaSegura123"
		);

		assertTrue(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarNomeVazio() {

		var dto = criarDto(
				"   ",
				"84999999999",
				"maria@email.com",
				"SenhaSegura123"
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "nome")
		);
	}

	@Test
	void deveRejeitarTelefoneVazio() {

		var dto = criarDto(
				"Maria Silva",
				"",
				"maria@email.com",
				"SenhaSegura123"
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "telefone")
		);
	}

	@Test
	void deveRejeitarTelefoneComQuantidadeInvalidaDeDigitos() {

		var dto = criarDto(
				"Maria Silva",
				"12345",
				"maria@email.com",
				"SenhaSegura123"
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "telefone")
		);
	}

	@Test
	void deveAceitarTelefoneFormatado() {

		var dto = criarDto(
				"Maria Silva",
				"(84) 99999-9999",
				"maria@email.com",
				"SenhaSegura123"
		);

		assertEquals("84999999999", dto.telefone());
		assertTrue(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarEmailInvalido() {

		var dto = criarDto(
				"Maria Silva",
				"84999999999",
				"email-invalido",
				"SenhaSegura123"
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "email")
		);
	}

	@Test
	void deveRejeitarEmailVazio() {

		var dto = criarDto(
				"Maria Silva",
				"84999999999",
				" ",
				"SenhaSegura123"
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "email")
		);
	}

	@Test
	void deveRejeitarSenhaCurta() {

		var dto = criarDto(
				"Maria Silva",
				"84999999999",
				"maria@email.com",
				"123"
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "senha")
		);
	}

	@Test
	void deveRejeitarSenhaVazia() {

		var dto = criarDto(
				"Maria Silva",
				"84999999999",
				"maria@email.com",
				""
		);

		assertTrue(
				contemErroNoCampo(validator.validate(dto), "senha")
		);
	}

	@Test
	void deveNormalizarNomeTelefoneEEmail() {

		var dto = criarDto(
				"  Maria Silva  ",
				"(84) 99999-9999",
				"  MARIA@EMAIL.COM  ",
				"SenhaSegura123"
		);

		assertEquals("Maria Silva", dto.nome());
		assertEquals("84999999999", dto.telefone());
		assertEquals("maria@email.com", dto.email());

		assertTrue(validator.validate(dto).isEmpty());
	}

	@Test
	void naoDeveModificarSenhaDuranteNormalizacao() {

		var dto = criarDto(
				"Maria Silva",
				"84999999999",
				"maria@email.com",
				"  SenhaSegura123  "
		);

		assertEquals("  SenhaSegura123  ", dto.senha());
	}
}
