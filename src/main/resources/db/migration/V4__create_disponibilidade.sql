CREATE TABLE jornadas_trabalho (
    id BIGSERIAL PRIMARY KEY,
    profissional_id BIGINT NOT NULL,
    dia_semana INTEGER NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    CONSTRAINT fk_jornada_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE,
    CONSTRAINT chk_jornada_horario
        CHECK (hora_inicio < hora_fim),
    CONSTRAINT chk_jornada_dia
        CHECK (dia_semana BETWEEN 1 AND 7)
);

CREATE TABLE folgas (
    id BIGSERIAL PRIMARY KEY,
    profissional_id BIGINT NOT NULL,
    data DATE NOT NULL,
    motivo VARCHAR(255),
    CONSTRAINT fk_folga_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE
);

CREATE INDEX idx_jornada_profissional ON jornadas_trabalho(profissional_id);
CREATE INDEX idx_folga_profissional ON folgas(profissional_id);

-- Drop the old tables that are now replaced by proper entities
DROP TABLE IF EXISTS profissional_dias_atendimento;
DROP TABLE IF EXISTS profissional_dias_folga;
