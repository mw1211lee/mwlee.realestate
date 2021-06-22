package com.study.mwlee.realestate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons
import com.study.mwlee.realestate.network.AptResponse
import com.study.mwlee.realestate.network.GeocodingResponse
import com.study.mwlee.realestate.network.RetrofitClient
import com.study.mwlee.realestate.preference.PreferenceManager
import com.study.mwlee.realestate.room.DatabaseHelper
import com.study.mwlee.realestate.room.AptEntity
import com.study.mwlee.realestate.room.LocationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private val tag = "MainActivity"

    private val preferenceManager: PreferenceManager by lazy { PreferenceManager(this) }
    private var aptList: List<AptEntity>? = null
    private var map: NaverMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // 지도의 초기 위치 지정 (안먹힘 TODO)
        val options = NaverMapOptions().camera(CameraPosition(LatLng(37.264315, 127.062990), 8.0))

        // 네이버 지도 설정
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map) as MapFragment?
            ?: MapFragment.newInstance(options).also {
                fm.beginTransaction().add(R.id.map, it).commit()
            }

        mapFragment.getMapAsync(this)

        checkRealEstateData()
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        this.map = naverMap
    }

    private fun checkRealEstateData() {
        if (preferenceManager.getLastUpdateDate().isEmpty() || preferenceManager.getLastUpdateDate() < "20210615") {
            // 실거래가 호출
            Log.e(tag, "request get apt data")
            // TODO 현재 날짜가 고정임 (3군데 수정 필요 - 프리퍼런스, 레트로핏)
            RetrofitClient.aptService.getAptRent(BuildConfig.KEY_APT, "41117", "202106").enqueue(object : Callback<AptResponse> {
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
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.5666102, 126.9783881))
                map?.moveCamera(cameraUpdate)

                addressDBList?.forEach {
                    val marker = Marker()
                    marker.position = LatLng(it.latitude!!.toDouble(), it.longitude!!.toDouble())
                    marker.icon = MarkerIcons.GRAY
                    marker.captionText = it.address
                    marker.map = map
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