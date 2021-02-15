package name.lmj0011.holdup.helpers.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import name.lmj0011.holdup.databinding.ListItemImageBinding
import name.lmj0011.holdup.helpers.models.Image

class GalleryListAdapter (
    private val removeImageClickListener: RemoveImageClickListener
): ListAdapter<Image, GalleryListAdapter.ViewHolder>(DiffCallback()) {
    class ViewHolder private constructor(val binding: ListItemImageBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(
            image: Image,
            removeImageClickListener: RemoveImageClickListener){
            binding.image = image
            binding.removeImageClickListener = removeImageClickListener

            Glide
                .with(context)
                .load(image.url)
                .into(binding.backgroundImageView)

            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemImageBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, parent.context)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Image>() {
        override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem == newItem
        }
    }

    class RemoveImageClickListener(val clickListener: (image: Image) -> Unit) {
        fun onClick(image: Image) = clickListener(image)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = getItem(position)

        holder.bind(image, removeImageClickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


}