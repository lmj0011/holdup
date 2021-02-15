package name.lmj0011.holdup.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Account
import name.lmj0011.holdup.databinding.ListItemAccountBinding

class AccountListAdapter(
    private val logOutClickListener: LogOutClickListener,
    private val accountNameClickListener: AccountNameClickListener
): ListAdapter<Account, AccountListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemAccountBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(
            account: Account,
            logOutClickListener: LogOutClickListener,
            accountNameClickListener: AccountNameClickListener){
            binding.account = account
            binding.logOutClickListener = logOutClickListener
            binding.accountNameClickListener = accountNameClickListener
            Glide
                .with(context)
                .load(account.iconImage)
                .apply(RequestOptions().override(100))
                .circleCrop()
                .error(R.drawable.ic_baseline_image_24)
                .into(binding.iconImageView)

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

    // click listener for when a user clicks on the account's username
    class AccountNameClickListener(val clickListener: (account: Account) -> Unit) {
        fun onClick(account: Account) = clickListener(account)
    }

    class LogOutClickListener(val clickListener: (account: Account) -> Unit) {
        fun onClick(account: Account) = clickListener(account)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = getItem(position)

        holder.bind(account, logOutClickListener, accountNameClickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

}