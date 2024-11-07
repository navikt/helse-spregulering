import java.time.LocalDate

data class AnvendtGrunnbeløpDto(
    val personidentifikator: String,
    val skjæringstidspunkt: LocalDate,
    val `6G`: SeksG
)