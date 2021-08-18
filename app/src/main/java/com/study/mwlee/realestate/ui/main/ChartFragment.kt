package com.study.mwlee.realestate.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.study.mwlee.realestate.databinding.ChartFragmentBinding
import com.study.mwlee.realestate.room.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ChartFragment(private val aptName: String) : Fragment() {

    private lateinit var binding: ChartFragmentBinding
    private var selectedArea: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ChartFragmentBinding.inflate(layoutInflater, container, false)
        reDrawChart(selectedArea)
        return binding.root
    }

    fun reDrawChart(selectedArea: String?) {
        this.selectedArea = selectedArea
        activity?.let {
            CoroutineScope(Dispatchers.IO).launch {
                // 실거래가 셋팅 (데이터)
                val tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)?.reversed()
                val filterData = tradeList?.filter { it.areaForExclusiveUse.toString() == selectedArea }
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                var startDateTime = 0L
                filterData?.let {
                    if (it.isNotEmpty()) {
                        startDateTime = dateFormat.parse(
                            it[0].dealYear.toString() + (if (it[0].dealMonth < 10) "0" else "") + it[0].dealMonth.toString() + (if (it[0].dealDay < 10) "0" else "") + it[0].dealDay.toString()
                        ).time
                    }
                }

                // TODO 같은날 여러건 있는 데이터는 1개의 데이터로 변환 (평균)
                val valuesList = filterData?.map { aptEntity ->
                    val endDateTime = dateFormat.parse(
                        aptEntity.dealYear.toString() + (if (aptEntity.dealMonth < 10) "0" else "") + aptEntity.dealMonth.toString() + (if (aptEntity.dealDay < 10) "0" else "") + aptEntity.dealDay.toString()
                    ).time
                    val differentTime = (endDateTime - startDateTime) / (24 * 60 * 60 * 1000)

                    Entry(
                        differentTime.toFloat(),
                        if (aptEntity.isTrade) aptEntity.dealAmount.replace(",", "").toFloat() else aptEntity.deposit.replace(",", "").toFloat()
                    )
                }

                val set1 = LineDataSet(valuesList, null)
                val dataSets: ArrayList<ILineDataSet> = ArrayList()
                dataSets.add(set1)

                val data = LineData(dataSets)
                data.setDrawValues(false)

                set1.color = Color.BLACK
                set1.setCircleColor(Color.BLACK)
                set1.setDrawCircles(false)
                set1.mode = LineDataSet.Mode.CUBIC_BEZIER

                launch(Dispatchers.Main) {
                    // 데이터 셋팅
                    binding.lineChart.data = data
                    // 좌측 하단 label 제거
                    binding.lineChart.legend.isEnabled = false
                    // 우측 하단 description 문구 제거
                    binding.lineChart.description = null
                    // Touch Highlight 제거
                    binding.lineChart.data.isHighlightEnabled = false
                    // 더블 Tap 으로 확대 기능 제거
                    binding.lineChart.isDoubleTapToZoomEnabled = false

                    binding.lineChart.notifyDataSetChanged()
                    binding.lineChart.invalidate()
                }
            }
        }
    }

}