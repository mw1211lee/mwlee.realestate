package com.study.mwlee.realestate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons
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


class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private val tag = "MainActivity"

    private lateinit var binding: MainActivityBinding
    private val preferenceManager: PreferenceManager by lazy { PreferenceManager(this) }
    private var aptList: List<AptEntity>? = null
    private var map: NaverMap? = null

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
        if (preferenceManager.getLastUpdateDate().isEmpty() || preferenceManager.getLastUpdateDate() < "20210615") {
            // 실거래가 호출
            Log.e(tag, "request get apt data")
            // TODO 현재 날짜가 고정임 (3군데 수정 필요 - 프리퍼런스, 레트로핏)
            RetrofitClient.aptService.getAptTrade(BuildConfig.KEY_APT, "41117", "202106").enqueue(object : Callback<AptResponse> {
                override fun onFailure(call: Call<AptResponse>, t: Throwable) {
                    Log.e(tag, t.toString())
                }

                override fun onResponse(call: Call<AptResponse>, response: Response<AptResponse>) {
                    preferenceManager.saveLastUpdateDate("20210615")
                    Log.e(tag, "retrofit context response size= " + response.body()?.body?.items?.item?.size.toString())

                    // DB 에 저장
                    val aptEntityList = ArrayList<AptEntity>()
                    response.body()?.body?.items?.item?.forEach {
                        val aptEntity = AptEntity(it)
                        aptEntityList.add(aptEntity)
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseHelper.getInstance(baseContext)?.getAptDao()?.insert(aptEntityList)

                        launch(Dispatchers.Main) { checkRealEstateData() }
                    }
                }
            })
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                aptList = DatabaseHelper.getInstance(baseContext)?.getAptDao()?.getAptAllData()
                Log.e(tag, "coroutine context apt db size= " + aptList?.size.toString())

                launch(Dispatchers.Main) { checkLocation() }
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
                    marker.icon = MarkerIcons.GRAY
                    val selectItem = aptList?.maxByOrNull { aptIt -> aptIt.dongPlusJibun == it.address }
                    selectItem?.let { aptIt ->
                        marker.captionText = if (aptIt.isTrade) aptIt.dealAmount else aptIt.deposit
                    }
                    marker.map = map
                    marker.subCaptionText = aptList?.find { aptIt -> aptIt.dongPlusJibun == it.address }?.apartmentName ?: it.address
                    marker.setOnClickListener {
                        val intent = Intent(this@MainActivity, DetailActivity::class.java)
                        startActivity(intent)
                        // 이벤트 소비, OnMapClick 이벤트는 발생하지 않음
                        true
                    }
                }
            }
        }
    }
}