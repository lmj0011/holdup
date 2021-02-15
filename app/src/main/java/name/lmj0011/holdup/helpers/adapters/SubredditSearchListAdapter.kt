package name.lmj0011.holdup.helpers.adapters

import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import name.lmj0011.holdup.R
import name.lmj0011.holdup.databinding.ListItemSearchResultSubredditBinding
import name.lmj0011.holdup.helpers.models.Subreddit
import java.util.*

class SubredditSearchListAdapter(val clickListener: SubredditSearchClickListener): ListAdapter<Subreddit, SubredditSearchListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemSearchResultSubredditBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(clickListener: SubredditSearchClickListener,  subreddit: Subreddit){
            binding.subreddit = subreddit
            binding.clickListener = clickListener
            Glide
                .with(context)
                .load(subreddit.iconImgUrl)
                .circleCrop()
                .error(R.mipmap.ic_default_subreddit_icon_round)
                .into(binding.iconImageView)

            binding.subredditNameTextView.text = subreddit.displayNamePrefixed
            val subCount = CompactDecimalFormat
                .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
                .format(subreddit.subscribers)
            binding.subscribersCountTextView.text = "${subCount.toLowerCase()} members"

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSearchResultSubredditBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Subreddit>() {
        override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem.displayNamePrefixed == newItem.displayNamePrefixed
        }

        override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }
    }

    class SubredditSearchClickListener(val clickListener: (subreddit: Subreddit) -> Unit) {
        fun onClick(subreddit: Subreddit) = clickListener(subreddit)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subreddit = getItem(position)

        holder.bind(clickListener, subreddit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

}