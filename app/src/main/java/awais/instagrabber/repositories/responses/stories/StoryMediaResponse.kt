package awais.instagrabber.repositories.responses.stories

import java.io.Serializable

data class StoryMediaResponse(
    val items: List<StoryMedia?>?, // length 1
    val status: String?
    // ignoring pagination properties
) : Serializable