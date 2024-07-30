CREATE TABLE anvendt_grunnbeloep(
    aktor_id            VARCHAR NOT NULL,
    personidentifikator VARCHAR NOT NULL,
    skjaeringstidspunkt DATE NOT NULL,
    seks_g              DECIMAL NOT NULL,
    oppdatert           TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE anvendt_grunnbeloep ADD CONSTRAINT unikt_sykefravaerstilfelle UNIQUE(personidentifikator, skjaeringstidspunkt);
CREATE INDEX idx_skjaeringstidspunkt ON anvendt_grunnbeloep (skjaeringstidspunkt);
CREATE INDEX idx_seks_g ON anvendt_grunnbeloep (seks_g);