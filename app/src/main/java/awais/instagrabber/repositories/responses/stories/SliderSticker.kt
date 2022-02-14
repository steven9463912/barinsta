package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class SliderSticker(
    val sliderId: Long,
    val question: String,
    val emoji: String?,
    val viewerCanVote: Boolean?,
    val viewerVote: Double?,
    val sliderVoteAverage: Double?,
    val sliderVoteCount: Int?,
) : Serializable