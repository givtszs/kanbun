package com.example.kanbun.ui.main_activity

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.ItemWorkspaceBinding
import com.example.kanbun.domain.model.WorkspaceInfo

class DrawerAdapter : RecyclerView.Adapter<DrawerAdapter.ItemWorkspaceViewHolder>() {
    companion object {
        private const val TAG = "DrawerAdapter"
        var prevSelectedWorkspaceId: String? = null
        var onItemClickCallback: ((String) -> Unit) = {}
    }

    var workspaces: List<DrawerWorkspace> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
                Log.d(TAG, "userWorkspaces: $field")
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemWorkspaceViewHolder {
        return ItemWorkspaceViewHolder(
            ItemWorkspaceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemWorkspaceViewHolder, position: Int) {
        holder.bind(workspaces[position])
    }

    override fun getItemCount(): Int = workspaces.size

    inner class ItemWorkspaceViewHolder(
        private val binding: ItemWorkspaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DrawerWorkspace) {
            binding.apply {
                tvName.text = item.workspace.name

                with(item.isSelected) {
                    cardBackground.isSelected = this
                    tvName.isSelected = this
                    ivLeadingIcon.isSelected = this
                }

                card.setOnClickListener {
                    if (item.workspace.id != prevSelectedWorkspaceId) {
                        Log.d(TAG, "Selected workspace: $item")
                        // close the drawer
                        onItemClickCallback.invoke(item.workspace.id)
                        prevSelectedWorkspaceId = item.workspace.id
                    }
                }
            }
        }

    }

    data class DrawerWorkspace(
        val workspace: WorkspaceInfo,
        var isSelected: Boolean = false
    )
}