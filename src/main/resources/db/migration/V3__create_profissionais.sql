CREATE TABLE profissionais (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    estabelecimento_id BIGINT NOT NULL,
    CONSTRAINT fk_profissionais_estabelecimento
        FOREIGN KEY (estabelecimento_id)
        REFERENCES estabelecimentos(id)
        ON DELETE CASCADE
);

CREATE TABLE profissionais_servicos (
    profissional_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    PRIMARY KEY (profissional_id, servico_id),
    CONSTRAINT fk_ps_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_servico
        FOREIGN KEY (servico_id) REFERENCES servicos(id) ON DELETE CASCADE
);

CREATE TABLE profissional_especialidades (
    profissional_id BIGINT NOT NULL,
    especialidade VARCHAR(100) NOT NULL,
    CONSTRAINT fk_pe_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE
);

CREATE TABLE profissional_dias_atendimento (
    profissional_id BIGINT NOT NULL,
    dia_semana INTEGER NOT NULL,
    CONSTRAINT fk_pda_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE
);

CREATE TABLE profissional_dias_folga (
    profissional_id BIGINT NOT NULL,
    dia_folga VARCHAR(10) NOT NULL,
    CONSTRAINT fk_pdf_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE CASCADE
);

CREATE INDEX idx_profissionais_estabelecimento ON profissionais(estabelecimento_id);
