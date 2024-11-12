import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.rapids_rivers.*
import java.time.LocalDate

class ManuellGrunnbeløpsreguleringRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "kjør_grunnbeløpsregulering")
                it.require("grunnbeløpGjelderFra") { grunnbeløpGjelderFra -> grunnbeløpGjelderFra.asLocalDate() }
                it.require("riktigGrunnbeløp") { riktigGrunnbeløp -> SeksG.fraGrunnbeløp(riktigGrunnbeløp.asDouble()) }
                it.requireKey("system_participating_services")
                it.interestedIn("grunnbeløpGjelderTil") { grunnbeløpGjelderTil ->
                    val fom = it["grunnbeløpGjelderFra"].asLocalDate()
                    val tom = grunnbeløpGjelderTil.asLocalDate()
                    Periode(fom, tom)
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val grunnbeløpGjelderFra = packet["grunnbeløpGjelderFra"].asLocalDate()
        val grunnbeløpGjelderTil = packet["grunnbeløpGjelderTil"].takeUnless { it.isMissingOrNull() }?.asLocalDate() ?: LocalDate.MAX

        val periode = Periode(grunnbeløpGjelderFra, grunnbeløpGjelderTil)
        val rikitgSeksG = SeksG.fraGrunnbeløp(packet["riktigGrunnbeløp"].asDouble())

        Grunnbeløpsregulering(anvendtGrunnbeløpDao, context, packet).leggTil(periode, rikitgSeksG).regulér()
    }
}