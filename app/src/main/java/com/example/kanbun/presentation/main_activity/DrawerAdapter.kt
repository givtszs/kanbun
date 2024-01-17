package com.example.kanbun.presentation.main_activity

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.ItemWorkspaceBinding
import com.example.kanbun.domain.model.UserWorkspace

class DrawerAdapter(
    private val context: Context,
) : RecyclerView.Adapter<DrawerAdapter.ItemWorkspaceViewHolder>() {

    var workspaces: List<UserWorkspace> = emptyList()

    var onItemClickCallback: ((String) -> Unit)? = null

    fun setData(data: List<UserWorkspace>) {
        workspaces = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemWorkspaceViewHolder {
        return ItemWorkspaceViewHolder(
            ItemWorkspaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemWorkspaceViewHolder, position: Int) {
        holder.bind(workspaces[position])
    }

    override fun getItemCount(): Int = workspaces.size

    inner class ItemWorkspaceViewHolder(
        private val binding: ItemWorkspaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workspace: UserWorkspace) {
            binding.tvName.text = workspace.name

            binding.root.apply {
                setOnClickListener {
                    isSelected = true
                    Log.d("DrawerAdapter", "Selected workspace: $workspace")
                    // close the drawer
                    onItemClickCallback?.invoke(workspace.id)
                }
            }
        }
    }

    data class WorkspaceModel(
        val name: String,
        var isSelected: Boolean = false
    )
}