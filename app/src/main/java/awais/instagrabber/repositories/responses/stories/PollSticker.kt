package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class PollSticker(
    val pollId: Long,
    val question: String?,
    val tallies: List<Tally>,
    var viewerVote: Int?
) : Serializable