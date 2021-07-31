package com.study.mwlee.realestate.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.study.mwlee.realestate.R
import com.study.mwlee.realestate.databinding.DetailFragmentBinding
import com.study.mwlee.realestate.room.AptEntity
import com.study.mwlee.realestate.room.DatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DetailFragment : Fragment() {

    companion object {
        fun newInstance() = DetailFragment()
    }

    private lateinit var binding: DetailFragmentBinding
    private lateinit var viewModel: DetailViewModel

    private var tradeList: List<AptEntity>? = null
    private val tradeShowCountMax = 5
    private var tradeCurrentCount = 0

    private val chartFragment1 = ChartFragment()
    private val chartFragment2 = ChartFragment()
    private val chartFragment3 = ChartFragment()
    private val chartFragment4 = ChartFragment()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DetailFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DetailViewModel::class.java)
        // Observer를 생성한 뒤 UI에 업데이트 시켜 줍니다.
        val testObserver = Observer<Int> { updateValue ->
            // 현재 MainActivity에는 TextView가 하나만 존재합니다.
            // 다른 데이터를 받는 UI 컴포넌트가 있다면 같이 세팅 해줍니다.
            binding.detailLastMonthData.text = updateValue.toString()
        }

        // LiveData를 Observer를 이용해 관찰하고
        // 현재 Activity 및 Observer를 LifecycleOwner로 전달합니다.
        viewModel.average.observe(viewLifecycleOwner, testObserver)

        // Tab 셋팅 - Chart
        activity?.supportFragmentManager?.beginTransaction()?.replace(binding.containerChart.id, chartFragment1)?.commit()

        binding.containerTab.addTab(binding.containerTab.newTab().setText("A"))
        binding.containerTab.addTab(binding.containerTab.newTab().setText("B"))
        binding.containerTab.addTab(binding.containerTab.newTab().setText("C"))
        binding.containerTab.addTab(binding.containerTab.newTab().setText("D"))

        binding.containerTab.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val position = tab.position
                var selected: ChartFragment? = null
                selected = when (position) {
                    0 -> chartFragment1
                    1 -> chartFragment2
                    2 -> chartFragment3
                    3 -> chartFragment4
                    else -> chartFragment1
                }
                activity?.supportFragmentManager?.beginTransaction()?.replace(binding.containerChart.id, selected)?.commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.textLastCheckTime.text = String.format(getString(R.string.last_check_time), 2)

        // 실거래가 셋팅
        makeTradeData(true, null)
        activity?.let {
            it.intent.extras?.let { extra ->
                val aptName = extra.getString("apartmentName")
                aptName?.let { apartmentName ->
                    CoroutineScope(Dispatchers.IO).launch {
                        tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(apartmentName)
                        launch(Dispatchers.Main) {
                            showNextTradeInfo()
                        }
                    }
                }
            }
        }

        // 더보기 이벤트
        binding.textMoreShow.setOnClickListener {
            showNextTradeInfo()
        }
    }

    private fun showNextTradeInfo() {
        tradeList?.let {
            if (tradeCurrentCount + 1 <= it.size) {
                for (i in tradeCurrentCount until (tradeCurrentCount + tradeShowCountMax)) {
                    if (i < it.size) {
                        makeTradeData(false, it[i])
                    }
                }
                tradeCurrentCount = binding.linearTradeListLayout.childCount - 1
                binding.textMoreShow.visibility = View.VISIBLE
            } else {
                binding.textMoreShow.visibility = View.GONE
            }
        }
    }

    private fun makeTradeData(isHeader: Boolean = false, data: AptEntity?) {
        activity?.let {
            val item = View.inflate(it, R.layout.view_trade_item, null)
            val viewLineTop = item.findViewById<View>(R.id.viewLineTop)
            val textFirst = item.findViewById<AppCompatTextView>(R.id.textFirst)
            val textSecond = item.findViewById<AppCompatTextView>(R.id.textSecond)
            val textThird = item.findViewById<AppCompatTextView>(R.id.textThird)
            val textFourth = item.findViewById<AppCompatTextView>(R.id.textFourth)

            if (isHeader) {
                item.setBackgroundColor(it.getColor(R.color.detail_view_trade_item_header_back))
                textFirst.text = "계약일"
                textSecond.text = "가격"
                textThird.text = "타입"
                textFourth.text = "층"
            } else {
                viewLineTop.visibility = View.GONE
                textFirst.text = data?.dealYear.toString() + "." + data?.dealMonth.toString() + "." + data?.dealDay.toString()
                textSecond.text = data?.dealAmount
                textThird.text = data?.areaForExclusiveUse.toString()
                textFourth.text = data?.floor.toString()
            }

            binding.linearTradeListLayout.addView(item)
        }
    }

}