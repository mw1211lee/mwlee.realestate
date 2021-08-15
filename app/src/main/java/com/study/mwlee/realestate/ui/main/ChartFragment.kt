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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ChartFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let {
            CoroutineScope(Dispatchers.IO).launch {
                // 실거래가 셋팅 (데이터)
                val tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)
                // TODO 처음 날짜와 끝 날짜를 가져와서 날짜 개수를 구하여 X 축 값 셋팅

                // TODO index 로는 안됨. 날짜 계산해서 나눠서 해야함. (같은날 여러건 있음)
                val valuesList = tradeList?.mapIndexed { index, aptEntity ->
                    Entry(
                        index.toFloat(),
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
                }
            }
        }

    }

}