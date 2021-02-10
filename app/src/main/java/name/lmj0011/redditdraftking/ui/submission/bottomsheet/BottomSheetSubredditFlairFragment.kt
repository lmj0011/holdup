package name.lmj0011.redditdraftking.ui.submission.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import name.lmj0011.redditdraftking.App
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.databinding.BottomsheetFragmentSubredditFlairBinding
import name.lmj0011.redditdraftking.helpers.RedditApiHelper
import name.lmj0011.redditdraftking.helpers.RedditAuthHelper
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.models.SubredditFlair
import name.lmj0011.redditdraftking.helpers.util.launchUI
import org.kodein.di.instance

class BottomSheetSubredditFlairFragment(
    val setFlairItemForSubmission: SubredditFlairListAdapter.FlairItemClickListener,
    val removeFlairClickListner: (v: View) -> Unit,
    val flairFlowList: SharedFlow<List<SubredditFlair>>
):
    BottomSheetDialogFragment() {
    private lateinit var binding: BottomsheetFragmentSubredditFlairBinding
    private lateinit var listAdapter: SubredditFlairListAdapter
    private lateinit var redditAuthHelper: RedditAuthHelper
    private lateinit var redditApiHelper: RedditApiHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        redditAuthHelper = (requireContext().applicationContext as App).kodein.instance()
        redditApiHelper = (requireContext().applicationContext as App).kodein.instance()
        return inflater.inflate(R.layout.bottomsheet_fragment_subreddit_flair, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        val sheetContainer = requireView().parent as? ViewGroup ?: return
        sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT // makes bottomsheet fill screen
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }
    }

    private fun setupObservers() {
        binding.goBackImageView.setOnClickListener {
            this.dismiss()
        }

        binding.removeFlairButton.setOnClickListener(removeFlairClickListner)
    }

    private fun setupBinding(view: View) {
        binding = BottomsheetFragmentSubredditFlairBinding.bind(view)
        binding.lifecycleOwner = this
    }

    private fun setupRecyclerView() {
        listAdapter = SubredditFlairListAdapter(setFlairItemForSubmission)

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.subredditFlairList.addItemDecoration(decor)

        binding.subredditFlairList.adapter = listAdapter

        launchUI {
            flairFlowList.collectLatest {
                listAdapter.submitList(it)
                listAdapter.notifyDataSetChanged()
            }
        }
    }
}