package com.example.kanbun.ui.create_task

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kanbun.R
import com.example.kanbun.common.DATE_FORMAT
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.TaskAction
import com.example.kanbun.common.convertDateStringToTimestamp
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.common.defaultTagColors
import com.example.kanbun.common.getColor
import com.example.kanbun.databinding.AlertDialogCreateTagBinding
import com.example.kanbun.databinding.AlertDialogDatetimePickerBinding
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.databinding.ItemColorPreviewBinding
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
        val task = args.task

        binding.apply {
            // use the task from arguments to preserve the `position` property
            when (args.actionType) {
                TaskAction.ACTION_CREATE -> {
                    btnCreateTask.setOnClickListener {
                        val updatedTask = task.copy(
                            name = etName.text?.trim().toString(),
                            description = etDescription.text?.trim().toString(),
                            tags = viewModel.createTaskState.value.tags
                                .filter { it.isSelected }
                                .map { it.tag.id },
                            dateStarts = convertDateStringToTimestamp(DATE_TIME_FORMAT, tvDateStarts.text.toString()),
                            dateEnds = convertDateStringToTimestamp(DATE_TIME_FORMAT, tvDateEnds.text.toString())
                        )

                        viewModel.createTask(
                            updatedTask,
                            args.boardListInfo
                        ) {
                            navController.popBackStack()
                        }
                    }
                }

                TaskAction.ACTION_EDIT -> {
                    etName.setText(task.name)
                    etDescription.setText(task.description)

                    with(btnCreateTask) {
                        isEnabled = true
                        setOnClickListener {
                            val updatedTask = task.copy(
                                name = etName.text?.trim().toString(),
                                description = etDescription.text?.trim().toString(),
                                tags = viewModel.createTaskState.value.tags
                                    .filter { it.isSelected }
                                    .map { it.tag.id },
                                dateStarts = convertDateStringToTimestamp(DATE_TIME_FORMAT, tvDateStarts.text.toString()),
                                dateEnds = convertDateStringToTimestamp(DATE_TIME_FORMAT, tvDateEnds.text.toString())
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

            tvDateStarts.apply {
                task.dateStarts?.let {
                    setText(convertTimestampToDateString(DATE_TIME_FORMAT, it))
                }

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        Log.d(TAG, "tvDateStarts is clicked")
                        buildDateTimePickerDialog(binding.tvDateStarts)
                    }
                }
            }

            tvDateEnds.apply {
                task.dateEnds?.let {
                    setText(convertTimestampToDateString(DATE_TIME_FORMAT, it))
                }

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        Log.d(TAG, "tvDateEnds is clicked")
                        buildDateTimePickerDialog(binding.tvDateEnds)
                    }
                }
            }
        }
    }

    private fun buildCreateTagDialog() {
        var tagColor = ""
        var alertDialogBinding: AlertDialogCreateTagBinding? =
            AlertDialogCreateTagBinding.inflate(layoutInflater, null, false)
        var colorPickerAdapter: ColorPickerAdapter? = ColorPickerAdapter { colorId ->
            tagColor = colorId
        }

        with(alertDialogBinding!!.rvTagColors) {
            adapter = colorPickerAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create tag")
            .setView(alertDialogBinding.root)
            .setCancelable(false)
            .setPositiveButton("Create") { _, _ ->
                viewModel.createTag(
                    name = alertDialogBinding!!.etName.text?.trim().toString(),
                    color = tagColor,
                    boardListInfo = args.boardListInfo
                )
                alertDialogBinding = null
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                colorPickerAdapter = null
                alertDialogBinding = null
                dialog.cancel()
            }
            .create()
            .apply {
                setOnShowListener {
                    val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        isEnabled = false
                    }
                    alertDialogBinding!!.etName.doOnTextChanged { _, _, _, count ->
                        positiveButton.isEnabled = count > 0
                    }
                }
            }
            .show()
    }

    private fun buildDateTimePickerDialog(textField: AutoCompleteTextView) {
//        fun close(dialogBinding: AlertDialogDatetimePickerBinding, dialog) {
//            alertDialogBinding = null
//            binding.tvDateStarts.clearFocus()
//            dialog.cancel()
//        }
//
        var alertDialogBinding: AlertDialogDatetimePickerBinding? =
            AlertDialogDatetimePickerBinding.inflate(layoutInflater, null, false)

        alertDialogBinding?.let { dialogBinding ->
            val date = textField.text.toString().substringBefore(",")
            val time = textField.text.toString().substringAfter(", ")

            dialogBinding.tvSelectedDate.apply {
                if (date.isNotEmpty()) {
                    setText(date)
                }

                setOnClickListener {
                    buildDatePicker(dialogBinding, dialogBinding.tvSelectedDate.text.toString())
                }
            }

            dialogBinding.tvSelectedTime.apply {
                if (time.isNotEmpty()) {
                    setText(time)
                }

                setOnClickListener {
                    buildTimePicker(dialogBinding, dialogBinding.tvSelectedTime.text.toString())
                }
            }

            MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setPositiveButton("Save") { _, _ ->
                    textField.setText(
                        resources.getString(
                            R.string.date_time,
                            dialogBinding.tvSelectedDate.text,
                            dialogBinding.tvSelectedTime.text
                        )
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setOnDismissListener {
                    alertDialogBinding = null
                    textField.clearFocus()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE).also {
                            it.isEnabled = false
                        }

                        dialogBinding.apply {
                            val _isDateSelected = MutableStateFlow(tvSelectedDate.text.isNotEmpty())
                            val _isTimeSelected = MutableStateFlow(tvSelectedTime.text.isNotEmpty())
                            lifecycleScope.launch {
                                combine(
                                    _isDateSelected,
                                    _isTimeSelected
                                ) { isDateSelected, isTimeSelected ->
                                    isDateSelected && isTimeSelected
                                }.collectLatest { isDateTimeSelected ->
                                    positiveButton.isEnabled = isDateTimeSelected
                                }
                            }

                            tvSelectedDate.doAfterTextChanged {
                                Log.d(TAG, "date has changed: $it")
                                _isDateSelected.value = it?.isNotEmpty() == true
                            }

                            tvSelectedTime.doAfterTextChanged {
                                Log.d(TAG, "time has changed: $it")
                                _isTimeSelected.value = it?.isNotEmpty() == true
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun buildDatePicker(
        alertDialogBinding: AlertDialogDatetimePickerBinding,
        date: String
    ) {
        val dateTimestamp = convertDateStringToTimestamp(DATE_FORMAT, date)

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraints)
            .setSelection(dateTimestamp ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener {
            alertDialogBinding.tvSelectedDate.setText(
               convertTimestampToDateString(DATE_FORMAT, it)
            )
        }
        datePicker.show(childFragmentManager, "date_picker")
    }

    private fun buildTimePicker(
        alertDialogBinding: AlertDialogDatetimePickerBinding,
        time: String,
    ) {
        fun addZero(time: Int): String {
            return if (time < 10) {
                "0$time"
            } else {
                time.toString()
            }
        }

        val timePickerBuilder = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setInputMode(INPUT_MODE_CLOCK)

        if (time.isNotEmpty()) {
            Log.d(TAG, "buildTimePicker: initTime: $time")
            timePickerBuilder.setHour(time.substringBefore(":").toInt())
            timePickerBuilder.setMinute(time.substringAfter(":").toInt())
        }

        val timePicker = timePickerBuilder.build()
        timePicker.addOnPositiveButtonClickListener {
            val hour = addZero(timePicker.hour)
            val minute = addZero(timePicker.minute)
            alertDialogBinding.tvSelectedTime.setText("$hour:$minute")
        }

        timePicker.show(childFragmentManager, "time_picker")
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

                if (tags.isNotEmpty()) {
                    tagsAdapter = TagsAdapter(areItemsClickable = true).also {
                        it.tags = tags
                    }
                    rvTags.adapter = tagsAdapter
                }

                message?.let {
                    showToast(it)
                    viewModel.messageShown()
                }
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
