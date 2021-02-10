package name.lmj0011.redditdraftking.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import name.lmj0011.redditdraftking.databinding.ListItemAddImageBinding
import name.lmj0011.redditdraftking.databinding.ListItemImageBinding
import name.lmj0011.redditdraftking.helpers.models.Image

/**
 * This Adapter serves as a Header/Footer to [GalleryListAdapter]
 *
 * ref: https://github.com/googlecodelabs/android-paging/pull/46/files#diff-3f0a09cfd8eb73d52cccb612f25c961cce8e1d800210a9f58220477ea81a3ee0
 */
class AddImageListAdapter (
    private val addImageClickListener: AddImageClickListener
): RecyclerView.Adapter<AddImageListAdapter.ViewHolder>() {
    class ViewHolder private constructor(val binding: ListItemAddImageBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(addImageClickListener: AddImageClickListener){
            binding.addImageClickListener = addImageClickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemAddImageBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }


    class AddImageClickListener(val clickListener: () -> Unit) {

        fun onClick() = clickListener()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(addImageClickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = 1


}