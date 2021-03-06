package com.example.fishies.viewModel

import android.annotation.SuppressLint
import android.hardware.usb.UsbDevice.getDeviceId
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.fishies.model.*
import com.example.fishies.repository.UserRepository
import kotlinx.coroutines.launch


class StateViewModel(val repository: UserRepository): ViewModel() {
    private val filename = "state.json"
    lateinit var User: MutableLiveData<User>
    var fishesNumber: MutableLiveData<Int> = MutableLiveData(0)
    var fishPrice: MutableLiveData<Int> = MutableLiveData(1)
    var currentAngler: MutableLiveData<Upgrade?> = MutableLiveData(null)
    var welcomeDialogTime: Boolean = true

    init {
       getUserState()
    }

    fun updateUserState(){
        viewModelScope.launch {
            repository.updateUserState(User.value!!)
        }
        Log.d("Server", "User repository called")
    }

    @SuppressLint("HardwareIds")
    fun getUserState() {
        User = MutableLiveData(User())
        viewModelScope.launch {
            val response = repository.getUserState(id = "1234")
            Log.d("Device id",getDeviceId("").toString())
            if (response.isSuccessful) {
                Log.d(
                    "Response",
                    "${response.body()?.location.toString()} \n ${response.body()?.new.toString()}"
                )
                User.value = User(
                    id = response.body()!!.id,
                    fishes = response.body()!!.fishes,
                    money = response.body()!!.money,
                    tackleBox = response.body()!!.tackleBox,
                    equippedFishingRod = response.body()!!.equippedFishingRod,
                    lastUnlockedBait = response.body()!!.lastUnlockedBait,
                    location = response.body()!!.location,
                    lastUnlockedAngler = response.body()!!.lastUnlockedAngler,
                    lastUnlockedFish = response.body()!!.lastUnlockedFish,
                    lastLoggedIn = response.body()!!.lastLoggedIn)
                updateUpgradesList()
                updateLocationsList()
                fishesNumber.value = User.value!!.fishes.toInt()
                fishPrice.value = UpgradesList.rods[User.value!!.equippedFishingRod].value
            } else {
                Log.e("Response", response.code().toString() )
            }
        }
    }

    fun click(){
        if(User.value!!.fishes <= UpgradesList.tackleBoxes[User.value!!.tackleBox].value) {
            User.value!!.fishes = User.value!!.fishes.plus(1)//todo: add other upgrades
            Log.d("Clicked", User.value!!.fishes.toString())
            fishesNumber.value = User.value!!.fishes.toInt()
        }
    }

    fun sellFishes() {
        User.value!!.money = User.value!!.money.plus(User.value!!.fishes.toInt()*fishPrice.value!!) //todo: add other upgrades
        User.value!!.fishes = 0f
        fishesNumber.value = User.value!!.fishes.toInt()
        Log.d("Sold", User.value!!.fishes.toString())
        updateUserState()
    }

    fun updateLocationsList() {
        for (index in 0..User.value!!.location) {
            LocationsList.locations[index].bought = true
        }
    }

    fun updateUpgradesList() {
        for (index in UpgradesList.tackleBoxes.indices){
            if(index <= User.value!!.tackleBox) UpgradesList.tackleBoxes[index].bought = true
        }
        for (bait in UpgradesList.baits.indices){
            if(bait <= User.value!!.lastUnlockedBait) UpgradesList.baits[bait].bought = true
        }
        for (index in UpgradesList.rods.indices){
            if(index <= User.value!!.equippedFishingRod) UpgradesList.rods[index].bought = true
        }
        for (index in UpgradesList.anglers.indices){
            if(User.value!!.lastUnlockedAngler != null) {
                if (index <= User.value!!.lastUnlockedAngler!!) UpgradesList.anglers[index].bought =
                    true
            }
        }
    }

    fun buyItem(upgrade: Upgrade) {
            when(upgrade.type){
                "TackleBox" ->{
                    if(User.value!!.tackleBox < UpgradesList.tackleBoxes.indexOf(upgrade)) User.value!!.tackleBox = UpgradesList.tackleBoxes.indexOf(upgrade)
                    UpgradesList.tackleBoxes.first{ it.name == upgrade.name }.bought = true
                    User.value!!.money = User.value!!.money.minus(upgrade.price)
                    User.value = User.value
                }
                "Rod"->{
                    if(User.value!!.equippedFishingRod < UpgradesList.rods.indexOf(upgrade)) User.value!!.equippedFishingRod = UpgradesList.rods.indexOf(upgrade)
                    UpgradesList.rods.first{ it.name == upgrade.name }.bought = true
                    Log.d("Bought item", upgrade.name+" "+UpgradesList.rods.first { it.name == upgrade.name }.bought.toString())
                    User.value!!.money = User.value!!.money.minus(upgrade.price)
                    fishPrice.value = UpgradesList.rods.get(User.value!!.equippedFishingRod).value
                    User.value = User.value
                }
                "Bait"->{
                    if(User.value!!.lastUnlockedBait < UpgradesList.baits.indexOf(upgrade)) User.value!!.lastUnlockedBait = UpgradesList.baits.indexOf(upgrade)
                    UpgradesList.baits.first { it.name == upgrade.name }.bought = true
                    Log.d("Bought item", upgrade.name+" "+UpgradesList.baits.first { it.name == upgrade.name }.bought.toString())
                    User.value!!.money = User.value!!.money.minus(upgrade.price)
                    //User.value!!.lastUnlockedFish = UpgradesList.baits.indexOf(upgrade) //todo: unlocking fish on fishing, not on buying bait
                    User.value = User.value
                }
                "Angler"->{
                    if(User.value!!.lastUnlockedAngler != null) {
                        if (User.value!!.lastUnlockedAngler!! < UpgradesList.anglers.indexOf(upgrade)) User.value!!.lastUnlockedAngler = UpgradesList.anglers.indexOf(upgrade)
                    }else User.value!!.lastUnlockedAngler = UpgradesList.anglers.indexOf(upgrade)
                    UpgradesList.anglers.first { it.name == upgrade.name }.bought = true
                    User.value!!.money = User.value!!.money.minus(upgrade.price)
                    User.value = User.value
                }
                "Reset"->{
                    viewModelScope.launch {
                        Log.d("User id",User.value!!.id)
                        repository.deleteUserState(User.value!!.id)
                    }
                    getUserState()
                }
            }
        updateUpgradesList()
        if(upgrade.type != "Reset") updateUserState()
    }

    fun buyLocation(locationIndex: Int) {
        User.value!!.location = locationIndex
        User.value!!.money = User.value!!.money.minus(LocationsList.locations[locationIndex].price)
        LocationsList.locations[locationIndex].bought = true
        User.value = User.value
        updateUserState()
    }
}