# Spregulering

Lagrer når vedtaksperioder er beregnet og dets skjæringstidspunkt slik at vi er i stand til å reberegne alle perioder som er utbetalt med feil grunnbeløp.
Sparker i gang G-regulering på alle perioder som har lagt feil G til grunn når vi har fått vite om den nye G'en.

## Sånn her G-regulerer man

Å nei, har du blitt valgt ut til å kjøre årets G-regulering? Det pleide å være leit, men nå er det blitt ganske greit: 

1. Gå til spout.intern.nav.no og velg deg malen *Reguler G for alle som trenger*. Fyll ut riktig verdier og send eventet. Nå henter spregulering frem alle personer med feil G og sender ut et G-reguleringsevent til spleis.
2. Sjekk om det er fortsatt er noen som ikke har blitt G-regulert. Det kan du sjekke ved å spørre databasen veldig pent: 
    ```
   select * from anvendt_grunnbeloep 
   where skjaeringstidspunkt >= '<riktig dato for G>'::date and seks_g != <siktig tallverdi for 6G>
   ```
3. Er det noen igjen? Det er ikke nødvendigvis så rart, men det kan være rart. Føler du deg flink kan du ta noen stikkprøver; kanskje de har en tidligere periode som venter på å bli behandlet? 
4. Len deg godt tilbake i stolen og nyt hvor enkelt G-regulering har blitt 

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #område-helse.