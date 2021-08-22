package name.lmj0011.holdup.ui.submission

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.imgpicker.ButtonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
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
import name.lmj0011.holdup.helpers.util.withUIContext
import org.jsoup.HttpStatusException

class ImageSubmissionFragment: Fragment(R.layout.fragment_image_submission),
    BaseFragmentInterface, SubmissionFragmentChild, BottomSheetImagePicker.OnImagesSelectedListener {
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override val actionBarTitle: String = "Image Submission"
    override var mode: Int = SubmissionFragmentChild.CREATE_AND_EDIT_MODE

    private lateinit var binding: FragmentImageSubmissionBinding
    private lateinit var listAdapter: GalleryListAdapter
    private lateinit var footerAdapter: AddImageListAdapter

    companion object {
        fun newInstance(submission: Submission?, mode: Int): ImageSubmissionFragment {
            val fragment = ImageSubmissionFragment()

            val args = Bundle().apply {
                putParcelable("submission", submission)
                putInt("mode", mode)
            }

            fragment.arguments = args

            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        submission = requireArguments().getParcelable("submission") as? Submission
        mode = requireArguments().getInt("mode")

        setupBinding(view)
        setupObservers()
        setupRecyclerView()

        submission?.imgGallery.let { list ->
            list?.let { updateImageGallery(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Image)
    }

    override fun setupBinding(view: View) {
        binding = FragmentImageSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
    }

    override fun setupObservers() {
        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })

//        viewModel.submissionImageGallery.observe(viewLifecycleOwner, { list -> })
    }

    override fun setupRecyclerView() {
        listAdapter = GalleryListAdapter(
            GalleryListAdapter.RemoveImageClickListener  { img ->
                listAdapter.currentList.toMutableList().let { list ->
                    list.remove(img)
                    viewModel.submissionImageGallery.postValue(list)
                    updateImageGallery(list)
                }
            }
        )

        footerAdapter = AddImageListAdapter(
            AddImageListAdapter.AddImageClickListener {
                BottomSheetImagePicker.Builder(getString(R.string.file_provider_authorities))
                    .cameraButton(ButtonType.None)
                    .galleryButton(ButtonType.Button)
                    .singleSelectTitle(R.string.image_picker_pick_single)
                    .show(childFragmentManager)
            }
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        binding.imageGalleryList.addItemDecoration(decor)

        if (mode == SubmissionFragmentChild.VIEW_MODE) {
            binding.imageGalleryList.adapter = ConcatAdapter(listAdapter)
        } else binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, footerAdapter)

    }

    /**
     *  clear any User input data from this Fragment
     */
    override fun clearUserInputViews() {
        val emptyList = mutableListOf<Image>()
        viewModel.submissionImageGallery.postValue(emptyList)
        updateImageGallery(emptyList)
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
                    val img = Image(
                        sourceUri = it.toString(),
                        mediaId = "",
                        url = "",
                        caption = "",
                        outboundUrl = ""
                    )

                    val list = listAdapter.currentList.toMutableList()
                    list.add(img)

                    withUIContext {
                        updateImageGallery(list)
                        viewModel.submissionImageGallery.postValue(list)
                        viewModel.validateSubmission(SubmissionKind.Image)
                    }
                } catch(ex: HttpStatusException) {
                    showSnackBar(binding.root, requireContext().getString(R.string.reddit_upload_media_error_msg, ex.statusCode, ex.message))
                }

            }


            withContext(Dispatchers.Main) {
                binding.progressBar.isVisible = false
                binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, footerAdapter)
            }
        }
    }

    override fun updateActionBarTitle() {}

    private fun updateImageGallery(list: MutableList<Image>) {
        listAdapter.submitList(list)

        launchUI {
            // scroll to end of List
            binding.imageGalleryList.adapter?.let {
                binding.imageGalleryList.scrollToPosition(it.itemCount - 1)
            }
        }
    }
}