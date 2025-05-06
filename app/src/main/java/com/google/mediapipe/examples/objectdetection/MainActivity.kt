/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mediapipe.examples.objectdetection

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.mediapipe.examples.objectdetection.databinding.ActivityMainBinding

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        activityMainBinding.navigation.setupWithNavController(navController)
        activityMainBinding.navigation.setOnNavigationItemReselectedListener {
            // ignore the reselection
        }
    }

    override fun onBackPressed() {
        finish()
    }
}

//class MainActivity : AppCompatActivity() {
//    private lateinit var activityMainBinding: ActivityMainBinding
//    private lateinit var webView: WebView  // Tambahkan ini
//    private val viewModel: MainViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(activityMainBinding.root)
//
//        // Inisialisasi WebView
//        webView = findViewById(R.id.webView)
//        setupWebView()
//
//        // Setup Navigation (Tetap)
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
//        val navController = navHostFragment.navController
//        activityMainBinding.navigation.setupWithNavController(navController)
//        activityMainBinding.navigation.setOnNavigationItemReselectedListener { /* ignore */ }
//    }
//
//    private fun setupWebView() {
//        webView.apply {
//            settings.javaScriptEnabled = true
//            settings.loadWithOverviewMode = true
//            settings.useWideViewPort = true
//            settings.domStorageEnabled = true // Untuk konten dinamis
//
//            webViewClient = object : WebViewClient() {
//                override fun shouldOverrideUrlLoading(
//                    view: WebView?,
//                    url: String?
//                ): Boolean {
//                    url?.let { view?.loadUrl(it) }
//                    return true
//                }
//
//                // Handle error loading
//                override fun onReceivedError(
//                    view: WebView?,
//                    errorCode: Int,
//                    description: String?,
//                    failingUrl: String?
//                ) {
//                    Log.e("WebView", "Error $errorCode: $description")
//                }
//            }
//
//            // Enable debugging (untuk development)
//            WebView.setWebContentsDebuggingEnabled(true)
//
//            // Load URL
//            loadUrl("http://192.168.4.1/stream")
//        }
//    }
//
//    override fun onBackPressed() {
//        if (webView.canGoBack()) {
//            webView.goBack()  // Handle back navigation di WebView
//        } else {
//            finish()  // Tutup app jika tidak bisa back
//        }
//    }
//}
