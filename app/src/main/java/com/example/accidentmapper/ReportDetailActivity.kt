package com.example.accidentmapper

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.accidentmapper.AccidentReport // Make sure this is imported
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ReportDetailActivity : AppCompatActivity() {

    // Keys for Intent Extras
    companion object {
        const val REPORT_EXTRA = "report_data" // Key for passing the AccidentReport object
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var ivDetailMediaImage: ImageView
    private lateinit var tvNoMediaPlaceholder: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        // 1. Initialize Views
        playerView = findViewById(R.id.video_player_view)
        ivDetailMediaImage = findViewById(R.id.iv_detail_media_image)
        tvNoMediaPlaceholder = findViewById(R.id.tv_no_media_placeholder)

        // 2. Retrieve Report Data
        val report = intent.getSerializableExtra(REPORT_EXTRA) as? AccidentReport

        if (report == null) {
            finish()
            return
        }

        // 3. Bind UI Data
        bindReportToUI(report)

        // 4. Handle Media Display and Playback
        displayMedia(report)
    }

    private fun bindReportToUI(report: AccidentReport) {
        val tvUserName = findViewById<TextView>(R.id.tv_detail_user_name)
        val tvTimestamp = findViewById<TextView>(R.id.tv_detail_timestamp)
        val tvDescription = findViewById<TextView>(R.id.tv_detail_description)
        val ivProfileImage = findViewById<CircleImageView>(R.id.iv_detail_profile_image)

        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

        tvUserName.text = report.reporterName
        tvTimestamp.text = report.timestamp.format(dateFormatter)
        tvDescription.text = report.description

        loadProfileImage(report.reporterProfileUrl, ivProfileImage)
    }

    private fun displayMedia(report: AccidentReport) {
        val mediaUri = report.mediaUri

        if (report.reportType == "Text" || mediaUri.isNullOrEmpty()) {
            tvNoMediaPlaceholder.visibility = View.VISIBLE
            return
        }

        val uri = Uri.parse(mediaUri)

        if (report.reportType == "Video" || report.reportType == "Media") {
            // For video or media (which could be video), initialize ExoPlayer
            playerView.visibility = View.VISIBLE
            initializePlayer(uri)
        } else if (report.reportType == "Image") {
            // For images, use the ImageView
            ivDetailMediaImage.visibility = View.VISIBLE
            Picasso.get().load(uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_error)
                .into(ivDetailMediaImage)
        }
    }

    private fun initializePlayer(mediaUri: Uri) {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val mediaItem = MediaItem.fromUri(mediaUri)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    /** Helper function to load the user's profile image. */
    private fun loadProfileImage(url: String?, imageView: CircleImageView) {
        val placeholderRes = R.drawable.ic_user_placeholder

        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholderRes)
        } else {
            Picasso.get()
                .load(Uri.parse(url))
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(imageView)
        }
    }

    // --- Lifecycle methods for Player Management ---

    override fun onResume() {
        super.onResume()
        // Ensure player is initialized and resumed playback
        if (player == null && playerView.visibility == View.VISIBLE) {
            // Re-initialize player if it was released in onPause (Android best practice)
            val report = intent.getSerializableExtra(REPORT_EXTRA) as? AccidentReport
            if (report != null && !report.mediaUri.isNullOrEmpty()) {
                initializePlayer(Uri.parse(report.mediaUri))
            }
        } else {
            player?.play()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause playback when the activity is not visible
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release player resources
        player?.release()
        player = null
    }
}