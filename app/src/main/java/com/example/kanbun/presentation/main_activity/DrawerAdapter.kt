package com.example.kanbun.presentation.main_activity

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.databinding.ItemWorkspaceBinding

class DrawerAdapter(
    private val context: Context,
    private val onItemClickCallback: () -> Unit
) : RecyclerView.Adapter<DrawerAdapter.ItemWorkspaceViewHolder>() {

    private var _workspaces: MutableList<WorkspaceModel> = mutableListOf(
        WorkspaceModel("Workspace 1"),
        WorkspaceModel("Workspace 2"),
        WorkspaceModel("Workspace 3")
    )

    fun addData(workspace: WorkspaceModel) {
        _workspaces.add(workspace)
        notifyItemInserted(workspaces.size - 1)
    }

    val workspaces: List<WorkspaceModel> = _workspaces

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemWorkspaceViewHolder {
        return ItemWorkspaceViewHolder(
            ItemWorkspaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemWorkspaceViewHolder, position: Int) {
        holder.bind(_workspaces[position])
    }

    override fun getItemCount(): Int = _workspaces.size

    inner class ItemWorkspaceViewHolder(
        val binding: ItemWorkspaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workspace: WorkspaceModel) {
            binding.tvName.text = workspace.name
            if (workspace.isSelected) {
                binding.root.setCardBackgroundColor(getColor(context, R.color.md_theme_light_secondaryContainer))
            } else {
                binding.root.setBackgroundColor(getColor(context, R.color.md_theme_light_surface))
            }

            binding.root.setOnClickListener {
                // close the drawer
                onItemClickCallback()
            }
        }
    }

    data class WorkspaceModel(
        val name: String,
        var isSelected: Boolean = false
    )
}