package com.raywenderlich.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.databinding.FragmentEpisodePlayerBinding
import com.raywenderlich.podplay.service.PodplayMediaCallback
import com.raywenderlich.podplay.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.raywenderlich.podplay.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.raywenderlich.podplay.service.PodplayMediaService
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class EpisodePlayerFragment : Fragment() {
    private var isVideo: Boolean = false
    private var playOnPrepare: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var progressAnimator: ValueAnimator? = null
    private var draggingScrubber: Boolean = false
    private var episodeDuration: Long = 0
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private lateinit var databinding: FragmentEpisodePlayerBinding
    private var playerSpeed: Float = 1.0f

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            podcastViewModel.activeEpisodeViewData?.isVideo ?: false
        } else {
            false
        }
        if (!isVideo) {
            initMediaBrowser()
        }
    }
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        databinding = FragmentEpisodePlayerBinding.inflate(inflater,
            container, false)
        return databinding.root
    }
    override fun onViewCreated(view: View, savedInstanceState:
    Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        if (isVideo) {
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
    }
    override fun onStart() {
        super.onStart()
        if (!isVideo) {
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if
                        (MediaControllerCompat.getMediaController(fragmentActivity) ==
                    null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }
    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity)
            != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
        if (isVideo) {
            mediaPlayer?.setDisplay(null)
        }

        if (!fragmentActivity.isChangingConfigurations) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private val podcastViewModel: PodcastViewModel by activityViewModels()

    private fun updateControls() {

        databinding.episodeTitleTextView.text =
            podcastViewModel.activeEpisodeViewData?.title

        val htmlDesc =
            podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        databinding.episodeDescTextView.text = descSpan
        databinding.episodeDescTextView.movementMethod =
            ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(podcastViewModel.podcastLiveData.value?.imageUrl)
            .into(databinding.episodeImageView)
        val speedButtonText = "${playerSpeed}x"
        databinding.speedButton.text = speedButtonText

        mediaPlayer?.let {
            updateControlsFromController()
        }
    }

    private fun startPlaying(episodeViewData:
                             PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
            viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl)
        controller.transportControls.playFromUri(
            Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    inner class MediaControllerCallback:
        MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?)
        {
            super.onMetadataChanged(metadata)
            println(
                "metadata changed to ${metadata?.getString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state:
                                            PlaybackStateCompat?) {
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
        }
    }

    inner class MediaBrowserCallBacks:
        MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()

            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
            updateControlsFromController()
        }
        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            // Disable transport controls
        }
        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            // Fatal error handling
        }
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity,
            ComponentName(fragmentActivity,
                PodplayMediaService::class.java),
            MediaBrowserCallBacks(),
            null)
    }

    private fun registerMediaController(token:
                                        MediaSessionCompat.Token) {

        val fragmentActivity = activity as FragmentActivity

        val mediaController = MediaControllerCompat(fragmentActivity,
            token)

        MediaControllerCompat.setMediaController(fragmentActivity,
            mediaController)

        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun togglePlayPause() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state ==
                PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let{ startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let{ startPlaying(it) }
        }
    }

    private fun setupControls() {
        databinding.playToggleButton.setOnClickListener {
            togglePlayPause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            databinding.speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            databinding.speedButton.visibility = View.INVISIBLE
        }
        databinding.forwardButton.setOnClickListener {
            seekBy(30)
        }
        databinding.replayButton.setOnClickListener {
            seekBy(-10)
        }
        databinding.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress:
            Int, fromUser: Boolean) {

                databinding.currentTimeTextView.text =
                    DateUtils.formatElapsedTime((progress / 1000).toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

                draggingScrubber = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                draggingScrubber = false

                val fragmentActivity = activity as FragmentActivity
                val controller =
                    MediaControllerCompat.getMediaController(fragmentActivity)
                if (controller.playbackState != null) {

                    controller.transportControls.seekTo(seekBar.progress.toLong())
                } else {

                    seekBar.progress = 0
                }
            }
        })
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        databinding.playToggleButton.isActivated = isPlaying
        val progress = position.toInt()
        databinding.seekBar.progress = progress
        val speedButtonText = "${playerSpeed}x"
        databinding.speedButton.text = speedButtonText
        if (isPlaying) {
            animateScrubber(progress, speed)
            if (isVideo) {
                setupVideoUI()
            }
        }
    }

    private fun changeSpeed() {

        playerSpeed += 0.25f
        if (playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }

        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)

        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)

        val speedButtonText = "${playerSpeed}x"
        databinding.speedButton.text = speedButtonText
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position +
                seconds*1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun updateControlsFromMetadata(metadata:
                                           MediaMetadataCompat) {
        episodeDuration =
            metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        databinding.endTimeTextView.text =
            DateUtils.formatElapsedTime((episodeDuration / 1000))
        databinding.seekBar.max = episodeDuration.toInt()

    }

    private fun animateScrubber(progress: Int, speed: Float) {
        // 2
        val timeRemaining = ((episodeDuration - progress) /
                speed).toInt()
        // 3
        if (timeRemaining < 0) {
            return;
        }
        // 4
        progressAnimator = ValueAnimator.ofInt(
            progress, episodeDuration.toInt())
        progressAnimator?.let { animator ->
            // 5
            animator.duration = timeRemaining.toLong()
            // 6
            animator.interpolator = LinearInterpolator()
            // 7
            animator.addUpdateListener {
                if (draggingScrubber) {
                    // 8
                    animator.cancel()
                } else {
                    // 9
                    databinding.seekBar.progress = animator.animatedValue as
                            Int
                }
            }
            animator.start()
        }
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(controller.playbackState.state,
                    controller.playbackState.position, playerSpeed)
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun initMediaSession() {
        if (mediaSession == null) {

            mediaSession = MediaSessionCompat(activity as Context,
                "EpisodePlayerFragment")

            mediaSession?.setMediaButtonReceiver(null)
        }
        mediaSession?.let {
            registerMediaController(it.sessionToken)
        }
    }

    private fun setSurfaceSize() {

        val mediaPlayer = mediaPlayer ?: return

        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        val parent = databinding.videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height

        val layoutAspectRatio = containerWidth.toFloat() /
                containerHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight

        val layoutParams = databinding.videoSurfaceView.layoutParams

        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height =
                (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width =
                (containerHeight * videoAspectRatio).toInt()
        }

        databinding.videoSurfaceView.layoutParams = layoutParams
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {

            mediaPlayer = MediaPlayer()
            mediaPlayer?.let { mediaPlayer ->

                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )


                mediaPlayer.setDataSource(podcastViewModel.activeEpisodeViewData
                    ?.mediaUrl)

                mediaPlayer.setOnPreparedListener {

                    val fragmentActivity = activity as FragmentActivity
                    mediaSession?.let { mediaSession ->
                        val episodeMediaCallback =
                            PodplayMediaCallback(fragmentActivity, mediaSession, it)
                        mediaSession.setCallback(episodeMediaCallback)
                    }

                    setSurfaceSize()

                    if (playOnPrepare) {
                        togglePlayPause()
                    }
                }
                mediaPlayer.prepareAsync()
            }
        } else {

            setSurfaceSize()
        }
    }

    private fun initVideoPlayer() {

        databinding.videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = databinding.videoSurfaceView.holder

        surfaceHolder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {

                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }
            override fun surfaceChanged(var1: SurfaceHolder, var2: Int,
                                        var3: Int, var4: Int) {
            }
            override fun surfaceDestroyed(var1: SurfaceHolder) {
            }
        })
    }

    private fun setupVideoUI() {
        databinding.episodeDescTextView.visibility = View.INVISIBLE
        databinding.headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()

        databinding.playerControls.setBackgroundColor(Color.argb(255/2,
            0, 0, 0))
    }
}