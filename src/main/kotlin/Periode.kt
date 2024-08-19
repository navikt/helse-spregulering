import java.time.LocalDate

class Periode(
    fom: LocalDate,
    tom: LocalDate
): ClosedRange<LocalDate>{
    init { check(tom >= fom) { "$fom til $tom var jo en tøysete periode da."} }

    override val start = fom
    override val endInclusive = tom
    override fun toString() = "$start til $endInclusive"

    private fun overlappendePeriode(other: Periode): Periode? {
        val start = maxOf(this.start, other.start)
        val slutt = minOf(this.endInclusive, other.endInclusive)
        if (start > slutt) return null
        return Periode(start, slutt)
    }

    private fun overlapperMed(other: Periode) = overlappendePeriode(other) != null

    companion object {
        fun Iterable<Periode>.overlapper(): Boolean {
            sortedBy { it.start }.zipWithNext { nåværende, neste ->
                if (nåværende.overlapperMed(neste)) return true
            }
            return false
        }
    }
}