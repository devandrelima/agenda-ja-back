CREATE TABLE estabelecimentos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    imagem_url VARCHAR(500),
    localizacao VARCHAR(255) NOT NULL
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    estabelecimento_id BIGINT REFERENCES estabelecimentos(id),
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);
