import no.nav.helse.rapids_rivers.*
import org.slf4j.event.Level
import org.slf4j.event.Level.ERROR
import org.slf4j.event.Level.INFO

class AutomatiskGrunnbeløpsreguleringRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny("@event_name", listOf("hel_time", "kjør_grunnbeløpsregulering"))
                it.rejectKey("riktigGrunnbeløp", "grunnbeløpGjelderFra")
                it.requireKey("system_participating_services")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (anvendtGrunnbeløpDao.erDetNoenSykefraværstilfellerMedFeilGrunnbeløp()) {
            return context.sendPåSlack(packet, ERROR, "Det er identifisert sykefraværstilfeller med feil grunnbeløp. Har må noen ta en ørlitten titt.")
        }

        if (packet["@event_name"].asText() == "kjør_grunnbeløpsregulering") {
            return context.sendPåSlack(packet, INFO, "Alle sykefraværstilfeller har rett grunnbeløp. Bare å lene seg tilbake å njuta.")
        }
    }

    private companion object {
        private fun MessageContext.sendPåSlack(packet: JsonMessage, level: Level, melding: String) {
            val slackmelding = JsonMessage.newMessage("slackmelding", mapOf(
                "melding" to "\n\n$melding\n\n - Deres erbødig SPregulering :money:",
                "level" to level.name,
                "system_participating_services" to packet["system_participating_services"]
            )).toJson()

            publish(slackmelding)
        }
    }
}