package com.example.kanbun.ui.main_activity

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.ItemWorkspaceBinding
import com.example.kanbun.domain.model.UserWorkspace

class DrawerAdapter(
    private val context: Context,
) : RecyclerView.Adapter<DrawerAdapter.ItemWorkspaceViewHolder>() {

    var workspaces: List<DrawerWorkspace> = emptyList()

    var onItemClickCallback: ((String) -> Unit)? = null

    var prevSelectedWorkspaceId: String? = null

    fun setData(data: List<DrawerWorkspace>) {
        workspaces = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemWorkspaceViewHolder {
        return ItemWorkspaceViewHolder(
            ItemWorkspaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemWorkspaceViewHolder, position: Int) {
        holder.bind(workspaces[position], position)
    }

    override fun getItemCount(): Int = workspaces.size

    inner class ItemWorkspaceViewHolder(
        private val binding: ItemWorkspaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DrawerWorkspace, position: Int) {
            binding.tvName.text = item.workspace.name

            if (item.isSelected) {
                binding.apply {
                    constraintLayout.setBackgroundColor(getColor(context, R.color.md_theme_light_secondaryContainer))
                    tvName.setTextColor(getColor(context, R.color.md_theme_light_onSecondaryContainer))
                    ivLeadingIcon.isSelected = true
                }
            } else {
                binding.apply {
                    constraintLayout.setBackgroundColor(getColor(context, R.color.md_theme_light_surface))
                    tvName.setTextColor(getColor(context, R.color.md_theme_light_onSurfaceVariant))
                    ivLeadingIcon.isSelected = false
                }
            }

            binding.root.apply {
                setOnClickListener {
                    if (item.workspace.id != prevSelectedWorkspaceId) {
                        Log.d("DrawerAdapter", "Selected workspace: $item")
                        // close the drawer
                        onItemClickCallback?.invoke(item.workspace.id)
                    }

                    prevSelectedWorkspaceId = item.workspace.id
                }
            }
        }
    }

    data class DrawerWorkspace(
        val workspace: UserWorkspace,
        var isSelected: Boolean = false
    )
}