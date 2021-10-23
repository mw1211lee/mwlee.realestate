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
import java.util.*
import kotlin.collections.ArrayList


class ChartFragment(private val position: Int, private val aptName: String) : Fragment() {

    private lateinit var binding: ChartFragmentBinding
    private var isTradeValue: Boolean = true
    private var selectedArea: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ChartFragmentBinding.inflate(layoutInflater, container, false)
        reDrawChart(isTradeValue, selectedArea)
        return binding.root
    }

    // 차트 다시 그리기
    fun reDrawChart(isTrade: Boolean?, selectedArea: String?) {
        this.isTradeValue = isTrade ?: true
        if (position == 0) {
            drawLatestYearChart(selectedArea)
        } else if (position == 1) {
            drawTradeRentChart(selectedArea)
        }
    }

    // 최근 1년 탭 그리기
    private fun drawLatestYearChart(selectedArea: String?) {
        this.selectedArea = selectedArea
        activity?.let {
            CoroutineScope(Dispatchers.Default).launch {
                // 실거래가 셋팅 (데이터)
                val calendarInstance = Calendar.getInstance()
                val tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)?.reversed()
                val filterData = tradeList?.filter {
                    it.isTrade == isTradeValue && it.areaForExclusiveUse.toString() == selectedArea
                            && (it.dealYear == calendarInstance.get(Calendar.YEAR) ||
                            ((it.dealYear == calendarInstance.get(Calendar.YEAR) - 1 && it.dealMonth > calendarInstance.get(Calendar.MONTH) + 1)))
                }

                // 1달 단위로 데이터 평균을 구함
                val startString = (calendarInstance.get(Calendar.YEAR) - 1).toString() +
                        (if (calendarInstance.get(Calendar.MONTH) + 2 < 10) "0" else "") + (calendarInstance.get(Calendar.MONTH) + 2)
                val monthAverage = filterData?.groupBy { it.dealYear.toString() + (if (it.dealMonth < 10) "0" else "") + it.dealMonth.toString() }
                val valuesList = ArrayList<Entry>()

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

                // 첫번째 달 넣어주기
                if (monthAverage?.containsKey(startString) == false && valuesList.isNotEmpty()) {
                    valuesList.add(
                        0,
                        Entry(
                            if (startString[3] == startString[3]) startString.toFloat() - startString.toFloat()
                            else startString.substring(4, 6).toFloat() - startString.substring(4, 6).toFloat() - 12,
                            valuesList.first().y
                        )
                    )
                }

                // 마지막 달 넣어주기
                val currentMonth = calendarInstance.get(Calendar.YEAR).toString() +
                        (if (calendarInstance.get(Calendar.MONTH) + 1 < 10) "0" else "") + (calendarInstance.get(Calendar.MONTH) + 1)
                if (monthAverage?.containsKey(currentMonth) == false && valuesList.isNotEmpty()) {
                    valuesList.add(
                        Entry(
                            if (currentMonth[3] == startString[3]) currentMonth.toFloat() - startString.toFloat()
                            else currentMonth.substring(4, 6).toFloat() - startString.substring(4, 6).toFloat() + 12,
                            valuesList.last().y
                        )
                    )
                }

                // 차트 셋팅
                val lineDataSet = LineDataSet(valuesList, null)
                lineDataSet.color = Color.BLACK
                lineDataSet.setCircleColor(Color.BLACK)
                lineDataSet.setDrawCircles(false)
                lineDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER

                val dataSets: ArrayList<ILineDataSet> = ArrayList()
                dataSets.add(lineDataSet)

                val data = LineData(dataSets)
                data.setDrawValues(false)

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

    // 전세/월세 탭 그리기
    private fun drawTradeRentChart(selectedArea: String?) {
        this.selectedArea = selectedArea
        activity?.let {
            CoroutineScope(Dispatchers.Default).launch {
                // 실거래가 셋팅 (데이터)
                val calendarInstance = Calendar.getInstance()
                val tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)?.reversed()
                val filterData = tradeList?.filter {
                    it.areaForExclusiveUse.toString() == selectedArea
                            && (it.dealYear == calendarInstance.get(Calendar.YEAR) ||
                            ((it.dealYear == calendarInstance.get(Calendar.YEAR) - 1 && it.dealMonth > calendarInstance.get(Calendar.MONTH) + 1)))
                }
                val dataSets: ArrayList<ILineDataSet> = ArrayList()

                for (index in 0..1) {
                    val tradeRentData = filterData?.filter { it.isTrade == (index == 0) }

                    // 1달 단위로 데이터 평균을 구함
                    val startString = (calendarInstance.get(Calendar.YEAR) - 1).toString() +
                            (if (calendarInstance.get(Calendar.MONTH) + 2 < 10) "0" else "") + (calendarInstance.get(Calendar.MONTH) + 2)
                    val monthAverage = tradeRentData?.groupBy { it.dealYear.toString() + (if (it.dealMonth < 10) "0" else "") + it.dealMonth.toString() }
                    val valuesList = ArrayList<Entry>()

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

                    // 첫번째 달 넣어주기
                    if (monthAverage?.containsKey(startString) == false && valuesList.isNotEmpty()) {
                        valuesList.add(
                            0,
                            Entry(
                                if (startString[3] == startString[3]) startString.toFloat() - startString.toFloat()
                                else startString.substring(4, 6).toFloat() - startString.substring(4, 6).toFloat() - 12,
                                valuesList.first().y
                            )
                        )
                    }

                    // 마지막 달 넣어주기
                    val currentMonth = calendarInstance.get(Calendar.YEAR).toString() +
                            (if (calendarInstance.get(Calendar.MONTH) + 1 < 10) "0" else "") + (calendarInstance.get(Calendar.MONTH) + 1)
                    if (monthAverage?.containsKey(currentMonth) == false && valuesList.isNotEmpty()) {
                        valuesList.add(
                            Entry(
                                if (currentMonth[3] == startString[3]) currentMonth.toFloat() - startString.toFloat()
                                else currentMonth.substring(4, 6).toFloat() - startString.substring(4, 6).toFloat() + 12,
                                valuesList.last().y
                            )
                        )
                    }

                    // 차트 셋팅
                    val lineDataSet = LineDataSet(valuesList, null)
                    lineDataSet.color = Color.BLACK
                    lineDataSet.setCircleColor(Color.BLACK)
                    lineDataSet.setDrawCircles(false)
                    lineDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                    dataSets.add(lineDataSet)
                }

                val data = LineData(dataSets)
                data.setDrawValues(false)

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