package com.example.kanbun.ui.board_settings.edit_tags

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.databinding.EditTagsBottomSheetBinding
import com.example.kanbun.databinding.ItemEditTagBinding
import com.example.kanbun.domain.model.Tag
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.create_tag_dialog.CreateTagDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "EditTags"

class EditTagsBottomSheet : BottomSheetDialogFragment(), StateHandler {
    private var _binding: EditTagsBottomSheetBinding? = null
    private val binding: EditTagsBottomSheetBinding get() = _binding!!
    private val viewModel: EditTagsViewModel by viewModels()
    private var editTagsAdapter: EditTagsAdapter? = null
    private lateinit var tags: List<Tag>

    companion object {
        fun init(tags: List<Tag>): EditTagsBottomSheet {
            return EditTagsBottomSheet().apply {
                this.tags = tags
            }
        }
    }

    var onDismissCallback: (List<Tag>) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditTagsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "tags: $tags")
        setUpTagsAdapter()
        viewModel.setTags(tags)
        collectState()

        binding.btnCreateTag.setOnClickListener {
            val createTagDialog = CreateTagDialog(requireContext()) { tag ->
                viewModel.upsertTag(tag)
            }
            createTagDialog.show()
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editTagsState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.EditTagsViewState) {
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.messageShown()
            }

            editTagsAdapter?.tags = tags
        }
    }

    private fun setUpTagsAdapter() {
        editTagsAdapter = EditTagsAdapter(
            onItemClicked = { clickedTag ->
                // edit tag
                val tagEditor = CreateTagDialog(requireContext()) { tag ->
                    // update tag
                    viewModel.upsertTag(tag)
                }
                tagEditor.setTag(clickedTag)
                tagEditor.show()
            },
            onDeleteIconClicked = { tagToDelete ->
                viewModel.deleteTag(tagToDelete)
            }
        )
        binding.rvTags.adapter = editTagsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        editTagsAdapter = null
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback(viewModel.editTagsState.value.tags)
    }

    private class EditTagsAdapter(
        private val onItemClicked: (Tag) -> Unit,
        private val onDeleteIconClicked: (String) -> Unit
    ) : RecyclerView.Adapter<EditTagsAdapter.ItemEditTagViewHolder>() {

        var tags: List<Tag> = emptyList()
            set(value) {
                if (field != value) {
                    field = value
                    Log.d(TAG, "editTagsAdapter: tags: $field")
                    notifyDataSetChanged()
                }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemEditTagViewHolder {
            return ItemEditTagViewHolder(
                binding = ItemEditTagBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onItemClicked = { position ->
                    onItemClicked(tags[position])
                },
                onDeleteTagClicked = { position ->
                    onDeleteIconClicked(tags[position].id)
                }
            )
        }

        override fun onBindViewHolder(holder: ItemEditTagViewHolder, position: Int) {
            holder.bind(tags[position])
        }

        override fun getItemCount(): Int = tags.size

        class ItemEditTagViewHolder(
            private val binding: ItemEditTagBinding,
            onItemClicked: (Int) -> Unit,
            onDeleteTagClicked: (Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.apply {
                    cardBackground.setOnClickListener {
                        onItemClicked(adapterPosition)
                    }

                    btnDeleteTag.setOnClickListener {
                        onDeleteTagClicked(adapterPosition)
                    }
                }
            }

            fun bind(tag: Tag) {
                binding.apply {
                    tvTagName.text = tag.name
                    tvTagName.setTextColor(Color.parseColor(tag.color))
                    cardBackground.setCardBackgroundColor(Color.parseColor(tag.getBackgroundColor()))
                }
            }
        }
    }
}