package com.example.accidentmapper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccidentAdapter(private val accidents: List<Accident>) :
    RecyclerView.Adapter<AccidentAdapter.AccidentViewHolder>() {

    class AccidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.cardImage)
        val titleView: TextView = itemView.findViewById(R.id.cardTitle)
        val descView: TextView = itemView.findViewById(R.id.cardDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccidentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_accident_card, parent, false)
        return AccidentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccidentViewHolder, position: Int) {
        val accident = accidents[position]
        holder.imageView.setImageResource(accident.imageResId)
        holder.titleView.text = accident.title
        holder.descView.text = accident.description
    }

    override fun getItemCount() = accidents.size
}
