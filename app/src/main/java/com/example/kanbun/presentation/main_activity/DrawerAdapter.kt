package com.example.kanbun.presentation.main_activity

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

    var prevSelectedPosition: Int? = null

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
                binding.constraintLayout.setBackgroundColor(getColor(context, R.color.md_theme_light_secondaryContainer))
            } else {
                binding.constraintLayout.setBackgroundColor(getColor(context, R.color.md_theme_light_surface))
            }

            binding.root.apply {
                setOnClickListener {
                    if (position != prevSelectedPosition) {
                        item.isSelected = true
                        prevSelectedPosition?.let { workspaces[it].isSelected = true }
                        Log.d("DrawerAdapter", "Selected workspace: $item")

                        // close the drawer
                        onItemClickCallback?.invoke(item.workspace.id)
                    }

                    prevSelectedPosition = position
                }
            }
        }
    }

    data class DrawerWorkspace(
        val workspace: UserWorkspace,
        var isSelected: Boolean = false
    )
}