package register.before.production.playfinity.raw

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


internal class BellParameters {

    @SerializedName("token")
    @Expose
    var token: String? = null

    @SerializedName("bellId")
    @Expose
    var bellId: String? = null

    @SerializedName("appId")
    @Expose
    var appId: String? = null

    constructor()

    constructor(bellId: String, appId: String) {
        this.bellId = bellId
        this.appId = appId
    }
}

internal class BellchimeResult {

    @SerializedName("didChime")
    @Expose
    var didChime: Boolean? = null

}