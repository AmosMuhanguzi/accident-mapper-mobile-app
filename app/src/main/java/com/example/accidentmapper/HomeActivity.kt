package com.example.accidentmapper

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.example.accidentmapper.adapters.ReportAdapter
import com.example.accidentmapper.AccidentReport
import com.example.accidentmapper.database.DatabaseHandler
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.*
import java.time.ZoneId
import java.time.ZoneOffset
import android.util.Log

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var profileIcon: CircleImageView

    // Variables to hold user data passed from Login/Signup
    private var userName = ""
    private var userEmail = ""
    private var userRegion = ""
    private var userProfileUrl = ""

    // --- NEW PROPERTIES FOR REPORTING FEATURE ---
    private lateinit var dbHandler: DatabaseHandler
    private lateinit var recentReportAdapter: ReportAdapter
    private lateinit var earlierReportAdapter: ReportAdapter
    private var refreshJob: Job? = null // For the 1-hour refresh timer
    // ---------------------------------------------

    private  val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. Initialize views
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        profileIcon = findViewById(R.id.profileIcon)
        dbHandler = DatabaseHandler(this)

        // 2. Retrieve user data from the Intent
        userName = intent.getStringExtra("userName") ?: "Default User"
        userEmail = intent.getStringExtra("userEmail") ?: "user@example.com"
        userRegion = intent.getStringExtra("userRegion") ?: "Unknown Region"
        userProfileUrl = intent.getStringExtra("userProfileUrl") ?: ""

        // 3. Set up toolbar and navigation
        setupToolbar()
        setupNavigationView()
        setupRecyclerViews()

        // 4. Set listener for the profile icon to show the popup
        profileIcon.setOnClickListener {
            showUserProfilePopup(it)
        }
    }

    // --- LIFECYCLE FOR LIVE DATA REFRESH ---
    override fun onResume() {
        super.onResume()
        loadReports()
        startReportRefreshTimer()
    }

    override fun onPause() {
        super.onPause()
        stopReportRefreshTimer()
    }
    // ----------------------------------------

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 🔑 FIX: RESTORED ORIGINAL NAVIGATION LOGIC
        // This line enables the standard 'hamburger' icon if the theme allows it,
        // but primarily, the NavigationOnClickListener handles the drawer opening.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(navigationView) // <-- This opens the drawer
        }

        displayProfileImage(userProfileUrl, profileIcon)
    }

    // ... (rest of HomeActivity.kt content remains the same) ...

    private fun setupNavigationView() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawers()

            // 🔑 NEW HELPER FUNCTION: Creates an Intent and attaches user data
            val createReportIntent = { activityClass: Class<*> ->
                Intent(this, activityClass).apply {
                    putExtra("submitterName", userName)
                    putExtra("submitterEmail", userEmail)
                    putExtra("submitterProfileUrl", userProfileUrl)
                }
            }

            when (menuItem.itemId) {
                // Pass user data to Text Report (Already done, but cleaner now)
                R.id.nav_report_text ->
                    startActivity(createReportIntent(TextReportActivity::class.java))

                    // ...
                    // 🔑 CHANGE THIS LINE: Assuming the correct ID for Capture Image is nav_report_camera
                    R.id.nav_report_camera ->
                        startActivity(createReportIntent(CaptureImageActivity::class.java))

                // 🔑 FIX: Pass user data to Record Video
                R.id.nav_report_video ->
                    startActivity(createReportIntent(RecordVideoActivity::class.java))

                // 🔑 FIX: Pass user data to Select from Gallery

                R.id.nav_gallery -> {
                    val intent = Intent(this, SelectFromGalleryActivity::class.java).apply {
                        // Pass user data to the reporting flow
                        putExtra(SelectFromGalleryActivity.REPORTER_NAME_EXTRA, userName)
                        putExtra(SelectFromGalleryActivity.REPORTER_EMAIL_EXTRA, userEmail)
                        putExtra(
                            SelectFromGalleryActivity.REPORTER_PROFILE_URL_EXTRA,
                            userProfileUrl
                        )
                    }

                    // 🔑 ADD THIS DEBUG LOG:
                    Log.d(TAG, "Attempting to launch SelectFromGalleryActivity...")
                    startActivity(intent)
                }

                R.id.nav_about ->
                    startActivity(Intent(this, AboutActivity::class.java))

                R.id.nav_logout ->
                    logoutUser()
            }
            true
        }
    }

// ... (rest of HomeActivity.kt content remains the same) ...

    // --- REPORT LOADING AND FILTERING ---

    private fun loadReports() {
        CoroutineScope(Dispatchers.IO).launch {
            val allReports = dbHandler.getAllReportsForFiltering()
            val oneWeekAgo = System.currentTimeMillis() - 604800000L

            // Filtering logic relies on Java Time API conversion
            val recentList = allReports.filter {
                it.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > oneWeekAgo
            }.sortedByDescending {
                it.timestamp
            }

            val earlierList = allReports.filter {
                it.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() <= oneWeekAgo
            }.sortedByDescending {
                it.timestamp
            }

            withContext(Dispatchers.Main) {
                recentReportAdapter.updateReports(recentList)
                earlierReportAdapter.updateReports(earlierList)
            }
        }
    }

    private fun startReportRefreshTimer() {
        stopReportRefreshTimer()

        refreshJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(60000) // 1 minute delay
                loadReports()
            }
        }
    }

    private fun stopReportRefreshTimer() {
        refreshJob?.cancel()
        refreshJob = null
    }

    // --- VIEW SETUP MODIFIED FOR LIVE DATA ---

    private fun setupRecyclerViews() {
        val recentRecyclerView = findViewById<RecyclerView>(R.id.recentRecyclerView)
        val earlierRecyclerView = findViewById<RecyclerView>(R.id.earlierRecyclerView)
        val recentButton = findViewById<Button>(R.id.recentButton)
        val earlierButton = findViewById<Button>(R.id.earlierButton)

        recentReportAdapter = ReportAdapter(emptyList())
        earlierReportAdapter = ReportAdapter(emptyList())

        recentRecyclerView.layoutManager = LinearLayoutManager(this)
        recentRecyclerView.adapter = recentReportAdapter

        earlierRecyclerView.layoutManager = LinearLayoutManager(this)
        earlierRecyclerView.adapter = earlierReportAdapter


        recentButton.setOnClickListener {
            recentRecyclerView.visibility = View.VISIBLE
            earlierRecyclerView.visibility = View.GONE
        }

        earlierButton.setOnClickListener {
            recentRecyclerView.visibility = View.GONE
            earlierRecyclerView.visibility = View.VISIBLE
        }

        loadReports()
    }

    // --- Profile Popup and Logout  ---

    private fun showUserProfilePopup(view: View) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_user_profile, null)

        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        popupView.measure(widthMeasureSpec, heightMeasureSpec)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val popupWindow = PopupWindow(
            popupView,
            popupWidth,
            popupHeight,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        popupView.findViewById<TextView>(R.id.userName).text = userName
        popupView.findViewById<TextView>(R.id.userEmail).text = userEmail
        popupView.findViewById<TextView>(R.id.userRegion).text = userRegion

        val popupProfileImage = popupView.findViewById<CircleImageView>(R.id.userProfileImage)
        displayProfileImage(userProfileUrl, popupProfileImage)

        popupView.findViewById<Button>(R.id.logoutFromProfile).setOnClickListener {
            popupWindow.dismiss()
            logoutUser()
        }

        val xOffset = view.width - popupWidth
        val yOffset = view.height
        popupWindow.showAsDropDown(view, xOffset, yOffset)
    }

    private fun displayProfileImage(url: String?, imageView: CircleImageView) {
        val placeholderRes = R.drawable.ic_user_placeholder

        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholderRes)
        } else if (url.startsWith("content://") || url.startsWith("file://")) {
            Picasso.get()
                .load(Uri.parse(url))
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(imageView)
        } else {
            Picasso.get()
                .load(url)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(imageView)
        }
    }

    private fun logoutUser() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to log out?")
        builder.setPositiveButton("Yes") { _, _ ->
            Toast.makeText(this, "Simulated Logout", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }
}