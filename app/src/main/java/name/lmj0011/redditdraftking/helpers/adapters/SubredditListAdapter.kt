package name.lmj0011.redditdraftking.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import name.lmj0011.redditdraftking.Keys
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.Subreddit
import name.lmj0011.redditdraftking.database.SubredditWithDrafts
import name.lmj0011.redditdraftking.databinding.ListItemSubredditBinding
import name.lmj0011.redditdraftking.helpers.DateTimeHelper
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getPostAtDateForListLayout

class SubredditListAdapter(private val clickListener: SubredditClickListener): ListAdapter<SubredditWithDrafts, SubredditListAdapter.ViewHolder>(SubredditWithDraftsDiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemSubredditBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){
        fun bind(clickListener: SubredditClickListener, subredditWithDrafts: SubredditWithDrafts){
            binding.subreddit = subredditWithDrafts.subreddit
            binding.clickListener = clickListener

            binding.subredditDisplayNameTextView.text = subredditWithDrafts.subreddit.displayNamePrefixed
            binding.subredditTotalTextView.text = context.resources.getQuantityString(R.plurals.draftsAvailable, subredditWithDrafts.drafts.size, subredditWithDrafts.drafts.size)
            Glide
                .with(context)
                .load(subredditWithDrafts.subreddit.iconImgUrl)
                .circleCrop()
                .error(R.mipmap.ic_default_subreddit_icon_round)
                .into(binding.subredditIconImageView)

            val draft = subredditWithDrafts.drafts.filter {
                it.postAtMillis > Keys.UNIX_EPOCH_MILLIS
            }.minByOrNull {
                it.postAtMillis
            }

            draft?.let {
                binding.draftAlarmIconImageView.visibility = View.VISIBLE
                binding.draftScheduledTextView.text = "${getPostAtDateForListLayout(it)} - ${draft.title}"
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSubredditBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class SubredditWithDraftsDiffCallback : DiffUtil.ItemCallback<SubredditWithDrafts>() {
        override fun areItemsTheSame(oldItem: SubredditWithDrafts, newItem: SubredditWithDrafts): Boolean {
            return oldItem.subreddit.uuid == newItem.subreddit.uuid
        }

        override fun areContentsTheSame(oldItem: SubredditWithDrafts, newItem: SubredditWithDrafts): Boolean {
            return oldItem == newItem
        }
    }

    class SubredditClickListener(val clickListener: (subreddit: Subreddit) -> Unit) {
        fun onClick(subreddit: Subreddit) = clickListener(subreddit)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subredditWithDrafts = getItem(position)

        holder.bind(clickListener, subredditWithDrafts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun submitList(list: MutableList<SubredditWithDrafts>?) {
        super.submitList(filterByDraftPostAtMillis(list))
    }

    /**
     * Sorts a list of `SubredditWithDrafts` by Drafts that are set to be posted the soonest;
     * also excludes `SubredditWithDrafts` with 0 `Drafts`
     */
    private fun filterByDraftPostAtMillis(list: MutableList<SubredditWithDrafts>?): MutableList<SubredditWithDrafts>? {
         list?.sortBy {
            val draft = it.drafts.filter {draft ->
                draft.postAtMillis > Keys.UNIX_EPOCH_MILLIS
            }.minByOrNull{draft ->
                draft.postAtMillis
            }
             draft?.postAtMillis ?: Long.MAX_VALUE
        }

        return list?.filter { it.drafts.isNotEmpty() }?.toMutableList()
    }

}