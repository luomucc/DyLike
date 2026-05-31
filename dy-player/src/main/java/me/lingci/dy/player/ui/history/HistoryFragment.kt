package me.lingci.dy.player.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import me.lingci.lib.base.util.JsonUtil
import me.lingci.lib.base.util.Log
import me.lingci.dy.player.databinding.FragmentHistoryBinding
import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.util.SpUtil

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val homeViewModel: HistoryViewModel by activityViewModels()
    private val spUtil by lazy { SpUtil(requireContext()) }
    private lateinit var mHistoryItemAdapter: HistoryItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        init()
        return binding.root
    }

    private fun init() {
        homeViewModel.text.observe(viewLifecycleOwner) {
            if (it.isNullOrBlank()) {
                binding.textHome.visibility = View.GONE
            } else {
                binding.textHome.visibility = View.VISIBLE
            }
            binding.textHome.text = it
        }
        initView()
    }

    private fun initView() {
        val list = JsonUtil.toList<VideoData>(spUtil.historyJson!!).toMutableList()
        if (list.isEmpty()) {
            homeViewModel.setText("尚无记录，添加资源库使用")
        }
        list.reverse()
        mHistoryItemAdapter = HistoryItemAdapter(list)
        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.adapter = mHistoryItemAdapter
        mHistoryItemAdapter.onItemClick { item, position ->
            Log.d(this@HistoryFragment, position, item)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refresh() {
        homeViewModel.setText("")
        val list = JsonUtil.toList<VideoData>(spUtil.historyJson!!).toMutableList()
        list.reverse()
        mHistoryItemAdapter.updateData(ArrayList())
    }
}