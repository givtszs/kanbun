package com.example.kanbun.ui.task_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.kanbun.R
import com.example.kanbun.common.TaskAction
import com.example.kanbun.databinding.FragmentTaskDetailsBinding
import com.example.kanbun.ui.BaseFragment

class TaskDetailsFragment : BaseFragment() {
    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding: FragmentTaskDetailsBinding get() = _binding!!
    private val args: TaskDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar)
    }

    override fun setUpListeners() {
        val task = args.task
        binding.apply {
            tvName.text = task.name
            tvCreatedBy.text = resources.getString(R.string.created_by, task.author)
            tvDescription.text =
                task.description.ifEmpty { resources.getString(R.string.no_description) }

            if (task.tags.isNotEmpty()) {
                tvNoTags.visibility = View.GONE
                // set up recyclerview
                rvTags.visibility = View.VISIBLE
            } else {
                tvNoTags.visibility = View.VISIBLE
                rvTags.visibility = View.GONE
            }

            if (task.members.isNotEmpty()) {
                tvNoMembers.visibility = View.GONE
                // set up recyclerview
                rvMembers.visibility = View.VISIBLE
            } else {
                tvNoMembers.visibility = View.VISIBLE
                rvMembers.visibility = View.GONE
            }

            tvMembers.text = resources.getString(R.string.task_members, task.members.size)

            tvDateStarts.text = task.dateStarts.ifEmpty { resources.getString(R.string.no_date_time) }
            tvDateEnds.text = task.dateStarts.ifEmpty { resources.getString(R.string.no_date_time) }

            fabEditTask.setOnClickListener {
                navController.navigate(TaskDetailsFragmentDirections.actionTaskDetailsFragmentToCreateTaskFragment(
                    actionType = TaskAction.ACTION_EDIT,
                    task = task,
                    boardListInfo = args.boardListInfo
                ))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}