package com.example.blescan.ble

data class ServiceModel(
    private val service: String,
    private val listOfCharacteristics: MutableList<String>?
)