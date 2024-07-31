ALTER TABLE anvendt_grunnbeloep ADD COLUMN opprettet TIMESTAMPTZ;
UPDATE anvendt_grunnbeloep SET opprettet=oppdatert;
ALTER TABLE anvendt_grunnbeloep ALTER COLUMN opprettet SET DEFAULT now();
ALTER TABLE anvendt_grunnbeloep ALTER COLUMN opprettet SET NOT NULL;