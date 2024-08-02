create table if not exists seks_g
(
    seks_g                        decimal not null primary key,
    tidligste_skjaeringstidspunkt date    not null,
    seneste_skjaeringstidspunkt   date    not null
);