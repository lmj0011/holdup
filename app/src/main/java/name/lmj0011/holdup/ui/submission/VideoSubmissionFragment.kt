package name.lmj0011.holdup.ui.submission

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentVideoSubmissionBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.FirebaseAnalyticsHelper
import name.lmj0011.holdup.helpers.adapters.AddImageListAdapter
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChildInterface
import name.lmj0011.holdup.helpers.models.Image
import name.lmj0011.holdup.helpers.models.Video
import name.lmj0011.holdup.helpers.util.*
import org.jsoup.HttpStatusException
import org.kodein.di.instance
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID.randomUUID
import kotlin.Exception

@ExperimentalCoroutinesApi
class VideoSubmissionFragment: Fragment(R.layout.fragment_video_submission),
    BaseFragmentInterface, SubmissionFragmentChildInterface {
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override var mode: Int = SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE
    lateinit var mediaPlayer: SimpleExoPlayer
    override lateinit var firebaseAnalyticsHelper: FirebaseAnalyticsHelper

    private lateinit var binding: FragmentVideoSubmissionBinding
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var localPlayerListener: Player.Listener

    private lateinit var pickVideoRequest: ActivityResultLauncher<PickVisualMediaRequest>

    companion object {
        fun newInstance(submission: Submission?, mode: Int, mediaPlayer: SimpleExoPlayer): VideoSubmissionFragment {
            val fragment = VideoSubmissionFragment()
            fragment.mediaPlayer = mediaPlayer

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
        pickVideoRequest = getPickVideoActivityResult()
        firebaseAnalyticsHelper = (requireContext().applicationContext as App).kodein.instance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataStoreHelper= (requireContext().applicationContext as App).kodein.instance()

        viewModel = SubmissionViewModel.getInstance(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application
        )

        submission = requireArguments().getParcelable("submission") as? Submission
        mode = requireArguments().getInt("mode")

        setupBinding(view)
        setupObservers()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        updateActionBarTitle()

        submission?.video?.let {
            try {
                initializePlayer(it)
            } catch (ex: Exception) {
                Timber.e(ex)
                hideVideoUI()
            }
        }
        viewModel.validateSubmission(SubmissionKind.Video)
    }

    override fun onStop() {
        super.onStop()
        (binding.videoView.player as? SimpleExoPlayer)?.stop()
        (binding.videoView.player as? SimpleExoPlayer)?.removeListener(localPlayerListener)
        binding.videoView.player = null

        if(mode == SubmissionFragmentChildInterface.VIEW_MODE) {
            hideVideoUI()
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentVideoSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.addVideoContainer.setOnClickWithDebounceListener(1000L) {
            submission?.video = null // so we don't trigger a playback error with the previous video
            pickVideoRequest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }

        binding.removeImageShapeableImageView.setOnClickListener {
            mediaPlayer.stop()
            hideVideoUI()
        }

        binding.toggleVolumeShapeableImageView.setOnClickListener { toggleMediaPlayerVolume() }
    }

    override fun setupObservers() {
        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner) {
            if (it) {
                clearUserInputViews()
            }
        }
    }

    override fun setupRecyclerView() {}

    override fun clearUserInputViews() {}

    override fun updateActionBarTitle() {}

    private fun initializePlayer(video: Video) {
        localPlayerListener = object: Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    showVideoUI()
                }

                if (state == Player.STATE_ENDED) {
                    when(mode) {
                        SubmissionFragmentChildInterface.VIEW_MODE -> {
                            hideVideoUI()
                            binding.videoView.player = null
                        }
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                val rootView = requireActivity().findViewById<View>(android.R.id.content)
                showSnackBar(rootView, "Playback error, ${error.sourceException.message}")
                binding.videoView.player = null
            }
        }

        val initVolumeIcon: () -> Unit = {
            /// show correct volume icon
            val contextForCoroutine = requireContext()
            launchUI {
                val isMuted = dataStoreHelper.getIsMediaPlayerMuted().first()

                if (isMuted) {
                    binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(contextForCoroutine, R.drawable.ic_baseline_volume_off_24))
                    mediaPlayer.volume = 0f
                } else {
                    binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(contextForCoroutine, R.drawable.ic_baseline_volume_up_24))
                    mediaPlayer.volume = 1f
                }
            }
            ///
        }

        val mediaItem = MediaItem.fromUri(getVideoSourceCompat(video))

        mediaPlayer
        .also { exoPlayer ->
            Glide
                .with(requireContext())
                .load(video.posterUrl)
                .into(binding.videoViewArtworkImageView)

            when(mode) {
                SubmissionFragmentChildInterface.VIEW_MODE -> {
                    binding.videoView.useController = false
                    hideVideoUI()

                    binding.videoCard.setOnClickListener {
                        initVolumeIcon()

                        if(binding.videoView.player?.isPlaying == true) {
                            binding.videoView.player?.pause()
                            return@setOnClickListener
                        }

                        if(binding.videoView.player?.isPlaying == false) {
                            binding.videoView.player?.play()
                            return@setOnClickListener
                        }

                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.playWhenReady = true
                        binding.videoView.player = exoPlayer
                        (binding.videoView.player as SimpleExoPlayer).addListener(localPlayerListener)
                        exoPlayer.prepare()
                    }
                }

                SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE -> {
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.playWhenReady = true
                    binding.videoView.player = exoPlayer
                    (binding.videoView.player as SimpleExoPlayer).addListener(localPlayerListener)
                    exoPlayer.prepare()
                    showVideoUI()
                }
            }

            initVolumeIcon()
            ////
        }.addListener(object: Player.Listener {
            override fun onMediaItemTransition(mediaItem0: MediaItem?, reason: Int) {
                (binding.videoView.player as? SimpleExoPlayer)?.removeListener(localPlayerListener)
                binding.videoView.player = null
                hideVideoUI()
            }
        })
    }

    private fun toggleMediaPlayerVolume() {
        val contextForCoroutine = requireContext()
        launchUI {
            val isMuted = dataStoreHelper.getIsMediaPlayerMuted().first()

            if (isMuted) {
                binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(contextForCoroutine, R.drawable.ic_baseline_volume_up_24))
                mediaPlayer.volume = 1f
                dataStoreHelper.setIsMediaPlayerMuted(!isMuted)
            } else {
                binding.toggleVolumeShapeableImageView.setImageDrawable(getDrawable(contextForCoroutine, R.drawable.ic_baseline_volume_off_24))
                mediaPlayer.volume = 0f
                dataStoreHelper.setIsMediaPlayerMuted(!isMuted)
            }
        }
    }

    private fun showVideoUI() {
        binding.videoViewArtworkImageView.visibility = View.GONE
        binding.addVideoContainer.visibility = View.GONE
        binding.toggleVolumeShapeableImageView.visibility = View.VISIBLE
        binding.videoView.visibility = View.VISIBLE
        binding.removeImageShapeableImageView.visibility = View.VISIBLE

        when(mode) {
            SubmissionFragmentChildInterface.VIEW_MODE -> {
                binding.removeImageShapeableImageView.visibility = View.GONE
            }
            SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE -> {
                binding.removeImageShapeableImageView.visibility = View.VISIBLE
            }
        }
    }

    private fun hideVideoUI() {
        binding.videoView.visibility = View.GONE
        binding.toggleVolumeShapeableImageView.visibility = View.GONE
        binding.removeImageShapeableImageView.visibility = View.GONE

        when(mode) {
            SubmissionFragmentChildInterface.VIEW_MODE -> {
                binding.videoViewArtworkImageView.visibility = View.VISIBLE
                binding.addVideoContainer.visibility = View.GONE
            }
            SubmissionFragmentChildInterface.CREATE_AND_EDIT_MODE -> {
                binding.videoViewArtworkImageView.visibility = View.GONE
                binding.addVideoContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun getPickVideoActivityResult(): ActivityResultLauncher<PickVisualMediaRequest> {
        return registerForActivityResult(ActivityResultContracts.PickVisualMedia())
        { videoUri ->
            videoUri?.let { uri ->
                binding.progressBar.isIndeterminate = true
                binding.progressBar.isVisible = true
                mediaPlayer = SimpleExoPlayer.Builder(requireContext()).build()

                val vid = Video(
                    sourceUri = uri.toString(),
                    mediaId = "",
                    url = "",
                    posterUrl = ""
                )

                requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                launchIO {
                    try {
                        /**
                         * Generate a video thumbnail
                         */
                        val thumbNail = Glide.with(requireContext())
                            .asBitmap().thumbnail(0.5F)
                            .load(uri).submit().get()

                        val file = File.createTempFile("${randomUUID()}", ".png", requireContext().cacheDir)
                        file.createNewFile()

                        val fileOut = FileOutputStream(file)

                        thumbNail.compress(Bitmap.CompressFormat.PNG, 100, fileOut)
                        fileOut.flush()
                        fileOut.close()

                        withUIContext{
                            val thumbNailUri = FileProvider.getUriForFile(
                                requireContext(),
                                getFileProviderAuthorities(requireContext()),
                                file
                            )

                            vid.posterUrl = thumbNailUri.toString()
                        }

                    }
                    catch(ex: HttpStatusException) {
                        withUIContext{
                            showSnackBar(binding.root, requireContext().getString(R.string.reddit_upload_media_error_msg, ex.statusCode, ex.message))
                            Timber.e(ex)
                        }
                    }
                    catch(ex: ImageDecoder.DecodeException) {
                        withUIContext{
                            vid.posterUrl = requireContext().getString(R.string.default_video_thumbnail_url)
                            showSnackBar(binding.root, ex.message.toString())
                            Timber.e(ex)
                        }
                    }
                    catch (ex: Exception) {
                        withUIContext{
                            showSnackBar(binding.root, ex.message.toString())
                            Timber.e(ex)
                        }
                    }
                    finally {
                        withUIContext{
                            binding.progressBar.isVisible = false
                            initializePlayer(vid)
                            viewModel.submissionVideo.postValue(vid)
                            viewModel.validateSubmission(SubmissionKind.Video)
                        }
                    }
                }
            }

        }
    }

}