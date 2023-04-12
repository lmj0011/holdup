package name.lmj0011.holdup.ui.submission


import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentImageSubmissionBinding
import name.lmj0011.holdup.helpers.FirebaseAnalyticsHelper
import name.lmj0011.holdup.helpers.adapters.AddImageListAdapter
import name.lmj0011.holdup.helpers.adapters.GalleryListAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChildInterface
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.helpers.util.*
import org.kodein.di.instance
import timber.log.Timber

class ImageSubmissionFragment: Fragment(R.layout.fragment_image_submission),
    BaseFragmentInterface, SubmissionFragmentChildInterface {
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override var mode: Int = SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE
    override lateinit var firebaseAnalyticsHelper: FirebaseAnalyticsHelper

    private lateinit var binding: FragmentImageSubmissionBinding
    private lateinit var listAdapter: GalleryListAdapter
    private lateinit var footerAdapter: AddImageListAdapter

    private lateinit var pickMultipleImagesRequest: ActivityResultLauncher<PickVisualMediaRequest>

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickMultipleImagesRequest = getPickMultipleImagesActivityResult()
        firebaseAnalyticsHelper = (requireContext().applicationContext as App).kodein.instance()
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
                pickMultipleImagesRequest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        binding.imageGalleryList.addItemDecoration(decor)

        if (mode == SubmissionFragmentChildInterface.VIEW_MODE) {
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

    private fun getPickMultipleImagesActivityResult(): ActivityResultLauncher<PickVisualMediaRequest> {

        return registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20))
        { uris ->
            launchIO {

                withContext(Dispatchers.Main) {
                    binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, AddImageListAdapter(
                        AddImageListAdapter.AddImageClickListener {
                            showSnackBar(binding.root, "Uploading images..")
                        }
                    ))

                    binding.imageGalleryList.adapter?.let {
                        binding.imageGalleryList.scrollToPosition(it.itemCount - 1)
                    }

                    binding.progressBar.isIndeterminate = true
                    binding.progressBar.isVisible = true
                }

                Timber.d(uris.toString())

                val list: MutableList<Image> = mutableListOf()

                uris.forEach { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val img = Image(
                        sourceUri = uri.toString(),
                        mediaId = "",
                        url = "",
                        caption = "",
                        outboundUrl = ""
                    )

                    list.add(img)
                }


                withContext(Dispatchers.Main) {
                    val oldList = listAdapter.currentList.toMutableList()
                    val currentList = oldList + list

                    val finalList = if(currentList.size > 20) {
                        showSnackBar(binding.root, "max allowed images reached!")
                        currentList.slice(0..19).toMutableList()
                    } else currentList.toMutableList()

                    updateImageGallery(finalList)
                    viewModel.submissionImageGallery.postValue(finalList)
                    viewModel.validateSubmission(SubmissionKind.Image)
                    binding.progressBar.isVisible = false
                    binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, footerAdapter)
                }
            }
        }
    }
}