package com.dial112

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.dial112.databinding.ActivityMainBinding
import com.dial112.ui.auth.AuthViewModel
import com.dial112.utils.services.SosEmergencyService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Observe authentication state to handle auto-login or logout properly
        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            val currentDestination = navController.currentDestination?.id ?: return@observe
            if (isLoggedIn) {
                // If logged in and still on login screen, navigate to Home
                if (currentDestination == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_homeFragment)
                }
            } else {
                // Ensure we handle logged out flow (e.g., stopping background tracking)
                stopSosService()
                
                // If logged out but inside the app, return to login flow
                if (currentDestination != R.id.loginFragment) {
                     navController.navigate(R.id.loginFragment)
                }
            }
        }
        
        // Initiate session check immediately
        authViewModel.checkLoginStatus()
    }

    private fun stopSosService() {
        val serviceIntent = Intent(this, SosEmergencyService::class.java)
        stopService(serviceIntent)
    }
}
