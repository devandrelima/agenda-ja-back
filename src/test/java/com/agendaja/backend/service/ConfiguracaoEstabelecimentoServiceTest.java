package com.agendaja.backend.service;

import com.agendaja.backend.dto.HorarioFuncionamentoDTO;
import com.agendaja.backend.dto.HorarioFuncionamentoDTO.Turno;
import com.agendaja.backend.model.Estabelecimento;
import com.agendaja.backend.repository.EstabelecimentoRepository;
import com.agendaja.backend.security.EstabelecimentoAuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoEstabelecimentoServiceTest {
    private static final Long ESTABELECIMENTO_ID = 1L;
    private static final String EMAIL = "gestor@teste.com";

    @Mock private EstabelecimentoRepository repository;
    @Mock private EstabelecimentoAuthorizationService authorizationService;

    @InjectMocks private ConfiguracaoEstabelecimentoService service;

    private Estabelecimento estabelecimento;

    @BeforeEach
    void setup() {
        estabelecimento = new Estabelecimento(
                ESTABELECIMENTO_ID, "Salão Original", "SALAO", null, "Centro"
        );
    }

    private List<HorarioFuncionamentoDTO> criarSemanaVazia()
    {
        List<HorarioFuncionamentoDTO> semana = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            semana.add(new HorarioFuncionamentoDTO(List.of()));
        }
        return semana;
    }

    @Test
    void salvarDados_deveAtualizarQuandoDadosValidos() {
        when(repository.findById(ESTABELECIMENTO_ID)).thenReturn(Optional.of(estabelecimento));

        service.salvarDados(ESTABELECIMENTO_ID, EMAIL, "  Novo Nome  ", "  BARBEARIA  ", "  Rua Nova  ");

        assertEquals("Novo Nome", estabelecimento.getNome());
        assertEquals("BARBEARIA", estabelecimento.getCategoria());
        assertEquals("Rua Nova", estabelecimento.getLocalizacao());
        verify(authorizationService).validarAcesso(EMAIL, ESTABELECIMENTO_ID);
    }

    @Test
    void salvarDados_deveRetornar404QuandoEstabelecimentoNaoExiste() {
        when(repository.findById(ESTABELECIMENTO_ID)).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.salvarDados(ESTABELECIMENTO_ID, EMAIL, "Nome", "Cat", "Local"));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void salvarDados_deveRejeitarCamposNulosEmBrancoOuAcimaDoLimite() {
        // nome: null, blank, > 100
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarDados(ESTABELECIMENTO_ID, EMAIL, null, "Cat", "Local")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarDados(ESTABELECIMENTO_ID, EMAIL, "   ", "Cat", "Local")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarDados(ESTABELECIMENTO_ID, EMAIL, "a".repeat(101), "Cat", "Local")).getStatusCode().value());

        // categoria: inválida
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarDados(ESTABELECIMENTO_ID, EMAIL, "Nome", "a".repeat(51), "Local")).getStatusCode().value());

        // localizacao: inválida
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarDados(ESTABELECIMENTO_ID, EMAIL, "Nome", "Cat", "a".repeat(256))).getStatusCode().value());

        verifyNoInteractions(repository);
    }

    @Test
    void salvarCapa_deveAceitarNuloEJpegValidoERejeitarInvalidos() {
        when(repository.findById(ESTABELECIMENTO_ID)).thenReturn(Optional.of(estabelecimento));

        // Nulo (remove capa)
        service.salvarCapa(ESTABELECIMENTO_ID, EMAIL, null);
        assertNull(estabelecimento.getImagemUrl());

        // JPEG válido
        String jpegValido = "data:image/jpeg;base64,dGVzdGU=";
        service.salvarCapa(ESTABELECIMENTO_ID, EMAIL, jpegValido);
        assertEquals(jpegValido, estabelecimento.getImagemUrl());

        // Prefixo inválido
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarCapa(ESTABELECIMENTO_ID, EMAIL, "data:image/png;base64,abc")).getStatusCode().value());

        // Tamanho acima do limite de 3MB
        String jpegGrande = "data:image/jpeg;base64," + "a".repeat(3_000_000);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarCapa(ESTABELECIMENTO_ID, EMAIL, jpegGrande)).getStatusCode().value());
    }

    @Test
    void salvarLogo_devePersistirOUmaLogoValidaERemoverComNulo() {
        when(repository.findById(ESTABELECIMENTO_ID)).thenReturn(Optional.of(estabelecimento));
        String logo = "data:image/jpeg;base64,bG9nbw==";

        service.salvarLogo(ESTABELECIMENTO_ID, EMAIL, logo);
        assertEquals(logo, estabelecimento.getLogoUrl());

        service.salvarLogo(ESTABELECIMENTO_ID, EMAIL, null);
        assertNull(estabelecimento.getLogoUrl());

        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarLogo(ESTABELECIMENTO_ID, EMAIL, "data:image/png;base64,abc")).getStatusCode().value());
    }

    @Test
    void lerHorarios_deveTratarNuloValidoEJsonInvalido() {
        // Nulo retorna 7 dias vazios
        estabelecimento.setHorariosFuncionamento(null);
        var padrao = service.lerHorarios(estabelecimento);
        assertEquals(7, padrao.size());
        assertTrue(padrao.stream().allMatch(d -> d.turnos().isEmpty()));

        // JSON válido
        estabelecimento.setHorariosFuncionamento("[{\"turnos\":[{\"inicio\":\"08:00\",\"fim\":\"12:00\"}]}]");
        var lidos = service.lerHorarios(estabelecimento);
        assertEquals(1, lidos.size());
        assertEquals("08:00", lidos.get(0).turnos().get(0).inicio());

        // JSON inválido lança IllegalStateException
        estabelecimento.setHorariosFuncionamento("{json-invalido");
        assertThrows(IllegalStateException.class, () -> service.lerHorarios(estabelecimento));
    }

    @Test
    void salvarHorarios_deveValidarEstruturaDeSemanaEDias() {
        // lista nula
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, null)).getStatusCode().value());

        // tamanho != 7
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, List.of())).getStatusCode().value());

        // dia nulo
        var semanaComDiaNulo = criarSemanaVazia();
        semanaComDiaNulo.set(0, null);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, semanaComDiaNulo)).getStatusCode().value());

        // turnos nulos
        var semanaComTurnosNulos = criarSemanaVazia();
        semanaComTurnosNulos.set(0, new HorarioFuncionamentoDTO(null));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, semanaComTurnosNulos)).getStatusCode().value());
    }

    @Test
    void salvarHorarios_deveRejeitarTurnosInvalidosOuSobrepostos() {
        // turno nulo
        var turnoNulo = criarSemanaVazia();
        var listaComNulo = new ArrayList<Turno>();
        listaComNulo.add(null);
        turnoNulo.set(0, new HorarioFuncionamentoDTO(listaComNulo));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, turnoNulo)).getStatusCode().value());

        // inicio nulo -> NullPointerException capturada
        var inicioNulo = criarSemanaVazia();
        inicioNulo.set(0, new HorarioFuncionamentoDTO(List.of(new Turno(null, "12:00"))));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, inicioNulo)).getStatusCode().value());

        // formato de inicio inválido
        var inicioFormatoInvalido = criarSemanaVazia();
        inicioFormatoInvalido.set(0, new HorarioFuncionamentoDTO(List.of(new Turno("8:00", "12:00"))));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, inicioFormatoInvalido)).getStatusCode().value());

        // formato de fim inválido
        var fimFormatoInvalido = criarSemanaVazia();
        fimFormatoInvalido.set(0, new HorarioFuncionamentoDTO(List.of(new Turno("08:00", "12:0"))));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, fimFormatoInvalido)).getStatusCode().value());

        // hora inexistente -> DateTimeParseException capturada
        var horaInvalida = criarSemanaVazia();
        horaInvalida.set(0, new HorarioFuncionamentoDTO(List.of(new Turno("25:00", "26:00"))));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, horaInvalida)).getStatusCode().value());

        // inicio >= fim
        var invertido = criarSemanaVazia();
        invertido.set(0, new HorarioFuncionamentoDTO(List.of(new Turno("14:00", "12:00"))));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, invertido)).getStatusCode().value());

        // turnos sobrepostos
        var sobreposto = criarSemanaVazia();
        sobreposto.set(0, new HorarioFuncionamentoDTO(List.of(
                new Turno("08:00", "12:00"),
                new Turno("11:00", "15:00")
        )));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, sobreposto)).getStatusCode().value());
    }

    @Test
    void salvarHorarios_devePersistirMultiplosTurnosValidosELancarErroSeSerializacaoFalhar() throws Exception {
        when(repository.findById(ESTABELECIMENTO_ID)).thenReturn(Optional.of(estabelecimento));

        var semanaValida = criarSemanaVazia();
        semanaValida.set(0, new HorarioFuncionamentoDTO(List.of(
                new Turno("14:00", "18:00"),
                new Turno("08:00", "12:00")
        )));

        service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, semanaValida);
        assertNotNull(estabelecimento.getHorariosFuncionamento());

        // Falha na serialização JSON
        ObjectMapper mapperQuebrado = mock(ObjectMapper.class);
        when(mapperQuebrado.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("erro") {});
        ReflectionTestUtils.setField(service, "objectMapper", mapperQuebrado);

        assertThrows(IllegalStateException.class,
                () -> service.salvarHorarios(ESTABELECIMENTO_ID, EMAIL, semanaValida));
    }
}
