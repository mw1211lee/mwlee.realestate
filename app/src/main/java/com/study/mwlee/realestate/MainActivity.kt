package com.study.mwlee.realestate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Align
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.study.mwlee.realestate.databinding.MainActivityBinding
import com.study.mwlee.realestate.network.AptResponse
import com.study.mwlee.realestate.network.GeocodingResponse
import com.study.mwlee.realestate.network.RetrofitClient
import com.study.mwlee.realestate.preference.PreferenceManager
import com.study.mwlee.realestate.room.AptEntity
import com.study.mwlee.realestate.room.DatabaseHelper
import com.study.mwlee.realestate.room.LocationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private val tag = "MainActivity"

    private lateinit var binding: MainActivityBinding
    private val preferenceManager: PreferenceManager by lazy { PreferenceManager(this) }
    private var aptList: List<AptEntity>? = null
    private var map: NaverMap? = null
    private var requestCount = 0
    private var responseCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 네이버 지도 설정
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map, it).commit()
            }

        mapFragment.getMapAsync(this)

        checkRealEstateData()
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        this.map = naverMap

        /* 시작 위치 변경 */
        CoroutineScope(Dispatchers.Main).launch {
            delay(500L)

            val lastLocation = preferenceManager.getLastLocation()
            if (!(lastLocation.latitude <= 0.0 && lastLocation.longitude <= 0.0)) {
                val cameraUpdate = CameraUpdate.scrollAndZoomTo(lastLocation, 14.0).animate(CameraAnimation.Fly, 3000)
                map?.moveCamera(cameraUpdate)
            }

            /* 마지막 위치 저장 */
            map?.let {
                it.addOnCameraChangeListener { _, _ ->
                    preferenceManager.saveLastLocation(it.cameraPosition.target)
                }
            }
        }
    }

    private fun checkRealEstateData() {
        val calendarInstance = Calendar.getInstance()
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarInstance.time)

        if (preferenceManager.getLastUpdateDate().isEmpty() || preferenceManager.getLastUpdateDate() < currentDate) {
            // 실거래가 호출 (최근 3개월)
            Log.e(tag, "request get apt data")

            // 1~2달전
            for (i in 0..1) {
                calendarInstance.add(Calendar.MONTH, -1)
                val year = calendarInstance.get(Calendar.YEAR)
                val month = calendarInstance.get(Calendar.MONTH) + 1
                val beforeDate = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(calendarInstance.time)

                CoroutineScope(Dispatchers.IO).launch {
                    val count = DatabaseHelper.getInstance(baseContext)?.getAptDao()?.getAptMonthData(year, month, 41117)
                    if (count != null && count == 0) {
                        launch(Dispatchers.Main) {
                            requestCount++
                            RetrofitClient.aptService.getAptTrade(BuildConfig.KEY_APT, "41117", beforeDate).enqueue(aptResponse)
                        }
                    }
                }
            }

            // 이번달
            requestCount++
            RetrofitClient.aptService.getAptTrade(BuildConfig.KEY_APT, "41117", currentDate.substring(0, 6)).enqueue(aptResponse)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                aptList = DatabaseHelper.getInstance(baseContext)?.getAptDao()?.getAptAllData()
                Log.e(tag, "coroutine context apt db size= " + aptList?.size.toString())

                launch(Dispatchers.Main) { checkLocation() }
            }
        }
    }

    private val aptResponse = object : Callback<AptResponse> {
        override fun onFailure(call: Call<AptResponse>, t: Throwable) {
            responseCount++
            Log.e(tag, t.toString())
        }

        override fun onResponse(call: Call<AptResponse>, response: Response<AptResponse>) {
            responseCount++
            // DB 에 저장
            val aptEntityList = ArrayList<AptEntity>()
            response.body()?.body?.items?.item?.forEach {
                val aptEntity = AptEntity(it)
                aptEntityList.add(aptEntity)
            }

            // 이번달 요청일 경우에만 프리퍼런스에 저장한다
            val requestDate = call.request().url.queryParameter("DEAL_YMD")
            val calendarInstance = Calendar.getInstance()
            val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendarInstance.time)
            val simpleCurrentDate = currentDate.substring(0, 6)
            if (requestDate == simpleCurrentDate) {
                preferenceManager.saveLastUpdateDate(currentDate)
            }
            Log.e(tag, "retrofit context response size= " + response.body()?.body?.items?.item?.size.toString() + "(" + requestDate + ")")

            CoroutineScope(Dispatchers.IO).launch {
                DatabaseHelper.getInstance(baseContext)?.getAptDao()?.insert(aptEntityList)

                launch(Dispatchers.Main) {
                    // 마지막 요청 결과가 왔을 경우 다음 로직을 수행한다
                    if (requestCount == responseCount) {
                        checkRealEstateData()
                    }
                }
            }
        }
    }

    private fun checkLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            var totalCount = 0
            var successCount = 0
            var failCount = 0

            // 주소 -> 좌표 변환
            val addressList = aptList?.map { it.dongPlusJibun }?.distinct()
            val addressDBList = addressList?.let {
                DatabaseHelper.getInstance(baseContext)?.getLocationDao()?.getLocationListData(it)
            }

            if (addressList?.size!! > addressDBList?.size!!) {
                Log.e(tag, "request get geocoding data")
                val mapDBList = addressDBList.map { it.address }

                addressList.forEach {
                    val index = mapDBList.indexOf(it)
                    if (index < 0) {
                        totalCount++
                        RetrofitClient.geocodingService.getGeocoding(it).enqueue(object : Callback<GeocodingResponse> {
                            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                                failCount++
                                Log.e(tag, t.toString())

                                if (totalCount == successCount + failCount) {
                                    drawMap()
                                }
                            }

                            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                                successCount++
                                CoroutineScope(Dispatchers.IO).launch {
                                    DatabaseHelper.getInstance(baseContext)?.getLocationDao()?.insert(
                                        LocationEntity(it, response.body()?.addresses?.get(0)?.y, response.body()?.addresses?.get(0)?.x)
                                    )
                                }

                                if (totalCount == successCount + failCount) {
                                    drawMap()
                                }
                            }
                        })
                    }
                }
            }

            if (totalCount == 0) {
                launch(Dispatchers.Main) { drawMap() }
            }
        }
    }

    @UiThread
    private fun drawMap() {
        CoroutineScope(Dispatchers.IO).launch {
            val addressDBList = DatabaseHelper.getInstance(baseContext)?.getLocationDao()?.getLocationAllData()
            Log.e(tag, "coroutine context location db size= " + addressDBList?.size)

            launch(Dispatchers.Main) {
                addressDBList?.forEach {
                    val marker = Marker()
                    marker.position = LatLng(it.latitude!!.toDouble(), it.longitude!!.toDouble())
                    marker.icon = OverlayImage.fromResource(R.drawable.outline_home_black) // MarkerIcons.GRAY
                    marker.iconTintColor = getColor(R.color.main_home_icon)
                    marker.map = map
                    marker.width = marker.icon.getBitmap(this@MainActivity)?.width?.times(1.6f)?.toInt() ?: Marker.SIZE_AUTO
                    marker.height = marker.icon.getBitmap(this@MainActivity)?.height?.times(1.6f)?.toInt() ?: Marker.SIZE_AUTO
                    marker.setCaptionAligns(Align.Center)
                    marker.captionText = aptList?.find { aptIt -> aptIt.dongPlusJibun == it.address }?.apartmentName ?: it.address
                    marker.captionTextSize = 10f
                    marker.captionColor = getColor(R.color.main_home_title)
                    marker.captionHaloColor = getColor(R.color.main_home_title)
                    val selectItem = aptList?.maxByOrNull { aptIt -> aptIt.dongPlusJibun == it.address }
                    selectItem?.let { aptIt ->
                        val amount: Double = if (aptIt.isTrade) aptIt.dealAmount.replace(",", "").toDouble() else aptIt.deposit.replace(",", "").toDouble()
                        marker.subCaptionText = ((amount / 1000).roundToInt() / 10f).toString() + "억"
                    }
                    marker.subCaptionTextSize = 13f
                    marker.subCaptionColor = getColor(R.color.white)
                    marker.subCaptionHaloColor = getColor(R.color.white)
                    marker.setOnClickListener {
                        val intent = Intent(this@MainActivity, DetailActivity::class.java)
                        intent.putExtra("apartmentName", marker.captionText)
                        startActivity(intent)
                        // 이벤트 소비, OnMapClick 이벤트는 발생하지 않음
                        true
                    }
                }
            }
        }
    }
}