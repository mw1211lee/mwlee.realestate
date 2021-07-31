package com.study.mwlee.realestate.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DetailViewModel : ViewModel() {
    var average = MutableLiveData<Int>()

    init {
        average.value = 0
    }
}