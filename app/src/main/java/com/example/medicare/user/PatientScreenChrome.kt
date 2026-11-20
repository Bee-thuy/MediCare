package com.example.medicare.user

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.user.settings.PatientSettingsActivity
import com.example.medicare.notification.NotificationsActivity

/** Shared header and bottom navigation for patient screens outside the home tab. */
object PatientScreenChrome {
    fun bind(activity: AppCompatActivity, title: String) {
        activity.findViewById<TextView>(R.id.patientChromeTitle)?.text = title
        activity.findViewById<TextView>(R.id.patientChromeMenu)?.setOnClickListener {
            openHome(activity, PatientHomeActivity.TAB_HOME, catalog = true)
        }
        activity.findViewById<TextView>(R.id.patientChromeNotifications)?.setOnClickListener {
            activity.startActivity(Intent(activity,
                if (activity is PatientSettingsActivity) NotificationsActivity::class.java
                else PatientSettingsActivity::class.java))
        }
        val nav = listOf(
            R.id.patientChromeNavAccount to {
                if (activity !is PatientSettingsActivity) activity.startActivity(Intent(activity, PatientSettingsActivity::class.java))
            },
            R.id.patientChromeNavHome to { openHome(activity, PatientHomeActivity.TAB_HOME) },
            R.id.patientChromeNavSearch to { openHome(activity, PatientHomeActivity.TAB_SEARCH) },
            R.id.patientChromeNavBooking to { openHome(activity, PatientHomeActivity.TAB_BOOKING) },
            R.id.patientChromeNavCatalog to { openHome(activity, PatientHomeActivity.TAB_HOME, catalog = true) },
        )
        nav.forEach { (id, action) ->
            activity.findViewById<TextView>(id)?.apply {
                val label = SpannableString(text)
                val end = label.indexOf('\n')
                if (end > 0) label.setSpan(RelativeSizeSpan(1.8f), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = label
                if (id == R.id.patientChromeNavAccount && activity is PatientSettingsActivity) {
                    setTextColor(activity.getColor(R.color.patient_purple))
                    isSelected = true
                }
                setOnClickListener { action() }
            }
        }
    }

    private fun openHome(activity: AppCompatActivity, tab: Int, catalog: Boolean = false) {
        activity.startActivity(Intent(activity, PatientHomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(PatientHomeActivity.EXTRA_TAB, tab)
            .putExtra(PatientHomeActivity.EXTRA_OPEN_CATALOG, catalog))
    }
}
