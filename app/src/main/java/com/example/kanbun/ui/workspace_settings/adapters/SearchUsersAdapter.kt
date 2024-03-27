package com.example.kanbun.ui.workspace_settings.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.loadUserProfilePicture
import com.example.kanbun.databinding.ItemUserSearchResultBinding
import com.example.kanbun.domain.model.User

class SearchUsersAdapter(
    private val onItemClicked: (User) -> Unit
): RecyclerView.Adapter<SearchUsersAdapter.ItemFoundUserViewHolder>() {
    private val TAG = "SearchUsersAdapter"

    var users: List<User> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
            Log.d(TAG, "setUsers: $value")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFoundUserViewHolder {
        return ItemFoundUserViewHolder(
            binding = ItemUserSearchResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) { position ->
            onItemClicked(users[position])
        }
    }

    override fun onBindViewHolder(holder: ItemFoundUserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    class ItemFoundUserViewHolder(
        val binding: ItemUserSearchResultBinding,
        private val clickAtPosition: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }

        fun bind(user: User) {
            binding.apply {
                tvName.text = user.name
                tvTag.text = itemView.context.getString(R.string.user_tag, user.tag)
                loadUserProfilePicture(
                    context = itemView.context,
                    pictureUrl = user.profilePicture,
                    view = ivProfilePicture
                )
//                ivIconAdded.isVisible = if ()

                // check if user is already in the workspace
            }
        }

    }
}