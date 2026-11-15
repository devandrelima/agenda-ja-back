UPDATE estabelecimentos
SET logo_url = CASE nome
    WHEN 'Studio Marina' THEN '/assets/negocios/logo-salao.png'
    WHEN 'Vivian Unhas' THEN '/assets/negocios/logo-unhas.png'
    WHEN 'Barbearia Central' THEN '/assets/negocios/logo-barbearia.png'
    WHEN 'Ateliê Aurora' THEN '/assets/negocios/logo-estetica.png'
    WHEN 'Espaço Belle' THEN '/assets/negocios/logo-salao.png'
    WHEN 'Navalha & Cia' THEN '/assets/negocios/logo-barbearia.png'
    WHEN 'Clínica Sereno' THEN '/assets/negocios/logo-estetica.png'
    WHEN 'Lumi Nails' THEN '/assets/negocios/logo-unhas.png'
    WHEN 'Corte Fino' THEN '/assets/negocios/logo-barbearia.png'
    WHEN 'Essenza Estética' THEN '/assets/negocios/logo-estetica.png'
    ELSE logo_url
END
WHERE nome IN (
    'Studio Marina', 'Vivian Unhas', 'Barbearia Central', 'Ateliê Aurora',
    'Espaço Belle', 'Navalha & Cia', 'Clínica Sereno', 'Lumi Nails',
    'Corte Fino', 'Essenza Estética'
);
