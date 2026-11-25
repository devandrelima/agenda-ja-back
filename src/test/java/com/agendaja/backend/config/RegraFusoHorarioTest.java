package com.agendaja.backend.config;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraFusoHorarioTest {
    // Tipos de java.time sensíveis ao fuso horário da JVM quando chamados com now() sem argumento
    private static final Pattern NOW_SEM_ARGUMENTO = Pattern.compile(
            "\\b(LocalDate|LocalTime|LocalDateTime|ZonedDateTime|OffsetDateTime|OffsetTime|Year|YearMonth|MonthDay)\\.now\\s*\\(\\s*\\)"
    );

    // Evita uso de CURRENT_TIMESTAMP em queries JPQL/HQL (depende do fuso do servidor de banco)
    private static final Pattern CURRENT_TIMESTAMP_NO_CODIGO = Pattern.compile(
            "\\bCURRENT_TIMESTAMP\\b"
    );

    @Test
    void naoDeveUsarNowSemFusoOuCurrentTimestampNoCodigoDeProducao() throws IOException {
        Path raizProducao = Path.of("src/main/java");
        assertTrue(Files.isDirectory(raizProducao), "Diretório src/main/java não encontrado.");

        List<String> violacoes = new ArrayList<>();

        try (Stream<Path> arquivos = Files.walk(raizProducao)) {
            List<Path> arquivosJava = arquivos
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList();

            for (Path arquivo : arquivosJava) {
                List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
                for (int i = 0; i < linhas.size(); i++) {
                    // Remove comentários de linha para evitar falsos positivos
                    String semComentario = linhas.get(i).replaceAll("//.*", "");
                    String caminhoRelativo = raizProducao.relativize(arquivo).toString();

                    if (NOW_SEM_ARGUMENTO.matcher(semComentario).find()) {
                        violacoes.add(caminhoRelativo + ":" + (i + 1)
                                + " -> use Clock ou TimeConfig.ZONA_PADRAO: " + linhas.get(i).trim());
                    }

                    if (CURRENT_TIMESTAMP_NO_CODIGO.matcher(semComentario).find()) {
                        violacoes.add(caminhoRelativo + ":" + (i + 1)
                                + " -> evite CURRENT_TIMESTAMP; passe :agora via Clock: " + linhas.get(i).trim());
                    }
                }
            }
        }

        assertTrue(
                violacoes.isEmpty(),
                "Encontradas chamadas sensíveis ao fuso horário sem zona explícita:\n - "
                        + String.join("\n - ", violacoes)
        );
    }
}
