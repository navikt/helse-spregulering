# Spregulering

Lagrer når vedtaksperioder er beregnet og dets skjæringstidspunkt slik at vi er i stand til å reberegne alle perioder som er utbetalt med feil grunnbeløp.
Sparker i gang G-regulering på alle perioder som har lagt feil G til grunn når vi har fått vite om den nye G'en.

## Erfaringer fra 2026
- vi oppdaterte spleis med nytt grunnbeløp og ventet til endringen var ute i produksjon
- vi sendte så eventet `kjør_grunnbeløpsregulering` fra Spout, **og** oppga `riktigGrunnbeløp` og `grunnbeløpGjelderFra`, altså tok vi sikte på `ManuellGrunnbeløpsreguleringRiver`, ref. erfaringer/tiltak fra 2025
- spregulering fant 1013 sykefraværstilfeller som trengte G-regulering.
- 12 av disse ble oppgaver i Speil med egenskapen `Grunnbeløpsregulering`.
- 25 av disse ble ikke umiddelbart regulert. 21 av disse viste seg å være selvstendig næringsdrivende, og regulering av disse var det ikke bygget støtte for. De resterende 4 var arbeidstakere som ventet på tidligere perioder.
- resten gikk automatisk.

### tiltak til 2027
- Dersom vi har implementert nye kategorier, sørg for at det er bygget støtte for å regulere disse før man legger inn nytt grunnbeløp, da slipper man å oppdage det i etterkant.

## Erfaringer fra 2025

- vi oppdaterte spleis med nytt grunnbeløp og ventet til endringen var ute i produksjon
- vi sendte så eventet `kjør_grunnbeløpsregulering` fra Spout, **uten** å oppgi `riktigGrunnbeløp` og `grunnbeløpGjelderFra`, altså tok vi sikte på `AutomatiskGrunnbeløpsreguleringRiver`
- spregulering fant 1007 skjæringstidspunkter som trengte G-regulering, men det tidligste var 2. mai 2025, selv om spregulering 
  hadde flere rader i tabellen `anvendt_grunnbeloep` med skjæringstidspunkt 1. mai 2025 som hadde brukt gammel G.
- spregulering fant også kun perioder opp til 19. mai, selv om det var perioder med nyere skjæringstidspunkt.
- basert på litt synsing i koden ser det ut til at spregulering er **avhengig** av å få inn nye utkast til vedtak med ny G på skjæringstidspunkt 1. mai 2025 før den kan kjøre G-regulering,
  ellers vil den ikke kunne klare å G-regulere alle.

### tiltak til 2026
- en mulig forenkling kan være at spregulering forholder seg til skjæringstidspunkter `31. desember [inneværende år] <= S >= 1.mai [inneværende år]` (og filtrerer bort de som kan ha fått ny G), slik 
  at vi unngår at vi går glipp av noen perioder som burde blitt G-regulert.
- det andre tiltaket kan være at vi **må** sende manuell G-regulering-melding og oppgi riktig periode med `grunnbeløpGjelderFra` og `grunnbeløpGjelderTil`

## Sånn her G-regulerer man

Å nei, har du blitt valgt ut til å kjøre årets G-regulering? Det pleide å være leit, men nå er det blitt ganske greit.
Fordi det skjer automatisk 🥳

Det er allikevel mulig å sparke det igang manuelt om man ikke vil vente på automatikken. 

F.eks. om vi får nytt grunnbeløp for 2025 lagt til i Spleis 20.Mai vil ikke automatikken slå inn før det er kommet et `utkast_til_vedtak` med nytt grunnbeløp & skjæringstidspunkt 1.Mai 2025. Det er stor sannsynlighet for at det skjer i løpet av såpass kort tid at det ikke er verdt å gjøre noe manuelt. Men du kan allikevel gjøre det:

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
