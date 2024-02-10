package com.example.kanbun.ui.create_task

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.TaskAction
import com.example.kanbun.common.defaultTagColors
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.AlertDialogCreateTagBinding
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.databinding.ItemColorPreviewBinding
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "CreateTaskFragment"

@AndroidEntryPoint
class CreateTaskFragment : BaseFragment(), StateHandler {
    private var _binding: FragmentCreateTaskBinding? = null
    private val binding: FragmentCreateTaskBinding get() = _binding!!
    private val viewModel: CreateTaskViewModel by viewModels()
    private val args: CreateTaskFragmentArgs by navArgs()

    //    private var alertBinding: AlertDialogCreateTagBinding? = null
    private var tagsAdapter: TagsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (args.actionType) {
            TaskAction.ACTION_CREATE -> {
                setUpActionBar(binding.topAppBar.toolbar, "Create task")
            }

            TaskAction.ACTION_EDIT -> {
                setUpActionBar(binding.topAppBar.toolbar, "Edit task")
            }
        }
        viewModel.init(args.task, args.boardListInfo, args.actionType)
        collectState()
    }

    override fun setUpListeners() {
        binding.apply {
            when (args.actionType) {
                TaskAction.ACTION_CREATE -> {
                    btnCreateTask.setOnClickListener {
                        viewModel.createTask(
                            // use the task from arguments to preserve the `position` property
                            args.task.copy(
                                name = etName.text?.trim().toString(),
                                description = etDescription.text?.trim().toString(),
                                author = viewModel.firebaseUser?.uid!!,
                                tags = viewModel.createTaskState.value.tags
                                    .filter { it.isSelected }
                                    .map { it.tag.id }
                            ),
                            args.boardListInfo
                        ) {
                            navController.popBackStack()
                        }
                    }
                }

                TaskAction.ACTION_EDIT -> {
                    val task = args.task

                    etName.setText(task.name)
                    etDescription.setText(task.description)

                    with(btnCreateTask) {
                        isEnabled = true
                        setOnClickListener {
                            val updatedTask = task.copy(
                                name = etName.text?.trim().toString(),
                                description = etDescription.text?.trim().toString()
                            )

                            viewModel.editTask(updatedTask, args.boardListInfo) {
                                navController.navigate(
                                    CreateTaskFragmentDirections.actionCreateTaskFragmentToTaskDetailsFragment(
                                        task = updatedTask,
                                        boardListInfo = args.boardListInfo
                                    )
                                )
                            }
                        }
                    }
                }
            }

            etName.doOnTextChanged { _, _, _, count ->
                Log.d(TAG, "etName: count: $count")
                btnCreateTask.isEnabled = count > 0
            }

            tvCreateTag.setOnClickListener {
                buildCreateTagDialog()
            }

            // set up tags recycler view
            tagsAdapter = TagsAdapter(areItemsClickable = true)
            rvTags.adapter = tagsAdapter
        }
    }

    private fun buildCreateTagDialog() {
        var tagColor = ""
        var alertBinding: AlertDialogCreateTagBinding? =
            AlertDialogCreateTagBinding.inflate(layoutInflater, null, false)
        var colorPickerAdapter: ColorPickerAdapter? = ColorPickerAdapter { colorId ->
            tagColor = colorId
        }

        alertBinding!!.apply {
            gridColors.adapter = colorPickerAdapter
            gridColors.layoutManager = GridLayoutManager(requireContext(), 4)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create tag")
                .setView(root)
                .setCancelable(false)
                .setPositiveButton("Create") { _, _ ->
                    viewModel.createTag(
                        name = etName.text?.trim().toString(),
                        color = tagColor,
                        boardListInfo = args.boardListInfo
                    )
                    alertBinding = null
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    colorPickerAdapter = null
                    alertBinding = null
                    dialog.cancel()
                }
                .create()
                .apply {
                    setOnShowListener {
                        val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).apply {
                            isEnabled = false
                        }
                        alertBinding!!.etName.doOnTextChanged { _, _, _, count ->
                            positiveButton.isEnabled = count > 0
                        }
                    }
                }
                .show()
        }
    }

    override fun collectState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createTaskState.collectLatest {
                    processState(it)
                }
            }
        }
    }

    override fun processState(state: ViewState) {
        with(state as ViewState.CreateTaskViewState) {
            binding.apply {
                if (loadingManager.isUpsertingTask) {
                    btnCreateTask.text = ""
                    btnCreateTask.isEnabled = false
                    pbCreatingTask.isVisible = true
                } else {
                    btnCreateTask.text = if (args.actionType == TaskAction.ACTION_CREATE) {
                        resources.getString(R.string.create_task)
                    } else {
                        resources.getString(R.string.save)
                    }
                    btnCreateTask.isEnabled =
                        etName.text?.trim().toString().isNotEmpty()
                    pbCreatingTask.isVisible = false
                }

                pbLoadingTags.isVisible = loadingManager.isLoadingTags
            }

            if (tags.isNotEmpty()) {
                tagsAdapter?.tags = tags
            }

            message?.let {
                showToast(it)
                viewModel.messageShown()
            }
        }
    }

    private class ColorPickerAdapter(private val onItemClicked: (String) -> Unit) :
        RecyclerView.Adapter<ColorPickerAdapter.ItemColorPreviewViewHolder>() {

        init {
            ItemColorPreviewViewHolder.prevSelectedPos = -1
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ItemColorPreviewViewHolder {
            return ItemColorPreviewViewHolder(
                ItemColorPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ) { position ->
                if (ItemColorPreviewViewHolder.prevSelectedPos != position) {
                    notifyItemChanged(ItemColorPreviewViewHolder.prevSelectedPos)
                    ItemColorPreviewViewHolder.prevSelectedPos = position
                    notifyItemChanged(position)
                    onItemClicked(defaultTagColors[position])
                }
            }
        }

        override fun onBindViewHolder(holder: ItemColorPreviewViewHolder, position: Int) {
            holder.bind(defaultTagColors[position])
        }

        override fun getItemCount(): Int = defaultTagColors.size

        class ItemColorPreviewViewHolder(
            private val binding: ItemColorPreviewBinding,
            private val clickAtPosition: (Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            companion object {
                var prevSelectedPos = -1
            }

            init {
                binding.cardColor.setOnClickListener {
                    if (prevSelectedPos != adapterPosition) {
                        clickAtPosition(adapterPosition)
                    }
                }
            }

            fun bind(hexColorValue: String) {
                binding.apply {
                    cardColor.isSelected = adapterPosition == prevSelectedPos
                    if (cardColor.isSelected) {
                        cardColor.strokeColor =
                            getColor(itemView.context, R.color.md_theme_light_primary)
                    } else {
                        cardColor.strokeColor =
                            getColor(itemView.context, R.color.md_theme_light_outlineVariant)
                    }

                    cardColor.setCardBackgroundColor(Color.parseColor(hexColorValue))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        alertBinding = null
        tagsAdapter = null
        _binding = null
    }
}
