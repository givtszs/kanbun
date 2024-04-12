package com.example.kanbun.ui.manage_members

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.loadProfilePicture
import com.example.kanbun.databinding.ItemUserSearchResultBinding
import com.example.kanbun.domain.model.User
import com.example.kanbun.ui.model.UserSearchResult

class SearchUsersAdapter(
    private val onItemClicked: (User) -> Unit
) : RecyclerView.Adapter<SearchUsersAdapter.ItemFoundUserViewHolder>() {
    private val TAG = "SearchUsersAdapter"

    var users: List<UserSearchResult> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
                Log.d(TAG, "setUsers: $value")
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFoundUserViewHolder {
        return ItemFoundUserViewHolder(
            binding = ItemUserSearchResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ) { position ->
            onItemClicked(users[position].user)
        }
    }

    override fun onBindViewHolder(holder: ItemFoundUserViewHolder, position: Int) {
        val user = users[position]
        Log.d(TAG, "bind: isUserAdded: ${user.isAdded}")
        holder.bind(
            user = user.user,
            isAdded = user.isAdded
        )
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

        fun bind(user: User, isAdded: Boolean) {
            binding.apply {
                tvName.text = user.name
                tvTag.text = itemView.context.getString(R.string.user_tag, user.tag)
                loadProfilePicture(
                    context = itemView.context,
                    pictureUrl = user.profilePicture,
                    view = ivProfilePicture
                )
                ivIconAdded.isVisible = isAdded
            }
        }

    }
}