package name.lmj0011.redditdraftking.ui.submission

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.kroegerama.imgpicker.BottomSheetImagePicker
import com.kroegerama.imgpicker.ButtonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentImageSubmissionBinding
import name.lmj0011.redditdraftking.helpers.adapters.AddImageListAdapter
import name.lmj0011.redditdraftking.helpers.adapters.GalleryListAdapter
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.interfaces.FragmentBaseInit
import name.lmj0011.redditdraftking.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.redditdraftking.helpers.models.Image
import name.lmj0011.redditdraftking.helpers.util.buildOneColorStateList
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchUI
import name.lmj0011.redditdraftking.helpers.util.showToastMessage
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import timber.log.Timber
import java.lang.Exception

class ImageSubmissionFragment(
    // TODO - can be refactored since callbacks are not needed because viewModel is a singleton
    val setFlairItemForSubmission: SubredditFlairListAdapter.FlairItemClickListener,
    val removeFlairClickListener: (v: View) -> Unit,
): Fragment(R.layout.fragment_image_submission),
    FragmentBaseInit, SubmissionFragmentChild, BottomSheetImagePicker.OnImagesSelectedListener {
    private lateinit var binding: FragmentImageSubmissionBinding
    private lateinit var bottomSheetSubredditFlairFragment: BottomSheetSubredditFlairFragment
    private lateinit var  viewModel: SubmissionViewModel
    private lateinit var listAdapter: GalleryListAdapter
    private lateinit var footerAdapter: AddImageListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupObservers()
        setupRecyclerView()

        resetFlairToDefaultState()
    }

    override fun onResume() {
        super.onResume()

        viewModel.validateSubmission(SubmissionKind.Image)
    }

    override fun setupBinding(view: View) {
        binding = FragmentImageSubmissionBinding.bind(view)
        binding.lifecycleOwner = this
    }

    override fun setupObservers() {
        viewModel.getSubreddit().observe(viewLifecycleOwner, {
            val listFlow = viewModel.getSubredditFlairListFlow()

            bottomSheetSubredditFlairFragment = BottomSheetSubredditFlairFragment(
                SubredditFlairListAdapter.FlairItemClickListener { flair ->
                    binding.addFlairChip.text = flair.text
                    try {
                        buildOneColorStateList(Color.parseColor(flair.backGroundColor))?.let {
                            binding.addFlairChip.chipBackgroundColor = it
                        }
                    }
                    catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

                    when(flair.textColor) {
                        "light" -> binding.addFlairChip.setTextColor(Color.WHITE)
                        else -> binding.addFlairChip.setTextColor(Color.DKGRAY)
                    }

                    setFlairItemForSubmission.clickListener(flair)
                    viewModel.validateSubmission(SubmissionKind.Image)
                    bottomSheetSubredditFlairFragment.dismiss()
                },
                { v: View ->
                    removeFlairClickListener(v)
                    resetFlairToDefaultState()
                    viewModel.validateSubmission(SubmissionKind.Image)
                    bottomSheetSubredditFlairFragment.dismiss()
                },
                listFlow)

            launchUI {
                listFlow.collectLatest {
                    if(it.isNotEmpty()) {
                        binding.addFlairChip.visibility = View.VISIBLE

                        binding.addFlairChip.setOnClickListener {
                            bottomSheetSubredditFlairFragment.show(childFragmentManager, "BottomSheetSubredditFlairFragment")
                        }
                    } else {
                        binding.addFlairChip.visibility = View.GONE
                    }
                }
            }
        })

        binding.imageTitleTextView.addTextChangedListener( object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.submissionImageTitle.postValue(s.toString())
            }
        })

        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
                resetFlairToDefaultState()
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
        binding.imageTitleTextView.text.clear()
        viewModel.submissionImageGallery.postValue(mutableListOf())
    }

    override fun resetFlairToDefaultState() {
        try {
            binding.addFlairChip.text = "+ Add Flair"
            buildOneColorStateList(Color.LTGRAY)?.let {
                binding.addFlairChip.chipBackgroundColor = it
            }
            binding.addFlairChip.setTextColor(Color.BLACK)
        }
        catch (ex: Exception) { /* flair.backGroundColor was either null or not a recognizable color */}

        viewModel.subredditFlair.postValue(null)
    }

    override fun onImagesSelected(uris: List<Uri>, tag: String?) {
        launchIO {

            withContext(Dispatchers.Main) {
                binding.imageGalleryList.adapter = ConcatAdapter(listAdapter, AddImageListAdapter(
                    AddImageListAdapter.AddImageClickListener {
                        showToastMessage(requireContext(), "Uploading Image(s)")
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
}