package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class QuizSticker(
    val quizId: Long,
    val question: String,
    val tallies: List<Tally>,
    var viewerAnswer: Int?,
    val correctAnswer: Int
) : Serializable