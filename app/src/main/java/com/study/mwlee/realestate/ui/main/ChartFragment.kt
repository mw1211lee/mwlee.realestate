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


class ChartFragment(private val aptName: String) : Fragment() {

    private lateinit var binding: ChartFragmentBinding
    private var selectedArea: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ChartFragmentBinding.inflate(layoutInflater, container, false)
        reDrawChart(true, selectedArea)
        return binding.root
    }

    fun reDrawChart(isTrade: Boolean?, selectedArea: String?) {
        val isTradeNotNull = isTrade ?: true
        this.selectedArea = selectedArea
        activity?.let {
            CoroutineScope(Dispatchers.IO).launch {
                // 실거래가 셋팅 (데이터)
                val tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(isTradeNotNull, aptName)?.reversed()
                val filterData = tradeList?.filter { it.areaForExclusiveUse.toString() == selectedArea }

                // 1달 단위로 데이터 평균을 구함
                var startString = ""
                val monthAverage = filterData?.groupBy { it.dealYear.toString() + (if (it.dealMonth < 10) "0" else "") + it.dealMonth.toString() }
                val valuesList = ArrayList<Entry>()
                val mapSize = monthAverage?.size ?: 0
                if (mapSize > 0) {
                    monthAverage?.firstNotNullOf {
                        startString = it.key
                    }
                }

                // 차트에 맞도록 데이터 가공
                monthAverage?.forEach { month ->
                    valuesList.add(Entry(
                        if (month.key[3] == startString[3]) month.key.toFloat() - startString.toFloat()
                        else month.key.substring(4, 6).toFloat() - startString.substring(4, 6).toFloat() + 12,
                        (month.value.sumOf {
                            if (it.isTrade) it.dealAmount.replace(",", "").toDouble()
                            else it.deposit.replace(",", "").toDouble()
                        } / month.value.size).toFloat()
                    ))
                }

                // 차트 셋팅
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