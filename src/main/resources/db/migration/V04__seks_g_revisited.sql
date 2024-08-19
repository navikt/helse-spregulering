drop table seks_g;

create view seks_g as select seks_g, min(skjaeringstidspunkt) as tidligste_skjaeringstidspunkt, max(skjaeringstidspunkt) as seneste_skjaeringstidspunkt from anvendt_grunnbeloep group by seks_g;