package zw.co.byrosolutions.landmarkguide.retrofit

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import zw.co.byrosolutions.landmarkguide.models.Landmark

interface APIInterface {

    // sign-up end-point
    @Headers("Content-Type:application/json")
    @POST("auth/user_sign_up.php")
    fun sign_up(@Body body: String): Call<ResponseBody?>?

    // login end-point
    @Headers("Content-Type:application/json")
    @POST("auth/user_login.php")
    fun login(@Body body: String): Call<ResponseBody?>?

    // save settings
    @Headers("Content-Type:application/json")
    @POST("user/save_settings.php")
    fun save_settings(@Body body: String): Call<ResponseBody?>?

    // get list of landmarks
    @GET("user/get_features.php")
    fun getLandmarks(): Call<ResponseBody?>?

    // save favorite
    @Headers("Content-Type:application/json")
    @POST("user/save_favorite.php")
    fun saveFavorite(@Body body: String): Call<ResponseBody?>?
}