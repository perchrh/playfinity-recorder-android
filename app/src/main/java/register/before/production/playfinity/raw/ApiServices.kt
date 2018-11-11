package register.before.production

import register.before.production.models.BellParameters
import register.before.production.models.BellchimeResult
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

internal interface ApiServices {

    @POST("doorbell/chime")
    fun chimeBell(@Body body: BellParameters): Call<BellchimeResult>

}
