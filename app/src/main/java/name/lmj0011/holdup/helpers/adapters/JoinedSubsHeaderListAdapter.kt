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
import name.lmj0011.holdup.databinding.ListItemJoinedSubsHeaderBinding
import name.lmj0011.holdup.databinding.ListItemSearchResultSubredditBinding
import name.lmj0011.holdup.helpers.models.Subreddit
import java.util.*

class JoinedSubsHeaderListAdapter(): ListAdapter<Subreddit, JoinedSubsHeaderListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemJoinedSubsHeaderBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(){}

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemJoinedSubsHeaderBinding.inflate(layoutInflater, parent, false)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * must be set, for this "list adapter header" to be visible
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = 1

}