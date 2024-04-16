package com.example.kanbun.ui.create_task

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.kanbun.R
import com.example.kanbun.common.DATE_FORMAT
import com.example.kanbun.common.DATE_TIME_FORMAT
import com.example.kanbun.common.TaskAction
import com.example.kanbun.common.convertDateStringToTimestamp
import com.example.kanbun.common.convertTimestampToDateString
import com.example.kanbun.databinding.AlertDialogDatetimePickerBinding
import com.example.kanbun.databinding.FragmentCreateTaskBinding
import com.example.kanbun.domain.model.BoardList
import com.example.kanbun.domain.model.BoardListInfo
import com.example.kanbun.domain.model.Task
import com.example.kanbun.ui.BaseFragment
import com.example.kanbun.ui.StateHandler
import com.example.kanbun.ui.ViewState
import com.example.kanbun.ui.board.common_adapters.TagsAdapter
import com.example.kanbun.ui.create_tag_dialog.CreateTagDialog
import com.example.kanbun.ui.main_activity.MainActivity
import com.example.kanbun.ui.manage_members.MembersAdapter
import com.example.kanbun.ui.manage_members.SearchUsersAdapter
import com.example.kanbun.ui.members.MembersBottomSheet
import com.example.kanbun.ui.model.Member
import com.example.kanbun.ui.shared.SharedBoardViewModel
import com.example.kanbun.ui.task_editor.TaskEditorFragment
import com.example.kanbun.ui.task_editor.TaskEditorViewModel
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

private const val TAG = "CreateTaskFragment"

@AndroidEntryPoint
class CreateTaskFragment : TaskEditorFragment(){
    private val args: CreateTaskFragmentArgs by navArgs()
    override val boardList: BoardList by lazy {
        args.boardList
    }
    override val task: Task? by lazy {
        args.task
    }
    override val viewModel: CreateTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar, "Create task")
    }

    override fun setUpListeners() {
        super.setUpListeners()
        binding.btnCreateTask.text = getString(R.string.create_task)
        binding.btnCreateTask.setOnClickListener {
            viewModel.createTask(
                createTask(),
                boardList
            ) {
                navController.popBackStack()
            }
        }
    }
}
