package com.study.mwlee.realestate.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
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


class DetailFragment(private val aptName: String) : Fragment(), View.OnClickListener {

    companion object {
        fun newInstance(aptName: String) = DetailFragment(aptName)
    }

    private lateinit var binding: DetailFragmentBinding
    private lateinit var viewModel: DetailViewModel

    private var tradeList: List<AptEntity>? = null
    private val tradeShowCountMax = 5
    private var tradeCurrentCount = 0

    private val chartFragment1 = ChartFragment(aptName)
    private val chartFragment2 = ChartFragment(aptName)
    private val chartFragment3 = ChartFragment(aptName)
    private val chartFragment4 = ChartFragment(aptName)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DetailFragmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DetailViewModel::class.java)

        // Trade 값이 변경될 경우 같이 변경되어야 하는 UI
        val isTradeObserver = Observer<Boolean> { updateValue ->
            if (updateValue) {
                // 매매(Trade) 클릭
                binding.textTrade.setTextColor(ResourcesCompat.getColor(resources, R.color.white, context?.theme))
                binding.textTrade.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.purple_200, context?.theme))
                binding.textRent.setTextColor(ResourcesCompat.getColor(resources, R.color.purple_200, context?.theme))
                binding.textRent.setBackgroundColor(ResourcesCompat.getColor(resources, android.R.color.transparent, context?.theme))
            } else {
                // 전월세(Rent) 클릭
                binding.textTrade.setTextColor(ResourcesCompat.getColor(resources, R.color.purple_200, context?.theme))
                binding.textTrade.setBackgroundColor(ResourcesCompat.getColor(resources, android.R.color.transparent, context?.theme))
                binding.textRent.setTextColor(ResourcesCompat.getColor(resources, R.color.white, context?.theme))
                binding.textRent.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.purple_200, context?.theme))
            }
        }

        // 관찰할 데이터에 옵저버 등록
        viewModel.isTrade.observe(viewLifecycleOwner, isTradeObserver)

        // 전월세 이벤트 등록
        binding.textTrade.setOnClickListener(this)
        binding.textRent.setOnClickListener(this)

        // Tab 셋팅 - Chart
        activity?.supportFragmentManager?.beginTransaction()?.replace(binding.containerChart.id, chartFragment1)?.commit()

        binding.containerTab.addTab(binding.containerTab.newTab().setText("A"))
        binding.containerTab.addTab(binding.containerTab.newTab().setText("B"))
        binding.containerTab.addTab(binding.containerTab.newTab().setText("C"))
        binding.containerTab.addTab(binding.containerTab.newTab().setText("D"))

        // Tab 선택 이벤트
        binding.containerTab.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selected = when (tab.position) {
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

        makeTradeData(true, null)
        activity?.let {
            CoroutineScope(Dispatchers.IO).launch {
                // 평형 가져오기 (데이터)
                // TODO
                val areaData = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptAreaData(aptName)

                // 실거래가 셋팅 (데이터)
                tradeList = DatabaseHelper.getInstance(it)?.getAptDao()?.getAptData(aptName)
                launch(Dispatchers.Main) {
                    // 평형 가져오기 (UI)
                    // TODO
                    areaData?.let {
                        binding.spinnerArea.adapter = context?.let { cnt ->
                            ArrayAdapter(cnt, android.R.layout.simple_spinner_dropdown_item, it.sorted())
                        }
                    }

                    // 실거래가 셋팅 (UI)
                    showNextTradeInfo()
                }
            }
        }
        binding.spinnerArea.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // TODO 스피너 선택한 데이터만 보여주기
                binding.textAverageMonth.text = adapterView?.selectedItem.toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 더보기 이벤트
        binding.textMoreShow.setOnClickListener {
            showNextTradeInfo()
        }

        // 기타 설정
        binding.textLastCheckTime.text = String.format(getString(R.string.last_check_time), 2)
    }

    private fun showNextTradeInfo() {
        tradeList?.let {
            if (tradeCurrentCount + 1 <= it.size) {
                for (i in tradeCurrentCount until (tradeCurrentCount + tradeShowCountMax)) {
                    if (i < it.size) {
                        makeTradeData(false, it[i])
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

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.textTrade -> viewModel.isTrade.value = true
            R.id.textRent -> viewModel.isTrade.value = false
        }
    }

}