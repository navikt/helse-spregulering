import no.nav.helse.rapids_rivers.*
import java.time.LocalDate

class KjørGrunnbeløpsreguleringRiver(rapidsConnection: RapidsConnection, private val anvendtGrunnbeløpDao: AnvendtGrunnbeløpDao): River.PacketListener {

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

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val grunnbeløpGjelderFra = packet["grunnbeløpGjelderFra"].asLocalDate()
        val grunnbeløpGjelderTil = packet["grunnbeløpGjelderTil"].takeUnless { it.isMissingOrNull() }?.asLocalDate() ?: LocalDate.MAX

        val periode = Periode(grunnbeløpGjelderFra, grunnbeløpGjelderTil)
        val rikitgSeksG = SeksG.fraGrunnbeløp(packet["riktigGrunnbeløp"].asDouble())

        Grunnbeløpsregulering(anvendtGrunnbeløpDao, context, packet).leggTil(periode, rikitgSeksG).regulér()
    }
}