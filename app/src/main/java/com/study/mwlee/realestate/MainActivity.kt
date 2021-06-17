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
import com.study.mwlee.realestate.preference.SharedManager
import com.study.mwlee.realestate.room.AptDatabase
import com.study.mwlee.realestate.room.AptEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private val sharedManager: SharedManager by lazy { SharedManager(this) }
    private var naverMap: NaverMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // 지도의 초기 위치 지정 (안먹힘 TODO)
        val options = NaverMapOptions()
            .camera(CameraPosition(LatLng(37.264315, 127.062990), 8.0))

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map) as MapFragment?
            ?: MapFragment.newInstance(options).also {
                fm.beginTransaction().add(R.id.map, it).commit()
            }

        mapFragment.getMapAsync(this)

        if (sharedManager.getLastUpdateDate()
                .isEmpty() || sharedManager.getLastUpdateDate() < "20210615"
        ) {
            // 실거래가 호출
            Log.e("MainActivity", "request getAptTrade")
            // TODO 현재 날짜가 고정임 (3군데 수정 필요 - 프리퍼런스, 레트로핏)
            RetrofitClient.aptService.getAptRent(BuildConfig.KEY_APT, "41117", "202106")
                .enqueue(object : Callback<AptResponse> {
                    override fun onFailure(
                        call: Call<AptResponse>,
                        t: Throwable
                    ) {
                        Log.e("MainActivity", t.toString())
                    }

                    override fun onResponse(
                        call: Call<AptResponse>,
                        response: Response<AptResponse>
                    ) {
                        sharedManager.saveLastUpdateDate("20210615")
                        // DB 에 저장
                        val aptEntityList = ArrayList<AptEntity>()
                        Log.e(
                            "MainActivity",
                            "retrofit context data size= " + response.body()?.body?.items?.item?.size.toString()
                        )
                        response.body()?.body?.items?.item?.forEach {
                            val aptEntity = AptEntity(it)
                            aptEntityList.add(aptEntity)
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            AptDatabase.getInstance(baseContext)
                                ?.getAptDao()
                                ?.insert(aptEntityList)

                            val aptList = AptDatabase.getInstance(baseContext)
                                ?.getAptDao()
                                ?.getAptAllData()

                            Log.e(
                                "MainActivity",
                                "coroutine context db size= " + aptList?.size.toString()
                            )
                        }
                    }
                })
        }

        CoroutineScope(Dispatchers.IO).launch {
            val aptList = AptDatabase.getInstance(baseContext)
                ?.getAptDao()
                ?.getAptAllData()

            Log.e("MainActivity", "coroutine context db size= " + aptList?.size.toString())
        }

        // 주소 -> 좌표 변환
        RetrofitClient.geocodingService.getGeocoding("매탄동 176")
            .enqueue(object : Callback<GeocodingResponse> {
                override fun onFailure(
                    call: Call<GeocodingResponse>,
                    t: Throwable
                ) {
                    Log.e("MainActivity", t.toString())
                }

                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    Log.e(
                        "MainActivity",
                        "x = " + response.body()?.addresses?.get(0)?.x + " y = " + response.body()?.addresses?.get(
                            0
                        )?.y
                    )
                }
            })
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.5666102, 126.9783881))
        naverMap.moveCamera(cameraUpdate)

        val marker = Marker()
        marker.position = LatLng(37.2711804, 127.0397250)
        marker.icon = MarkerIcons.GRAY
        marker.map = naverMap
        marker.setOnClickListener { overlay ->
            val intent = Intent(this, DetailActivity::class.java)
            startActivity(intent)
            // 이벤트 소비, OnMapClick 이벤트는 발생하지 않음
            true
        }

        naverMap.setOnMapClickListener { point, coord ->
            Toast.makeText(
                this, "${coord.latitude}, ${coord.longitude}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}