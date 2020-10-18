package name.lmj0011.redditdraftking.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import name.lmj0011.redditdraftking.Keys
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.Account
import name.lmj0011.redditdraftking.database.Draft
import name.lmj0011.redditdraftking.databinding.ListItemAccountBinding
import name.lmj0011.redditdraftking.databinding.ListItemSubredditDraftBinding
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getLocalDateFormatFromUtcMillis
import name.lmj0011.redditdraftking.helpers.DateTimeHelper.getPostAtDateForListLayout
import java.util.*

class AccountListAdapter(
    private val logOutClickListener: LogOutClickListener,
    private val reauthenticationClickListener: ReauthenticationClickListener
): ListAdapter<Account, AccountListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemAccountBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(
            account: Account,
            logOutClickListener: LogOutClickListener,
            reauthenticationClickListener: ReauthenticationClickListener){
            binding.account = account
            binding.logOutClickListener = logOutClickListener
            binding.reauthenticationClickListener = reauthenticationClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemAccountBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }

    class LogOutClickListener(val clickListener: (account: Account) -> Unit) {
        fun onClick(account: Account) = clickListener(account)
    }

    class ReauthenticationClickListener(val clickListener: (account: Account) -> Unit) {
        fun onClick(account: Account) = clickListener(account)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = getItem(position)

        holder.bind(account, logOutClickListener, reauthenticationClickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

}