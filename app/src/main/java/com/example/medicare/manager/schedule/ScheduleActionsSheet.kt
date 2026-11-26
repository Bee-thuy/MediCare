package com.example.medicare.manager.schedule

import android.content.Context
import com.example.medicare.databinding.SheetScheduleActionsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object ScheduleActionsSheet {
    fun show(
        context: Context,
        onCreateShift: () -> Unit,
        onLeaveRequests: () -> Unit,
    ) {
        val sheet = SheetScheduleActionsBinding.inflate(android.view.LayoutInflater.from(context))
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(sheet.root)
        sheet.scheduleActionCreate.setOnClickListener {
            dialog.dismiss()
            onCreateShift()
        }
        sheet.scheduleActionLeave.setOnClickListener {
            dialog.dismiss()
            onLeaveRequests()
        }
        dialog.show()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }
}
