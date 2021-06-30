package com.dat.fresh_connect.ble

data class ServiceModel(
    private val service: String,
    private val listOfCharacteristics: MutableList<String>?
)