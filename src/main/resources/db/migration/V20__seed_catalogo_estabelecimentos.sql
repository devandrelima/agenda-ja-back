DO $$
DECLARE
    nomes TEXT[] := ARRAY['Studio Marina', 'Vivian Unhas', 'Barbearia Central', 'Ateliê Aurora', 'Espaço Belle', 'Navalha & Cia', 'Clínica Sereno', 'Lumi Nails', 'Corte Fino', 'Essenza Estética'];
    categorias TEXT[] := ARRAY['Salão de beleza', 'Manicure e pedicure', 'Barbearia', 'Clínica de estética', 'Salão de beleza', 'Barbearia', 'Clínica de estética', 'Manicure e pedicure', 'Barbearia', 'Clínica de estética'];
    locais TEXT[] := ARRAY['Aldeota, Fortaleza - CE', 'Meireles, Fortaleza - CE', 'Centro, Fortaleza - CE', 'Cocó, Fortaleza - CE', 'Benfica, Fortaleza - CE', 'Fátima, Fortaleza - CE', 'Papicu, Fortaleza - CE', 'Varjota, Fortaleza - CE', 'Joaquim Távora, Fortaleza - CE', 'São João do Tauape, Fortaleza - CE'];
    capas TEXT[] := ARRAY['/assets/negocios/capa-salao.png', '/assets/negocios/capa-unhas.png', '/assets/negocios/capa-barbearia.png', '/assets/negocios/capa-estetica.png', '/assets/negocios/capa-salao.png', '/assets/negocios/capa-barbearia.png', '/assets/negocios/capa-estetica.png', '/assets/negocios/capa-unhas.png', '/assets/negocios/capa-barbearia.png', '/assets/negocios/capa-estetica.png'];
    logos TEXT[] := ARRAY['/assets/negocios/logo-salao.svg', '/assets/negocios/logo-unhas.svg', '/assets/negocios/logo-barbearia.svg', '/assets/negocios/logo-estetica.svg', '/assets/negocios/logo-salao.svg', '/assets/negocios/logo-barbearia.svg', '/assets/negocios/logo-estetica.svg', '/assets/negocios/logo-unhas.svg', '/assets/negocios/logo-barbearia.svg', '/assets/negocios/logo-estetica.svg'];
    i INTEGER;
    estabelecimento_id BIGINT;
    servico_id BIGINT;
    profissional_id BIGINT;
BEGIN
    FOR i IN 1..array_length(nomes, 1) LOOP
        IF NOT EXISTS (SELECT 1 FROM estabelecimentos WHERE nome = nomes[i]) THEN
            INSERT INTO estabelecimentos (nome, categoria, imagem_url, logo_url, localizacao)
            VALUES (nomes[i], categorias[i], capas[i], logos[i], locais[i])
            RETURNING id INTO estabelecimento_id;

            INSERT INTO servicos (nome, duracao_minutos, valor, estabelecimento_id, ativo)
            VALUES ('Atendimento principal', 60, 90.00, estabelecimento_id, TRUE)
            RETURNING id INTO servico_id;

            INSERT INTO profissionais (nome, estabelecimento_id, ativo)
            VALUES ('Profissional responsável', estabelecimento_id, TRUE)
            RETURNING id INTO profissional_id;

            INSERT INTO profissionais_servicos (profissional_id, servico_id)
            VALUES (profissional_id, servico_id);

            INSERT INTO jornadas_trabalho (profissional_id, dia_semana, hora_inicio, hora_fim)
            SELECT profissional_id, dia, '09:00', '18:00'
            FROM generate_series(1, 6) AS dia;
        END IF;
    END LOOP;
END $$;
