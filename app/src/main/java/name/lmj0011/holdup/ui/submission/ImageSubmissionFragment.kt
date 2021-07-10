package name.lmj0011.holdup.ui.submission

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.imgpicker.ButtonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentImageSubmissionBinding
import name.lmj0011.holdup.helpers.adapters.AddImageListAdapter
import name.lmj0011.holdup.helpers.adapters.GalleryListAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.launchUI
import name.lmj0011.holdup.helpers.util.showSnackBar
import timber.log.Timber
import java.lang.Exception

class ImageSubmissionFragment(
    override var viewModel:  SubmissionViewModel,
    override val submission: Submission? = null,
    override val actionBarTitle: String? = "Image Submission"
): Fragment(R.layout.fragment_image_submission),
    BaseFragmentInterface, SubmissionFragmentChild, BottomSheetImagePicker.OnImagesSelectedListener {
    private lateinit var binding: FragmentImageSubmissionBinding
    private lateinit var listAdapter: GalleryListAdapter
    private lateinit var footerAdapter: AddImageListAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
        setupRecyclerView()

        submission?.let {
            listAdapter.submitList(it.imgGallery)
            listAdapter.notifyDataSetChanged()

            // scroll to end of List
            binding.imageGalleryList.adapter?.let { recyclerView ->
                binding.imageGalleryList.scrollToPosition(recyclerView.itemCount - 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Image)
    }

    override fun setupBinding(view: View) {
        binding = FragmentImageSubmissionBinding.bind(view)
        binding.lifecycleOwner = this
    }

    override fun setupObservers() {
        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })

        viewModel.submissionImageGallery.observe(viewLifecycleOwner, { list ->
            list?.let {
                listAdapter.submitList(it)
                listAdapter.notifyDataSetChanged()
            }

            // scroll to end of List
            binding.imageGalleryList.adapter?.let {
                binding.imageGalleryList.scrollToPosition(it.itemCount - 1)
            }
        })
    }

    override fun setupRecyclerView() {
        listAdapter = GalleryListAdapter(
            GalleryListAdapter.RemoveImageClickListener  { img ->
                viewModel.submissionImageGallery.value?.let { list ->
                    list.remove(img)
                    viewModel.submissionImageGallery.postValue(list)
                }
            },

            GalleryListAdapter.CopyImageUrlLongClickListener { img ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText("image url", img.url)
                clipboard.setPrimaryClip(clip)

                showSnackBar(binding.root, "copied image url")
                true
            }
        )

        footerAdapter = AddImageListAdapter(
            AddImageListAdapter.AddImageClickListener {
                BottomSheetImagePicker.Builder(getString(R.string.file_provider))
                    .cameraButton(ButtonType.None)
                    .galleryButton(ButtonType.Button)
                    .singleSelectTitle(R.string.image_picker_pick_single)
                    .show(childFragmentManager)
            }
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        binding.imageGalleryList.addItemDecoration(decor)
        binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, footerAdapter)
    }

    /**
     *  clear any User input data from this Fragment
     */
    override fun clearUserInputViews() {
        viewModel.submissionImageGallery.postValue(mutableListOf())
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        launchIO {

            withContext(Dispatchers.Main) {
                binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, AddImageListAdapter(
                    AddImageListAdapter.AddImageClickListener {
                        showSnackBar(binding.root, "Busy uploading images..")
                    }
                ))

                binding.imageGalleryList.adapter?.let {
                    binding.imageGalleryList.scrollToPosition(it.itemCount - 1)
                }

                binding.progressBar.isIndeterminate = true
                binding.progressBar.isVisible = true
            }

            uris.forEach {
                try {
                    val mediaInfo = viewModel.uploadMedia(Uri.parse(it.toString()))

                    val img = Image(
                        sourceUri = it.toString(),
                        mediaId = mediaInfo.first,
                        url = mediaInfo.second,
                        caption = "",
                        outboundUrl = ""
                    )

                    val list = viewModel.submissionImageGallery.value ?: mutableListOf()
                    list.add(img)

                    viewModel.submissionImageGallery.postValue(list)
                    viewModel.validateSubmission(SubmissionKind.Image)
                } catch (ex: Exception) {
                    Timber.e(ex)
                }

            }


            withContext(Dispatchers.Main) {
                binding.progressBar.isVisible = false
                binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, footerAdapter)
            }
        }
    }

    override fun updateActionBarTitle() {
        actionBarTitle?.let {
            launchUI {
                (requireActivity() as AppCompatActivity).supportActionBar?.title = it
            }
        }
    }
}