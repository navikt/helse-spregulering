import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

class Grunnbeløpsregulering(
    private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao,
    private val context: MessageContext,
    packet: JsonMessage
) {
    private val manueltInitiert = packet["@event_name"].asText() == "kjør_grunnbeløpsregulering"
    private val systemParticipatingServices = packet["system_participating_services"]
    private val skalReguleres = mutableMapOf<Periode, SeksG>()

    fun leggTil(periode: Periode, riktigSeksG: SeksG): Grunnbeløpsregulering {
        check(skalReguleres[periode] == null) { "Hei! dette går ikke an! $periode er allerede lagt til." }
        skalReguleres[periode] = riktigSeksG
        return this
    }

    fun regulér() {
        loggStart()

        val meldingslinjer = mutableListOf<String>()

        skalReguleres.forEach { (periode, riktigSeksG) ->
            val feilanvendteGrunnbeløp = anvendtGrunnbeløpDao.hentFeilanvendteGrunnbeløp(periode, riktigSeksG)

            if (feilanvendteGrunnbeløp.isNotEmpty()) {
                meldingslinjer.add("- Sendt ut grunnbeløpsregulering for ${feilanvendteGrunnbeløp.size} sykefraværstilfeller med skjæringstidspunkt i perioden $periode som ikke hadde 6G $riktigSeksG (grunnbeløp ${riktigSeksG.verdi / 6})")
            }

            feilanvendteGrunnbeløp.forEach { feilanvendtGrunnbeløp ->
                val grunnbeløpsreguleringEvent = feilanvendtGrunnbeløp.toGrunnbeløpsreguleringEvent()
                sikkerlogg.info("Sender grunnbeløpsregulering:\n\t${grunnbeløpsreguleringEvent}")
                context.publish(feilanvendtGrunnbeløp.personidentifikator, grunnbeløpsreguleringEvent)
            }
        }

        val melding = meldingslinjer.melding() ?: return sikkerlogg.info(Gladmelding)

        sikkerlogg.info(melding)
        context.sendPåSlack(melding)
    }

    private fun loggStart() {
        val periodene = if (skalReguleres.keys.isEmpty()) "- men det var ingen perioder å regulere, gitt." else "for periodene ${skalReguleres.keys.joinToString()}"
        if (manueltInitiert) return sikkerlogg.info("Starter manuell grunnbeløpsregulering $periodene")
        sikkerlogg.info("Starter automatisk grunnbeløpsregulering $periodene")
    }

    private fun List<String>.melding(): String? {
        if (isNotEmpty()) return joinToString("\n")
        if (manueltInitiert) return Gladmelding
        return null
    }

    private fun MessageContext.sendPåSlack(melding: String) {
        val slackmelding = JsonMessage.newMessage("slackmelding", mapOf(
            "melding" to "\n\n$melding\n\n - Deres erbødig SPregulering :money:",
            "level" to "INFO",
            "system_participating_services" to systemParticipatingServices
        )).toJson()

        publish(slackmelding)
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private const val Gladmelding = "Alle sykefraværstilfeller har rett grunnbeløp. Bare å lene seg tilbake å njuta."

        private fun AnvendtGrunnbeløpDto.toGrunnbeløpsreguleringEvent() = JsonMessage.newMessage("grunnbeløpsregulering", mapOf(
            "fødselsnummer" to personidentifikator,
            "aktørId" to aktørId,
            "skjæringstidspunkt" to skjæringstidspunkt
        )).toJson()
    }
}