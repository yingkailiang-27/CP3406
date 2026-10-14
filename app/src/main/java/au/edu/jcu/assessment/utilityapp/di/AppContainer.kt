package au.edu.jcu.assessment.utilityapp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import au.edu.jcu.assessment.utilityapp.data.LocalPlannerRepository
import au.edu.jcu.assessment.utilityapp.data.PlannerRepository
import au.edu.jcu.assessment.utilityapp.data.RetrofitWeatherRepository
import au.edu.jcu.assessment.utilityapp.data.WeatherApi
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import au.edu.jcu.assessment.utilityapp.viewmodel.PlannerViewModel
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.plannerDataStore by preferencesDataStore(name = "studyday")

/** Manual constructor injection is sufficient for this small, two-screen application. */
class AppContainer(context: Context) {
    val plannerRepository: PlannerRepository = LocalPlannerRepository(context.plannerDataStore, seedDemo = true)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    private val api = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)
    val weatherRepository: WeatherRepository = RetrofitWeatherRepository(api)
    val viewModelFactory = PlannerViewModel.Factory(plannerRepository, weatherRepository)
}
