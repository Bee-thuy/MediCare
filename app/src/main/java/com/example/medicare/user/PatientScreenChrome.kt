package com.example.medicare.user

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R

/** Shared header and bottom navigation for patient screens outside the home tab. */
object PatientScreenChrome {
    fun bind(activity: AppCompatActivity, title: String) {
        activity.findViewById<TextView>(R.id.patientChromeTitle)?.text = title
        activity.findViewById<TextView>(R.id.patientChromeMenu)?.setOnClickListener {
            openHome(activity, PatientHomeActivity.TAB_HOME, catalog = true)
        }
        activity.findViewById<TextView>(R.id.patientChromeNotifications)?.setOnClickListener {
            openHome(activity, PatientHomeActivity.TAB_ACCOUNT)
        }
        val nav = listOf(
            R.id.patientChromeNavAccount to {
                openHome(activity, PatientHomeActivity.TAB_ACCOUNT)
            },
            R.id.patientChromeNavHome to { openHome(activity, PatientHomeActivity.TAB_HOME) },
            R.id.patientChromeNavSearch to { openHome(activity, PatientHomeActivity.TAB_SEARCH) },
            R.id.patientChromeNavBooking to { openHome(activity, PatientHomeActivity.TAB_BOOKING) },
            R.id.patientChromeNavCatalog to { openHome(activity, PatientHomeActivity.TAB_HOME, catalog = true) },
        )
        nav.forEach { (id, action) ->
            activity.findViewById<View>(id)?.setOnClickListener { action() }
        }
    }

    private fun openHome(activity: AppCompatActivity, tab: Int, catalog: Boolean = false) {
        activity.startActivity(Intent(activity, PatientHomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(PatientHomeActivity.EXTRA_TAB, tab)
            .putExtra(PatientHomeActivity.EXTRA_OPEN_CATALOG, catalog))
    }
}
