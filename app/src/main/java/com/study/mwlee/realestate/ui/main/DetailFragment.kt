package com.study.mwlee.realestate.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.size
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
import java.util.*
import kotlin.collections.ArrayList


class DetailFragment(private val aptName: String) : Fragment(), View.OnClickListener {

    companion object {
        fun newInstance(aptName: String) = DetailFragment(aptName)
    }

    private lateinit var binding: DetailFragmentBinding
    private lateinit var viewModel: DetailViewModel
    private val chartFragment = ArrayList<ChartFragment>()

    private var tradeList: List<AptEntity>? = null
    private val tradeShowCountMax = 5
    private var tradeCurrentCount = 0

    private var selectedArea: String? = null

    init {
        for (position in 0..1) {
            chartFragment.add(ChartFragment(position, aptName))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DetailFragmentBinding.inflate(layoutInflater, container, false)
        viewModel = ViewModelProvider(this).get(DetailViewModel::class.java)

        // Trade 값이 변경될 경우 같이 변경되어야 하는 UI
        val isTradeObserver = Observer<Boolean> { isTrade ->
            if (isTrade) {
                // 매매(Trade) 클릭
                binding.textTrade.setTextColor(ResourcesCompat.getColor(resources, R.color.white, context?.theme))
                binding.textTrade.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.actionbar, context?.theme))
                binding.textRent.setTextColor(ResourcesCompat.getColor(resources, R.color.actionbar, context?.theme))
                binding.textRent.setBackgroundColor(ResourcesCompat.getColor(resources, android.R.color.transparent, context?.theme))
            } else {
                // 전월세(Rent) 클릭
                binding.textTrade.setTextColor(ResourcesCompat.getColor(resources, R.color.actionbar, context?.theme))
                binding.textTrade.setBackgroundColor(ResourcesCompat.getColor(resources, android.R.color.transparent, context?.theme))
                binding.textRent.setTextColor(ResourcesCompat.getColor(resources, R.color.white, context?.theme))
                binding.textRent.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.actionbar, context?.theme))
            }
            // 차트 다시 그리기
            chartFragment[binding.containerTab.selectedTabPosition].reDrawChart(isTrade, selectedArea)

            // 실거래 리스트 다시 그리기
            activity?.let {
                CoroutineScope(Dispatchers.Default).launch {
                    // 실거래가 셋팅 (데이터)
                    tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)?.filter { it.isTrade == isTrade }

                    launch(Dispatchers.Main) {
                        // 거래 리스트 데이터 초기화
                        tradeCurrentCount = 0
                        clearTradeData()
                        // 실거래가 셋팅 (UI)
                        showNextTradeInfo()
                    }
                }
            }
        }

        // 관찰할 데이터에 옵저버 등록
        viewModel.isTrade.observe(viewLifecycleOwner, isTradeObserver)

        // 전월세 이벤트 등록
        binding.textTrade.setOnClickListener(this)
        binding.textRent.setOnClickListener(this)

        // Tab 셋팅 - Chart
        activity?.supportFragmentManager?.beginTransaction()?.replace(binding.containerChart.id, chartFragment[0])?.commit()

        binding.containerTab.addTab(binding.containerTab.newTab().setText(String.format(getString(R.string.latest_year), 1)))
        binding.containerTab.addTab(binding.containerTab.newTab().setText(getString(R.string.trade_rent)))

        // Tab 선택 이벤트
        binding.containerTab.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selected = chartFragment[tab.position]
                activity?.supportFragmentManager?.beginTransaction()?.replace(binding.containerChart.id, selected)?.commit()

                selectedArea?.let {
                    chartFragment[tab.position].reDrawChart(viewModel.isTrade.value, it)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        makeTradeData(true, null)
        activity?.let {
            CoroutineScope(Dispatchers.Default).launch {
                // 평형 가져오기 (데이터)
                val areaData = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptAreaData(aptName)

                // 실거래가 셋팅 (데이터)
                tradeList = viewModel.isTrade.value?.let { isTrade ->
                    DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)?.filter { it.isTrade == isTrade }
                }
                launch(Dispatchers.Main) {
                    // 평형 가져오기 (UI)
                    areaData?.let {
                        binding.spinnerArea.adapter = context?.let { cnt ->
                            ArrayAdapter(cnt, android.R.layout.simple_spinner_dropdown_item, it.sorted())
                        }
                    }
                }
            }
        }

        binding.spinnerArea.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 스피너로 선택한 데이터만 보여주기
                selectedArea = adapterView?.selectedItem.toString()
                // TODO 테스트 용도로 제거 필요
                binding.textAverageMonth.text = adapterView?.selectedItem.toString()

                // 거래 리스트 데이터 초기화
                tradeCurrentCount = 0
                clearTradeData()

                // 차트 데이터 초기화
                chartFragment[binding.containerTab.selectedTabPosition].reDrawChart(viewModel.isTrade.value, selectedArea)

                // 실거래가 셋팅 (UI)
                showNextTradeInfo()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 더보기 이벤트
        binding.textMoreShow.setOnClickListener {
            showNextTradeInfo()
        }

        // 기타 설정
        binding.textLastCheckTime.text = String.format(getString(R.string.last_check_time), 2)
        return binding.root
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.textTrade -> viewModel.isTrade.value = true
            R.id.textRent -> viewModel.isTrade.value = false
        }
    }

    // 데이터에 따른 더보기 레이아웃 보여줄지, 데이터를 추가할지 여부
    private fun showNextTradeInfo() {
        tradeList?.let { it ->
            val filterData = it.filter { it.areaForExclusiveUse.toString() == selectedArea }
            if (tradeCurrentCount + 1 <= filterData.size) {
                for (i in tradeCurrentCount until (tradeCurrentCount + tradeShowCountMax)) {
                    if (i < filterData.size) {
                        makeTradeData(false, filterData[i])
                    } else {
                        binding.textMoreShow.visibility = View.GONE
                        return@let
                    }
                }
                tradeCurrentCount = binding.linearTradeListLayout.childCount - 1
                binding.textMoreShow.visibility = View.VISIBLE
            } else {
                binding.textMoreShow.visibility = View.GONE
            }
        }
    }

    // 상세 리스트에 데이터 추가
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
                val firstData = data?.dealYear.toString() + "." + data?.dealMonth.toString() + "." + data?.dealDay.toString()
                textFirst.text = firstData
                textSecond.text = if (data?.isTrade == true) data.dealAmount else data?.deposit
                textThird.text = data?.areaForExclusiveUse.toString()
                textFourth.text = data?.floor.toString()
            }

            binding.linearTradeListLayout.addView(item)
        }
    }

    // 상세 리스트 클리어
    private fun clearTradeData() {
        for (itemIndex in binding.linearTradeListLayout.size - 1 downTo 1) {
            binding.linearTradeListLayout.removeViewAt(itemIndex)
        }
    }

}