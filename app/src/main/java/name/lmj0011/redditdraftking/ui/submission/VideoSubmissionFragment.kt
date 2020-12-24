package name.lmj0011.redditdraftking.ui.submission

import android.app.ActionBar
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.media.ThumbnailUtils.createVideoThumbnail
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Size
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentVideoSubmissionBinding
import name.lmj0011.redditdraftking.helpers.adapters.SubredditFlairListAdapter
import name.lmj0011.redditdraftking.helpers.enums.SubmissionKind
import name.lmj0011.redditdraftking.helpers.interfaces.FragmentBaseInit
import name.lmj0011.redditdraftking.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.redditdraftking.helpers.models.Video
import name.lmj0011.redditdraftking.helpers.util.buildOneColorStateList
import name.lmj0011.redditdraftking.helpers.util.launchIO
import name.lmj0011.redditdraftking.helpers.util.launchUI
import name.lmj0011.redditdraftking.ui.submission.bottomsheet.BottomSheetSubredditFlairFragment
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class VideoSubmissionFragment: Fragment(R.layout.fragment_video_submission),
    FragmentBaseInit, SubmissionFragmentChild {
    companion object {
        const val GET_VIDEO_REQUEST_CODE = 100
    }

    private lateinit var binding: FragmentVideoSubmissionBinding
    private lateinit var  viewModel: SubmissionViewModel
    private lateinit var mediaPlayer: MediaPlayer

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
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()
        viewModel.validateSubmission(SubmissionKind.Video)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            GET_VIDEO_REQUEST_CODE -> {
                binding.progressBar.isIndeterminate = true
                binding.progressBar.isVisible = true

                launchIO {
                    try {
                        val uri = data?.data
                        val videoInfo = viewModel.uploadMedia(Uri.parse(uri.toString()))

                        val vid = Video(
                            sourceUri = uri.toString(),
                            mediaId = videoInfo.first,
                            url = videoInfo.second,
                            posterUrl = ""
                        )


                        /**
                         * Generate a video thumbnail
                         */
                        try {
                            val thumbNail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                requireContext().contentResolver.loadThumbnail(Uri.parse(uri.toString()),
                                    Size(1280, 720), null)
                            } else {
                                createVideoThumbnail(uri.toString(), MediaStore.Images.Thumbnails.MINI_KIND)
                            }

                            thumbNail?.let { bitmap ->
                                val file = File(requireContext().cacheDir, "${videoInfo.first}.png")
                                file.createNewFile()
                                val fileOut = FileOutputStream(file)

                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOut)
                                fileOut.flush()
                                fileOut.close()

                                val thumbNailInfo = viewModel.uploadMedia(file, "image/png")
                                vid.posterUrl = thumbNailInfo.second
                            }
                        } catch (ex: Exception) {
                            Timber.e(ex)
                        }
                        /**
                         *
                         */


                        Timber.d("$vid")
                        viewModel.submissionVideo.postValue(vid)
                        viewModel.validateSubmission(SubmissionKind.Video)
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                    finally {
                        withContext(Dispatchers.Main) {
                            binding.progressBar.isVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentVideoSubmissionBinding.bind(view)
        binding.lifecycleOwner = this

        binding.addVideoContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                addCategory(Intent.CATEGORY_OPENABLE)

                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)

                type = "video/*"
            }

            startActivityForResult(intent, GET_VIDEO_REQUEST_CODE)
        }

        binding.removeImageShapeableImageView.setOnClickListener {
            hideVideoUI()
        }

        binding.toggleVolumeShapeableImageView.setOnClickListener { toggleMediaPlayerVolume() }
    }

    override fun setupObservers() {
        viewModel.submissionVideo.observe(viewLifecycleOwner, { video ->
            if (video != null) {
                showVideoUI()
                val uri = Uri.parse(video.sourceUri)
                binding.videoView.setMediaController(MediaController(requireContext()))
                binding.videoView.setVideoURI(uri)
            } else hideVideoUI()
        })

        binding.videoView.setOnPreparedListener { player ->
            mediaPlayer = player

            // mute the volume by default
            binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_baseline_volume_off_24))
            binding.toggleVolumeShapeableImageView.tag = "muted"
            mediaPlayer.setVolume(0f, 0f)
            ////

            mediaPlayer.start()
        }


        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })
    }

    override fun setupRecyclerView() {}

    override fun clearUserInputViews() {
        viewModel.submissionVideo.postValue(null)
    }

    override fun updateActionBarTitle() {
        launchUI {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = "Video Submission"
        }
    }


    private fun toggleMediaPlayerVolume() {
        when(binding.toggleVolumeShapeableImageView.tag) {
            "muted" -> {
                binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_baseline_volume_up_24))
                binding.toggleVolumeShapeableImageView.tag = "unmuted"
                mediaPlayer.setVolume(1f, 1f)
            }
            "unmuted" -> {
                binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_baseline_volume_off_24))
                binding.toggleVolumeShapeableImageView.tag = "muted"
                mediaPlayer.setVolume(0f, 0f)
            }
        }
    }

    private fun showVideoUI() {
        binding.addVideoContainer.visibility = View.GONE
        binding.removeImageShapeableImageView.visibility = View.VISIBLE
        binding.toggleVolumeShapeableImageView.visibility = View.VISIBLE
        binding.videoView.visibility = View.VISIBLE
    }

    private fun hideVideoUI() {
        binding.videoView.visibility = View.GONE
        binding.toggleVolumeShapeableImageView.visibility = View.GONE
        binding.removeImageShapeableImageView.visibility = View.GONE
        binding.addVideoContainer.visibility = View.VISIBLE
    }

}