package com.example.medicare.manager.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.databinding.ActivityManagerBinding
import com.example.medicare.manager.fragment.ManagerDashboardFragment
import com.example.medicare.manager.fragment.ManagerDoctorsFragment
import com.example.medicare.manager.fragment.ManagerMoreFragment
import com.example.medicare.manager.fragment.ManagerPatientsFragment
import com.example.medicare.manager.fragment.ManagerWorkScheduleFragment

/** The manager shell: one activity that owns the persistent bottom navigation. */
open class ManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerBinding
    private var selectedTab = TAB_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        binding = ActivityManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB, TAB_HOME) ?: TAB_HOME
        binding.managerNavHomeInclude.root.setOnClickListener { showTab(TAB_HOME) }
        binding.managerNavDoctorsInclude.root.setOnClickListener { showTab(TAB_DOCTORS) }
        binding.managerNavPatientsInclude.root.setOnClickListener { showTab(TAB_PATIENTS) }
        binding.managerNavNotificationsInclude.root.setOnClickListener { showTab(TAB_WORK_SCHEDULE) }
        binding.managerNavMoreInclude.root.setOnClickListener { showTab(TAB_MORE) }
        binding.managerNotificationButton.setOnClickListener { showTab(TAB_WORK_SCHEDULE) }

        if (savedInstanceState == null) showTab(selectedTab) else updateNavigation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showTab(tab: Int) {
        selectedTab = tab
        binding.managerBottomNavigation.isVisible = tab != TAB_WORK_SCHEDULE
        supportFragmentManager.beginTransaction()
            .replace(binding.managerFragmentContainer.id, fragmentFor(tab))
            .commit()
        updateNavigation()
    }

    private fun fragmentFor(tab: Int): Fragment = when (tab) {
        TAB_HOME -> ManagerDashboardFragment()
        TAB_DOCTORS -> ManagerDoctorsFragment()
        TAB_PATIENTS -> ManagerPatientsFragment()
        TAB_WORK_SCHEDULE -> ManagerWorkScheduleFragment()
        else -> ManagerMoreFragment()
    }

    private fun updateNavigation() {
        binding.managerBottomNavigation.isVisible = selectedTab != TAB_WORK_SCHEDULE
        val items = listOf(
            NavigationItem(binding.managerNavHomeInclude.root, binding.managerNavHomeInclude.managerNavHomeIcon, binding.managerNavHomeInclude.managerNavHomeLabel, TAB_HOME),
            NavigationItem(binding.managerNavDoctorsInclude.root, binding.managerNavDoctorsInclude.managerNavDoctorsIcon, binding.managerNavDoctorsInclude.managerNavDoctorsLabel, TAB_DOCTORS),
            NavigationItem(binding.managerNavPatientsInclude.root, binding.managerNavPatientsInclude.managerNavPatientsIcon, binding.managerNavPatientsInclude.managerNavPatientsLabel, TAB_PATIENTS),
            NavigationItem(binding.managerNavNotificationsInclude.root, binding.managerNavNotificationsInclude.managerNavNotificationsIcon, binding.managerNavNotificationsInclude.managerNavNotificationsLabel, TAB_WORK_SCHEDULE),
            NavigationItem(binding.managerNavMoreInclude.root, binding.managerNavMoreInclude.managerNavMoreIcon, binding.managerNavMoreInclude.managerNavMoreLabel, TAB_MORE),
        )
        items.forEach { item ->
            val active = item.tab == selectedTab
            val color = ContextCompat.getColor(this, if (active) R.color.manager_blue else R.color.manager_muted)
            item.container.isSelected = active
            item.icon.imageTintList = ColorStateList.valueOf(color)
            item.label.setTextColor(color)
        }
    }

    private data class NavigationItem(
        val container: View,
        val icon: ImageView,
        val label: TextView,
        val tab: Int,
    )

    private companion object {
        const val STATE_SELECTED_TAB = "selected_manager_tab"
        const val TAB_HOME = 0
        const val TAB_DOCTORS = 1
        const val TAB_PATIENTS = 2
        const val TAB_WORK_SCHEDULE = 3
        const val TAB_MORE = 4
    }
}
