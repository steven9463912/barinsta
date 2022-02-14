package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class QuestionSticker(
    val questionType: String,
    val questionId: Long,
    val question: String
) : Serializable