import java.time.LocalDate

data class AnvendtGrunnbeløpDto(
    val aktørId: String,
    val personidentifikator: String,
    val skjæringstidspunkt: LocalDate,
    val `6G`: Double
)