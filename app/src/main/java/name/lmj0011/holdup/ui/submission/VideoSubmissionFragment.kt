package name.lmj0011.holdup.ui.submission

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils.createVideoThumbnail
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.View
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import name.lmj0011.holdup.App
import name.lmj0011.holdup.R
import name.lmj0011.holdup.database.AppDatabase
import name.lmj0011.holdup.database.models.Submission
import name.lmj0011.holdup.databinding.FragmentVideoSubmissionBinding
import name.lmj0011.holdup.helpers.DataStoreHelper
import name.lmj0011.holdup.helpers.enums.SubmissionKind
import name.lmj0011.holdup.helpers.interfaces.BaseFragmentInterface
import name.lmj0011.holdup.helpers.interfaces.SubmissionFragmentChild
import name.lmj0011.holdup.helpers.models.Video
import name.lmj0011.holdup.helpers.util.launchIO
import name.lmj0011.holdup.helpers.util.launchUI
import name.lmj0011.holdup.helpers.util.showSnackBar
import name.lmj0011.holdup.helpers.util.withUIContext
import org.kodein.di.instance
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception

@ExperimentalCoroutinesApi
class VideoSubmissionFragment: Fragment(R.layout.fragment_video_submission),
    BaseFragmentInterface, SubmissionFragmentChild {
    override lateinit var viewModel: SubmissionViewModel
    override var submission: Submission? = null
    override val actionBarTitle: String = "Video Submission"
    override var mode: Int = SubmissionFragmentChild.CREATE_AND_EDIT_MODE
    lateinit var mediaPlayer: SimpleExoPlayer

    private lateinit var binding: FragmentVideoSubmissionBinding
    private lateinit var dataStoreHelper: DataStoreHelper
    private lateinit var localPlayerListener: Player.Listener

    companion object {
        const val GET_VIDEO_REQUEST_CODE = 100

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

        if(mode == SubmissionFragmentChild.VIEW_MODE) {
            hideVideoUI()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            GET_VIDEO_REQUEST_CODE -> {
                binding.progressBar.isIndeterminate = true
                binding.progressBar.isVisible = true
                mediaPlayer = SimpleExoPlayer.Builder(requireContext()).build()

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


                        Timber.d("vid: $vid")
                        withUIContext {
                            initializePlayer(vid)
                            viewModel.submissionVideo.postValue(vid)
                            viewModel.validateSubmission(SubmissionKind.Video)
                        }
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                    finally {
                        withUIContext{
                            binding.progressBar.isVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun setupBinding(view: View) {
        binding = FragmentVideoSubmissionBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.addVideoContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                addCategory(Intent.CATEGORY_OPENABLE)

                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)

                type = "video/*"
            }

            submission?.video = null // so we don't trigger a playback error with the previous video
            startActivityForResult(intent, GET_VIDEO_REQUEST_CODE)
        }

        binding.removeImageShapeableImageView.setOnClickListener {
            mediaPlayer.stop()
            hideVideoUI()
        }

        binding.toggleVolumeShapeableImageView.setOnClickListener { toggleMediaPlayerVolume() }
    }

    override fun setupObservers() {
//        viewModel.submissionVideo.observe(viewLifecycleOwner, { video ->
//            Timber.d("class named: ${this::class.simpleName}")
//        })


        viewModel.isSubmissionSuccessful.observe(viewLifecycleOwner, {
            if (it) {
                clearUserInputViews()
            }
        })
    }

    override fun setupRecyclerView() {}

    override fun clearUserInputViews() {
//        viewModel.submissionVideo.postValue(null)
    }

    override fun updateActionBarTitle() {}

    private fun initializePlayer(video: Video) {
        localPlayerListener = object: Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    showVideoUI()
                }

                if (state == Player.STATE_ENDED) {
                    when(mode) {
                        SubmissionFragmentChild.VIEW_MODE -> {
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

        val mediaItem = MediaItem.fromUri(Uri.parse(video.url))

        mediaPlayer
        .also { exoPlayer ->
            Glide
                .with(requireContext())
                .load(video.posterUrl)
                .into(binding.videoViewArtworkImageView)

            when(mode) {
                SubmissionFragmentChild.VIEW_MODE -> {
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

                SubmissionFragmentChild.CREATE_AND_EDIT_MODE -> {
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
            SubmissionFragmentChild.VIEW_MODE -> {
                binding.removeImageShapeableImageView.visibility = View.GONE
            }
            SubmissionFragmentChild.CREATE_AND_EDIT_MODE -> {
                binding.removeImageShapeableImageView.visibility = View.VISIBLE
            }
        }
    }

    private fun hideVideoUI() {
        binding.videoView.visibility = View.GONE
        binding.toggleVolumeShapeableImageView.visibility = View.GONE
        binding.removeImageShapeableImageView.visibility = View.GONE

        when(mode) {
            SubmissionFragmentChild.VIEW_MODE -> {
                binding.videoViewArtworkImageView.visibility = View.VISIBLE
                binding.addVideoContainer.visibility = View.GONE
            }
            SubmissionFragmentChild.CREATE_AND_EDIT_MODE -> {
                binding.videoViewArtworkImageView.visibility = View.GONE
                binding.addVideoContainer.visibility = View.VISIBLE
            }
        }
    }

}