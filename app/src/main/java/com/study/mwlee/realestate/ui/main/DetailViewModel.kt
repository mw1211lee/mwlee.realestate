package com.study.mwlee.realestate.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DetailViewModel : ViewModel() {
    var isTrade = MutableLiveData<Boolean>()

    init {
        isTrade.value = true
    }
}