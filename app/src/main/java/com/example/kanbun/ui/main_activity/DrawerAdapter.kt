package com.example.kanbun.ui.main_activity

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.ItemWorkspaceBinding
import com.example.kanbun.databinding.ItemWorkspacesLabelBinding
import com.example.kanbun.domain.model.WorkspaceInfo

class DrawerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TAG = "DrawerAdapter"
        private const val VIEW_TYPE_WORKSPACE_ITEM = 0
        private const val VIEW_TYPE_USER_WORKSPACES_LABEL = 1
        private const val VIEW_TYPE_SHARED_WORKSPACES_LABEL = 2
    }

    var prevSelectedWorkspaceId: String? = null
    var userWorkspaces: List<DrawerWorkspace> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
                Log.d(TAG, "userWorkspaces: $field")
            }
        }

    var sharedWorkspaces: List<DrawerWorkspace> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
                Log.d(TAG, "sharedWorkspaces: $field")

            }
        }

    var onItemClickCallback: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_WORKSPACE_ITEM -> {
                ItemWorkspaceViewHolder(
                    ItemWorkspaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )
            }

            VIEW_TYPE_USER_WORKSPACES_LABEL -> {
                WorkspacesLabelViewHolder(
                    binding = ItemWorkspacesLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    label = "My workspaces"
                )
            }

            VIEW_TYPE_SHARED_WORKSPACES_LABEL -> {
                WorkspacesLabelViewHolder(
                    binding = ItemWorkspacesLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    label = "Shared workspaces"
                )
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemWorkspaceViewHolder) {
            if (userWorkspaces.isNotEmpty()) {
                if (position > 0 && position <= userWorkspaces.size) {
                    holder.bind(userWorkspaces[position - 1])
                } else if (sharedWorkspaces.isNotEmpty() && position > userWorkspaces.size + 1) {
                    holder.bind(sharedWorkspaces[position - (userWorkspaces.size + 2)])
                }
            } else if (sharedWorkspaces.isNotEmpty() && position > 0 && position <= sharedWorkspaces.size) {
                holder.bind(sharedWorkspaces[position - 1])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        var itemViewType = 0
        if (userWorkspaces.isNotEmpty()) {
            if (position == 0) {
                itemViewType = VIEW_TYPE_USER_WORKSPACES_LABEL
            } else if (position > 0 && position <= userWorkspaces.size) {
                itemViewType = VIEW_TYPE_WORKSPACE_ITEM
            }

            if (sharedWorkspaces.isNotEmpty()) {
                if (position == userWorkspaces.size + 1) {
                    itemViewType = VIEW_TYPE_SHARED_WORKSPACES_LABEL
                } else if (position > userWorkspaces.size + 1) {
                    itemViewType = VIEW_TYPE_WORKSPACE_ITEM
                }
            }
        } else if (sharedWorkspaces.isNotEmpty()) {
            if (position == 0) {
                itemViewType = VIEW_TYPE_SHARED_WORKSPACES_LABEL
            } else if (position > 0 && position <= sharedWorkspaces.size) {
                itemViewType = VIEW_TYPE_WORKSPACE_ITEM
            }
        } else {
            throw IllegalArgumentException("Could not resolve item view type at the position $position")
        }
        Log.d(TAG, "getItemViewType: position: $position, ")
        return itemViewType
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount is called")
        var itemCount = 0
        if (userWorkspaces.isNotEmpty()) {
            itemCount += userWorkspaces.size + 1
        }

        if (sharedWorkspaces.isNotEmpty()) {
            itemCount += sharedWorkspaces.size + 1
        }
        return itemCount
    }

    inner class ItemWorkspaceViewHolder(
        private val binding: ItemWorkspaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DrawerWorkspace) {
            binding.apply {
                tvName.text = item.workspace.name

                if (item.isSelected) {
                    constraintLayout.setBackgroundColor(
                        getColor(
                            itemView.context,
                            R.color.md_theme_light_secondaryContainer
                        )
                    )
                    tvName.setTextColor(
                        getColor(
                            itemView.context,
                            R.color.md_theme_light_onSecondaryContainer
                        )
                    )
                    ivLeadingIcon.isSelected = true
                } else {
                    constraintLayout.setBackgroundColor(
                        getColor(
                            itemView.context,
                            R.color.md_theme_light_surface
                        )
                    )
                    tvName.setTextColor(
                        getColor(
                            itemView.context,
                            R.color.md_theme_light_onSurfaceVariant
                        )
                    )
                    ivLeadingIcon.isSelected = false
                }

                root.setOnClickListener {
                    if (item.workspace.id != prevSelectedWorkspaceId) {
                        Log.d(TAG, "Selected workspace: $item")
                        // close the drawer
                        onItemClickCallback?.invoke(item.workspace.id)
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

    class WorkspacesLabelViewHolder(
        val binding: ItemWorkspacesLabelBinding,
        label: String
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvWorkspacesHeadline.text = label
        }
    }
}