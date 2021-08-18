package name.lmj0011.holdup.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.App
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.MainActivity
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.ListItemSubmissionBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.DateTimeHelper.getPostAtDateForListLayout
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.util.launchUI
import name.lmj0011.holdup.ui.submission.ImageSubmissionFragment
import name.lmj0011.holdup.ui.submission.LinkSubmissionFragment
import name.lmj0011.holdup.ui.submission.PollSubmissionFragment
import name.lmj0011.holdup.ui.submission.TextSubmissionFragment
import name.lmj0011.holdup.ui.submission.VideoSubmissionFragment
import org.kodein.di.instance

class SubmissionListAdapter(private val clickListener: ClickListener, private val parentFragment: Fragment): ListAdapter<Submission, SubmissionListAdapter.ViewHolder>(SubmissionDiffCallback()) {
    class ViewHolder private constructor(
        val binding: ListItemSubmissionBinding,
        val context: Context
        ) : RecyclerView.ViewHolder(binding.root){

        fun bind(clickListener: ClickListener, submission: Submission){
            binding.submission = submission
            binding.clickListener = clickListener

            binding.subredditDisplayNameTextView.text = submission.subreddit?.displayNamePrefixed
            binding.submissionTitleTextView.text = submission.title
            binding.submissionPublishDateTextView.text = getPostAtDateForListLayout(submission)
            binding.accountUsernameTextView.text = submission.account?.name


            when (submission.kind) {
                SubmissionKind.Link -> {
                    binding.submissionKindIconImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_link_24))
                }
                SubmissionKind.Image -> {
                    binding.submissionKindIconImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_image_24))
                }
                SubmissionKind.Video, SubmissionKind.VideoGif  -> {
                    binding.submissionKindIconImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_videocam_24))
                }
                SubmissionKind.Self -> {
                    binding.submissionKindIconImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_text_snippet_24))
                }
                SubmissionKind.Poll -> {
                    binding.submissionKindIconImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_baseline_poll_24))
                }
            }

            Glide
                .with(context)
                .load(submission.subreddit?.iconImgUrl)
                .circleCrop()
                .error(R.mipmap.ic_default_subreddit_icon_round)
                .into(binding.subredditIconImageView)

            Glide
                .with(context)
                .load(submission.account?.iconImage)
                .circleCrop()
                .error(R.mipmap.ic_default_subreddit_icon_round)
                .into(binding.accountIconImageView)


            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSubmissionBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class SubmissionDiffCallback : DiffUtil.ItemCallback<Submission>() {
        override fun areItemsTheSame(oldItem: Submission, newItem: Submission): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Submission, newItem: Submission): Boolean {
            return oldItem == newItem
        }
    }

    class ClickListener(val clickListener: (submission: Submission) -> Unit) {
        fun onClick(submission: Submission) = clickListener(submission)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val submission = getItem(position)
        holder.bind(clickListener, submission)
    }

    @ExperimentalCoroutinesApi
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        val dataStoreHelper: DataStoreHelper = (holder.context.applicationContext as App).kodein.instance()

        launchUI {
            when(dataStoreHelper.getSubmissionsDisplayOption().first()) {
                holder.context.resources.getString(R.string.submissions_display_option_compact_list) -> {
                    holder.binding.listItemSubmissionContentPreviewViewPager.visibility = View.GONE
                }
                holder.context.resources.getString(R.string.submissions_display_option_full_list) -> {
                    val mediaPlayer = (holder.context as MainActivity).mediaPlayer

                    holder.binding.submission?.let{ submission ->
                        val fragment = when (submission.kind) {
                            SubmissionKind.Link -> {
                                LinkSubmissionFragment.newInstance(holder.binding.submission, SubmissionFragmentChild.VIEW_MODE)
                            }
                            SubmissionKind.Image -> {
                                ImageSubmissionFragment.newInstance(holder.binding.submission, SubmissionFragmentChild.VIEW_MODE)
                            }
                            SubmissionKind.Video, SubmissionKind.VideoGif  -> {
                                VideoSubmissionFragment.newInstance(holder.binding.submission, SubmissionFragmentChild.VIEW_MODE, mediaPlayer)
                            }
                            SubmissionKind.Self -> {
                                TextSubmissionFragment.newInstance(holder.binding.submission, SubmissionFragmentChild.VIEW_MODE)
                            }
                            SubmissionKind.Poll -> {
                                PollSubmissionFragment.newInstance(holder.binding.submission, SubmissionFragmentChild.VIEW_MODE)
                            }
                            else -> throw Exception("invalid submission.kind!")
                        }

                        holder.binding.listItemSubmissionContentPreviewViewPager.adapter = object: FragmentStateAdapter(parentFragment) {
                            override fun createFragment(position: Int): Fragment {
                                parentFragment.childFragmentManager.beginTransaction().remove(fragment).commitNow()
                                return fragment
                            }

                            override fun getItemCount(): Int {
                                return 10
                            }
                        }
                    }

                    holder.binding.listItemSubmissionContentPreviewViewPager.visibility = View.VISIBLE
                    holder.binding.listItemSubmissionContentPreviewViewPager.isUserInputEnabled = false
                }
                else -> {
                    holder.binding.listItemSubmissionContentPreviewViewPager.visibility = View.GONE
                }
            }
        }

        super.onViewAttachedToWindow(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun submitList(list: MutableList<Submission>?) {
        super.submitList(filterBySubmissionPostAtMillis(list))
    }

    /**
     * Sorts a list of `SubredditWithDrafts` by Drafts that are set to be posted the soonest;
     */
    private fun filterBySubmissionPostAtMillis(list: MutableList<Submission>?): MutableList<Submission>? {
         list?.sortBy { sub ->
             sub.postAtMillis
        }

        return list?.filter { sub -> sub.postAtMillis > Keys.UNIX_EPOCH_MILLIS }?.toMutableList()
    }

}