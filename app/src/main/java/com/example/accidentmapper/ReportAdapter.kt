package com.example.accidentmapper.adapters

import android.content.Intent // Import for launching activities
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.accidentmapper.R
import com.example.accidentmapper.AccidentReport
import com.example.accidentmapper.ReportDetailActivity // Needed for the Intent
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ReportAdapter(private var reportsList: List<AccidentReport>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

    fun updateReports(newReports: List<AccidentReport>) {
        reportsList = newReports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_accident_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportsList[position]
        holder.bind(report)
    }

    override fun getItemCount(): Int = reportsList.size

    // 🔑 FIX: The ViewHolder must implement View.OnClickListener
    inner class ReportViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener { // <--- THIS LINE IS THE FIX!

        private val ivProfileImage: CircleImageView = itemView.findViewById(R.id.iv_profile_image)
        private val tvUserHeader: TextView = itemView.findViewById(R.id.tv_user_header)
        private val tvLocationTime: TextView = itemView.findViewById(R.id.tv_location_time)
        private val tvDetails: TextView = itemView.findViewById(R.id.tv_details)
        private val ivAccidentMedia: ImageView = itemView.findViewById(R.id.iv_accident_media)

        init {
            // Now 'this' is recognized as a View.OnClickListener
            itemView.setOnClickListener(this)
        }

        fun bind(report: AccidentReport) {
            // 1. User Name and Profile Image
            tvUserHeader.text = report.reporterName
            loadProfileImage(report.reporterProfileUrl, ivProfileImage)

            // 2. Load the accident image/video thumbnail
            loadAccidentMedia(report.mediaUri, ivAccidentMedia, report.reportType)

            // 3. Time Stamp (Formatted)
            val formattedTime = report.timestamp.format(dateFormatter)
            tvLocationTime.text = "Time: $formattedTime | Location: Captured"

            // 4. Accident Details
            tvDetails.text = report.description
        }

        // The implementation of the interface method
        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val report = reportsList[position]
                val context = itemView.context

                // Launch the ReportDetailActivity, passing the report object
                val intent = Intent(context, ReportDetailActivity::class.java).apply {
                    putExtra(ReportDetailActivity.REPORT_EXTRA, report)
                }
                context.startActivity(intent)
            }
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

        /** Helper function: Loads the accident image/media from URI. */
        private fun loadAccidentMedia(uriString: String?, imageView: ImageView, reportType: String) {
            if (!uriString.isNullOrEmpty() && reportType != "Text") {
                Picasso.get()
                    .load(Uri.parse(uriString))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error)
                    .into(imageView)

                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
}