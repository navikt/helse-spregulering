import no.nav.helse.rapids_rivers.*

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
        val grunnbeløpsregulering = Grunnbeløpsregulering(anvendtGrunnbeløpDao, context, packet)

        anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp().forEach { (periode, antattRiktigSeksG) ->
            grunnbeløpsregulering.leggTil(periode, antattRiktigSeksG)
        }

        grunnbeløpsregulering.regulér()
    }
}