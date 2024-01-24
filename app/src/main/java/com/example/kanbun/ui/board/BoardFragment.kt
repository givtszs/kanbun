package com.example.kanbun.ui.board

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.kanbun.databinding.FragmentBoardBinding
import com.example.kanbun.domain.model.Workspace
import com.example.kanbun.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "BoardFragm"

@AndroidEntryPoint
class BoardFragment : BaseFragment() {
    private var _binding: FragmentBoardBinding? = null
    private val binding: FragmentBoardBinding get() = _binding!!
    private val viewModel: BoardViewModel by viewModels()
    private val args: BoardFragmentArgs by navArgs()
    private lateinit var boardInfo: Workspace.BoardInfo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardBinding.inflate(inflater, container, false)
        boardInfo = args.boardInfo
        Log.d(TAG, "boardInfo: $boardInfo")
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpActionBar(binding.topAppBar.toolbar, boardInfo.name)
    }

    override fun setUpListeners() {
//        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}