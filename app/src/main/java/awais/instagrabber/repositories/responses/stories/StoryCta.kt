package awais.instagrabber.repositories.responses.stories

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StoryCta(
    @SerializedName("webUri")
    val webUri: String?
) : Serializable