package pl.herfor.android.retrofits

import pl.herfor.android.objects.User
import retrofit2.Call
import retrofit2.http.GET

interface UserRetrofit {
    @GET("users/register")
    fun register(): Call<User>
}