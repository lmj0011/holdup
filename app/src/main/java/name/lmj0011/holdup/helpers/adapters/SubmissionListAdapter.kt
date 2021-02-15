package name.lmj0011.holdup.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import name.lmj0011.holdup.Keys
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.ListItemSubmissionBinding
import name.lmj0011.holdup.helpers.DateTimeHelper.getPostAtDateForListLayout
import name.lmj0011.holdup.helpers.enums.SubmissionKind

class SubmissionListAdapter(private val clickListener: ClickListener): ListAdapter<Submission, SubmissionListAdapter.ViewHolder>(SubmissionDiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemSubmissionBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){
        fun bind(clickListener: ClickListener, submission: Submission){
            binding.submission = submission
            binding.clickListener = clickListener

            binding.subredditDisplayNameTextView.text = submission.subreddit?.displayNamePrefixed
            binding.submissionTitleTextView.text = submission.title
            binding.submissionPublishDateTextView.text = getPostAtDateForListLayout(submission)
            binding.accountUsernameTextView.text = submission.account?.name

            when (submission.kind) {
                SubmissionKind.Link -> {
                    binding.submissionKindIconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_link_24))
                }
                SubmissionKind.Image -> {
                    binding.submissionKindIconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_image_24))
                }
                SubmissionKind.Video -> {
                    binding.submissionKindIconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_videocam_24))
                }
                SubmissionKind.Self -> {
                    binding.submissionKindIconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_text_snippet_24))
                }
                SubmissionKind.Poll -> {
                    binding.submissionKindIconImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_poll_24))
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun submitList(list: MutableList<Submission>?) {
        super.submitList(filterBySubmissionPostAtMillis(list))
    }

    /**
     * Sorts a list of `SubredditWithDrafts` by Drafts that are set to be posted the soonest;
     * also excludes `SubredditWithDrafts` with 0 `Drafts`
     */
    private fun filterBySubmissionPostAtMillis(list: MutableList<Submission>?): MutableList<Submission>? {
         list?.sortBy { sub ->
             sub.postAtMillis
        }

        return list?.filter { sub -> sub.postAtMillis > Keys.UNIX_EPOCH_MILLIS }?.toMutableList()
    }

}