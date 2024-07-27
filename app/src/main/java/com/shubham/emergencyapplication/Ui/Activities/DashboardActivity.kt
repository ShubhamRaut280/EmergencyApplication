package com.shubham.emergencyapplication.Ui.Activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View.GONE
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.shubham.emergencyapplication.BottomSheets.DialogUtils.showSosBottomSheetDialog
import com.shubham.emergencyapplication.BottomSheets.showUpdateDetailsBottomSheet
import com.shubham.emergencyapplication.Callbacks.ResponseCallBack
import com.shubham.emergencyapplication.Dialogs.DialogUtils.showUpdateDetailsDialog
import com.shubham.emergencyapplication.Models.User
import com.shubham.emergencyapplication.R
import com.shubham.emergencyapplication.Repositories.UserRepository.addLocationToDb
import com.shubham.emergencyapplication.Repositories.UserRepository.getUserInfo
import com.shubham.emergencyapplication.SharedPref.UserDataSharedPref.isProfileUpdated
import com.shubham.emergencyapplication.SharedPref.UserDataSharedPref.setUserDetails
import com.shubham.emergencyapplication.Ui.Fragments.HomeFragment
import com.shubham.emergencyapplication.Ui.Fragments.MapFragment
import com.shubham.emergencyapplication.Ui.Fragments.ProfileFragment
import com.shubham.emergencyapplication.Utils.Constants.EMAIL
import com.shubham.emergencyapplication.Utils.Constants.IMAGE_URL
import com.shubham.emergencyapplication.Utils.Constants.NAME
import com.shubham.emergencyapplication.Utils.Constants.PHONE
import com.shubham.emergencyapplication.Utils.DraggableUtils.makeViewDraggable
import com.shubham.emergencyapplication.Utils.UtilityFuns.handleAdjustResizeForKeyboard
import com.shubham.emergencyapplication.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private val LOCATION_REQUEST_CODE = 1
    private val BACKGROUND_LOCATION_REQUEST_CODE = 2
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewPager = binding.viewPager
        bottomNavigationView = binding.bottomNavigation

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        init()

        requestLocationPermissions()
        saveUserDetails()

    }

    private fun saveUserDetails() {
        getUserInfo(this, object : ResponseCallBack<User>{
            override fun onSuccess(response: User?) {
                if (response != null) {
                    setUserDetails(this@DashboardActivity, NAME, response.name)
                    setUserDetails(this@DashboardActivity, EMAIL, response.email)
                    setUserDetails(this@DashboardActivity, PHONE, response.phone.toString())
                    setUserDetails(this@DashboardActivity, IMAGE_URL, response.image_url)
                }
            }
            override fun onError(error: String?) {
                // Handle error
                Log.d("DashboardActivity", "getUserInfoerror : $error")
            }
        })
    }

    private val locationRequest = LocationRequest.create().apply {
        interval = 4000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }



    private fun init() {


        if(!isProfileUpdated(this)){
            showUpdateDetailsBottomSheet(this, FirebaseAuth.getInstance())
        }

        makeViewDraggable(binding.addPerson)

        binding.addPerson.setOnClickListener {
            showSosBottomSheetDialog(this, supportFragmentManager)
        }
        window.statusBarColor = resources.getColor(R.color.main)

        setupViewPager()
        setupBottomNavigation()
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun setupViewPager() {
        val adapter = FragmentAdapter(this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.map -> {
                    binding.addPerson.visibility = GONE
                    viewPager.currentItem = 1
                    true
                }
                R.id.profile -> {
                    viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bottom_navigation_menu, menu)
        return true
    }

    private inner class FragmentAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> MapFragment()
                2 -> ProfileFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_REQUEST_CODE)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                }
            }
            BACKGROUND_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Handle background location permission granted
                }
            }
        }
    }

    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult?.let {
                    for (location in it.locations) {
                        saveLocation(location)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error starting location updates", e)
        }
    }

    private fun saveLocation(location: Location?) {
        if (location!=null){
            val latitude = location.latitude
            val longitude = location.longitude
            addLocationToDb(this@DashboardActivity, latitude, longitude)

        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



}
