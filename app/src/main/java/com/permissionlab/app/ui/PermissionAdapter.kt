package com.permissionlab.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.permissionlab.app.R
import com.permissionlab.app.model.PermissionItem
import com.permissionlab.app.model.PermissionStatus

class PermissionAdapter(
    private val context: Context,
    private val items: List<PermissionItem>,
    private val onToggleClick: (PermissionItem, Boolean) -> Unit,
    private val onConfigureClick: (PermissionItem) -> Unit,
    private val onTestClick: (PermissionItem, Int) -> Unit
) : RecyclerView.Adapter<PermissionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvPermissionTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvPermissionCategory)
        val tvDescription: TextView = view.findViewById(R.id.tvPermissionDescription)
        val layoutStatusChip: LinearLayout = view.findViewById(R.id.layoutStatusChip)
        val ivStatusIcon: ImageView = view.findViewById(R.id.ivStatusIcon)
        val tvStatusText: TextView = view.findViewById(R.id.tvStatusText)
        val switchPermission: MaterialSwitch = view.findViewById(R.id.switchPermission)
        val layoutTestResult: LinearLayout = view.findViewById(R.id.layoutTestResult)
        val tvTestOutput: TextView = view.findViewById(R.id.tvTestOutput)
        val btnConfigure: MaterialButton = view.findViewById(R.id.btnConfigure)
        val btnTest: MaterialButton = view.findViewById(R.id.btnTest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvCategory.text = item.category
        holder.tvDescription.text = item.description

        // Status chip styling
        when (item.status) {
            PermissionStatus.GRANTED -> {
                holder.layoutStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_granted_bg))
                holder.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_granted))
                holder.tvStatusText.text = context.getString(R.string.status_granted)
                holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_granted))
            }
            PermissionStatus.DENIED -> {
                holder.layoutStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_denied_bg))
                holder.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_denied))
                holder.tvStatusText.text = context.getString(R.string.status_denied)
                holder.ivStatusIcon.setImageResource(R.drawable.ic_cancel)
                holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_denied))
            }
            PermissionStatus.RESTRICTED -> {
                holder.layoutStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_restricted_bg))
                holder.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_restricted))
                holder.tvStatusText.text = context.getString(R.string.status_restricted)
                holder.ivStatusIcon.setImageResource(R.drawable.ic_warning)
                holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_restricted))
            }
            PermissionStatus.NOT_APPLICABLE -> {
                holder.layoutStatusChip.setBackgroundColor(ContextCompat.getColor(context, R.color.status_na_bg))
                holder.tvStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_na))
                holder.tvStatusText.text = context.getString(R.string.status_not_applicable)
                holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_na))
            }
        }

        // Toggle Switch binding: reflects actual granted status
        val isGranted = (item.status == PermissionStatus.GRANTED)
        holder.switchPermission.isChecked = isGranted

        // When user interacts with the toggle switch
        holder.switchPermission.setOnClickListener {
            val isChecked = holder.switchPermission.isChecked
            if (!isGranted) {
                // If not currently granted, reset the switch UI to unchecked
                // until user actually grants the permission from the requested prompt
                holder.switchPermission.isChecked = false
            }
            onToggleClick(item, isChecked)
        }

        // Test result box
        if (!item.testResult.isNullOrEmpty()) {
            holder.layoutTestResult.visibility = View.VISIBLE
            holder.tvTestOutput.text = item.testResult
            if (item.testResult?.startsWith("[SUCCESS]") == true) {
                holder.tvTestOutput.setTextColor(ContextCompat.getColor(context, R.color.status_granted))
            } else if (item.testResult?.startsWith("[FAILED]") == true) {
                holder.tvTestOutput.setTextColor(ContextCompat.getColor(context, R.color.status_denied))
            } else {
                holder.tvTestOutput.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            }
        } else {
            holder.layoutTestResult.visibility = View.GONE
        }

        holder.btnConfigure.setOnClickListener {
            onConfigureClick(item)
        }

        holder.btnTest.setOnClickListener {
            onTestClick(item, position)
        }
    }
}
