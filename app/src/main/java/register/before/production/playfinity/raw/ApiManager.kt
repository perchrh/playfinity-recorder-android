package register.before.production.playfinity.raw

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class BellApiManager(private val apiServices: ApiServices) {

    fun chime(bellId: String, appId: String) {
        print("Shall call service with: $bellId")
        val bell = BellParameters(bellId, appId)
        val bellCall = apiServices.chimeBell(bell)
        bellCall.enqueue(object: Callback<BellchimeResult> {
            override fun onFailure(call: Call<BellchimeResult>?, t: Throwable?) {
                print("Failed to call service: ${t.toString()}")
            }

            override fun onResponse(call: Call<BellchimeResult>?, response: Response<BellchimeResult>?) {
                print("service was called: ${response.toString()}")
            }
        })
    }

}