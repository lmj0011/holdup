package name.lmj0011.redditdraftking.helpers.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.redditdraftking.databinding.ListItemSubredditFlairBinding
import name.lmj0011.redditdraftking.helpers.models.SubredditFlair
import name.lmj0011.redditdraftking.helpers.util.buildOneColorStateList
import java.lang.Exception

class SubredditFlairListAdapter(val itemClickListener: FlairItemClickListener): ListAdapter<SubredditFlair, SubredditFlairListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemSubredditFlairBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(itemClickListener: FlairItemClickListener, flair: SubredditFlair){
            binding.flair = flair
            binding.clickListener = itemClickListener
            binding.selectableFlairChip.text = flair.text

            try {
                buildOneColorStateList(Color.parseColor(flair.backGroundColor))?.let {
                    binding.selectableFlairChip.chipBackgroundColor = it
                }
            }
            catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

            when(flair.textColor) {
                "light" -> binding.selectableFlairChip.setTextColor(Color.WHITE)
                else -> binding.selectableFlairChip.setTextColor(Color.DKGRAY)
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSubredditFlairBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SubredditFlair>() {
        override fun areItemsTheSame(oldItem: SubredditFlair, newItem: SubredditFlair): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: SubredditFlair, newItem: SubredditFlair): Boolean {
            return oldItem == newItem
        }
    }

    class FlairItemClickListener(val clickListener: (flair: SubredditFlair) -> Unit) {
        fun onClick(flair: SubredditFlair) = clickListener(flair)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flair = getItem(position)

        holder.bind(itemClickListener, flair)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

}