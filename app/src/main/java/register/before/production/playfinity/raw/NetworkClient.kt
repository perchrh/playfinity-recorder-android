package register.before.production.playfinity.raw

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit


internal object NetworkClient {

    private lateinit var apiServices: ApiServices
    private lateinit var retrofit: Retrofit

    fun getApi(apiUrl: String): ApiServices {

        val gson = GsonBuilder()
            .create()

        val okHttpClient = getOkHttpClient()

        retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiServices = retrofit.create(ApiServices::class.java)

        return apiServices
    }

    @Throws(IOException::class)
    fun <T> convertErrorResponse(type: Type, responseBody: ResponseBody?): T? {
        if (responseBody == null) {
            return null
        }

        val converter = retrofit.responseBodyConverter<T>(type, arrayOfNulls<Annotation>(0))

        return converter.convert(responseBody)
    }

    private fun getOkHttpClient(): OkHttpClient {
        val client = OkHttpClient.Builder()
        client.addInterceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url()

            val url = originalHttpUrl.newBuilder()
                .build()

            val builder = chain.request().newBuilder()
                .url(url)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            val request = builder.build()


            chain.proceed(request)
        }

        client.writeTimeout(7, TimeUnit.SECONDS)
        client.readTimeout(7, TimeUnit.SECONDS)

        return client.build()
    }
}