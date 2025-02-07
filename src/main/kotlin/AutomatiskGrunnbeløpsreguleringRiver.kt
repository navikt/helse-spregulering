import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class AutomatiskGrunnbeløpsreguleringRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireAny("@event_name", listOf("midnatt", "kjør_grunnbeløpsregulering"))
                it.forbid("riktigGrunnbeløp", "grunnbeløpGjelderFra")
            }
            validate {
                it.requireKey("system_participating_services")
            }
        }.register(this)
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val grunnbeløpsregulering = Grunnbeløpsregulering(anvendtGrunnbeløpDao, context, packet)

        anvendtGrunnbeløpDao.perioderMedForskjelligGrunnbeløp().forEach { (periode, antattRiktigSeksG) ->
            grunnbeløpsregulering.leggTil(periode, antattRiktigSeksG)
        }

        grunnbeløpsregulering.regulér()
    }
}
