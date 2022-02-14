package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class Tally(
    val text: String,
    val count: Int
) : Serializable