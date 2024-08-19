
class SeksG(input: Number) {
    val verdi = input.toDouble()
    init {
        require(verdi % 6 == 0.0 && verdi in grense) {
            "${verdi/6} virker jo som et rart grunnbeløp da. 6G er $verdi, virkelig?"
        }
    }

    override fun toString() = "$verdi"
    override fun equals(other: Any?) = other is SeksG && verdi == other.verdi
    override fun hashCode() = verdi.hashCode()

    companion object {
        // Dette er minste 6G vi har registrert, og vil ikke kunne registrere lavere
        private const val Minste = 599148.0
        // Sanity-sjekk på om noen sender ny 6G som om det er 1G, da vil vi bikke maks-verdien.
        private val grense = Minste..<(Minste * 6)

        fun fraGrunnbeløp(grunnbeløp: Double) = SeksG(grunnbeløp * 6)
    }
}