package name.lmj0011.redditdraftking.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.redditdraftking.Keys
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.models.Draft
import name.lmj0011.redditdraftking.databinding.ListItemSubredditDraftBinding
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getPostAtDateForListLayout
import java.util.*

class DraftListAdapter(private val alarmIconClickListener: AlarmIconClickListener): ListAdapter<Draft, DraftListAdapter.ViewHolder>(DraftsDiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemSubredditDraftBinding, val context: Context) : RecyclerView.ViewHolder(binding.root){
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        fun bind(alarmIconClickListener: AlarmIconClickListener, draft: Draft){
            binding.draft = draft
            binding.alarmIconClickListener = alarmIconClickListener
            binding.draftTitleTextView.text = draft.title

            when {
                draft.postAtMillis > Keys.UNIX_EPOCH_MILLIS -> {
                    binding.draftScheduledTextView.text = getPostAtDateForListLayout(draft)
                    binding.draftAlarmIconImageView.setImageResource(R.drawable.ic_baseline_access_alarm_24)
                }
                else -> {
                    binding.draftScheduledTextView.text = ""
                }
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSubredditDraftBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class DraftsDiffCallback : DiffUtil.ItemCallback<Draft>() {
        override fun areItemsTheSame(oldItem: Draft, newItem: Draft): Boolean {
            return oldItem.uuid == newItem.uuid
        }

        override fun areContentsTheSame(oldItem: Draft, newItem: Draft): Boolean {
            return oldItem == newItem
        }
    }

    class AlarmIconClickListener(val clickListener: (draft: Draft) -> Unit) {
        fun onClick(draft: Draft) = clickListener(draft)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val draft = getItem(position)

        holder.bind(alarmIconClickListener, draft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

}