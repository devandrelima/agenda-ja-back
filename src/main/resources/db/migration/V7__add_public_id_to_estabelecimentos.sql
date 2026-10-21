ALTER TABLE estabelecimentos
    ADD COLUMN public_id UUID;

UPDATE estabelecimentos
SET public_id = gen_random_uuid()
WHERE public_id IS NULL;

ALTER TABLE estabelecimentos
    ALTER COLUMN public_id SET NOT NULL,
ALTER COLUMN public_id SET DEFAULT gen_random_uuid();

ALTER TABLE estabelecimentos
    ADD CONSTRAINT uk_estabelecimentos_public_id
        UNIQUE (public_id);
