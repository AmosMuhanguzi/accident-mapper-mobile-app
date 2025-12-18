package com.example.accidentmapper

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class AccidentReportAdapter(
    private val context: Context,
    private var reports: List<AccidentReport>
) : RecyclerView.Adapter<AccidentReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reporterProfileImage: CircleImageView = itemView.findViewById(R.id.reporterProfileImage)
        val reporterNameText: TextView = itemView.findViewById(R.id.reporterNameText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        val mediaPreview: ImageView = itemView.findViewById(R.id.mediaPreview)
        val reportTypeBadge: TextView = itemView.findViewById(R.id.reportTypeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.accident_report_card, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        // 1. Reporter Profile Image
        report.reporterProfileUrl?.let { urlString ->
            if (urlString.isNotEmpty()) {
                Picasso.get()
                    .load(Uri.parse(urlString))
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(holder.reporterProfileImage)
            } else {
                holder.reporterProfileImage.setImageResource(R.drawable.ic_user_placeholder)
            }
        }

        // 2. Report Details
        holder.reporterNameText.text = report.reporterName
        holder.descriptionText.text = report.description
        holder.reportTypeBadge.text = report.reportType.uppercase()

        // 3. Time and Date
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        holder.timestampText.text = report.timestamp.format(formatter)

        // 4. Media Preview (Image/Video)
        report.mediaUri?.let { uriString ->
            holder.mediaPreview.visibility = View.VISIBLE
            // Use Picasso to load the media preview (works for image URIs)
            Picasso.get()
                .load(Uri.parse(uriString))
                .resize(500, 300)
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(holder.mediaPreview)
        } ?: run {
            holder.mediaPreview.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = reports.size

    fun updateReports(newReports: List<AccidentReport>) {
        reports = newReports
        notifyDataSetChanged()
    }
}