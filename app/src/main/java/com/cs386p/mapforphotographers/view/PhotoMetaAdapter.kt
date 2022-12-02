package com.cs386p.mapforphotographers.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cs386p.mapforphotographers.PhotoViewModel
import com.cs386p.mapforphotographers.PhotoViewModel.Companion.doOnePhotoViewing
import com.cs386p.mapforphotographers.databinding.RowBinding
import com.cs386p.mapforphotographers.model.PhotoMeta

class PhotoMetaAdapter(private val viewModel: PhotoViewModel)
    : ListAdapter<PhotoMeta, PhotoMetaAdapter.VH>(Diff()) {
    // This class allows the adapter to compute what has changed
    class Diff : DiffUtil.ItemCallback<PhotoMeta>() {
        override fun areItemsTheSame(oldItem: PhotoMeta, newItem: PhotoMeta): Boolean {
            return oldItem.firestoreID == newItem.firestoreID
        }

        override fun areContentsTheSame(oldItem: PhotoMeta, newItem: PhotoMeta): Boolean {
            return oldItem.firestoreID == newItem.firestoreID
                    && oldItem.pictureTitle == newItem.pictureTitle
                    && oldItem.ownerUid == newItem.ownerUid
                    && oldItem.ownerName == newItem.ownerName
                    && oldItem.uuid == newItem.uuid
                    && oldItem.byteSize == newItem.byteSize
                    && oldItem.timeStamp == newItem.timeStamp
                    && oldItem.likedBy == newItem.likedBy
                    && oldItem.private == newItem.private
                    && oldItem.pictureLat == newItem.pictureLat
                    && oldItem.pictureLng == newItem.pictureLng
                    && oldItem.pictureAperture == newItem.pictureAperture
                    && oldItem.pictureCamera == newItem.pictureCamera
                    && oldItem.pictureDate == newItem.pictureDate
                    && oldItem.pictureDescription == newItem.pictureDescription
                    && oldItem.pictureFocalLength == newItem.pictureFocalLength
                    && oldItem.pictureIso == newItem.pictureIso
                    && oldItem.pictureLens == newItem.pictureLens
                    && oldItem.pictureShutterSpeed == newItem.pictureShutterSpeed
        }
    }

    inner class VH(private val rowBinding: RowBinding) :
        RecyclerView.ViewHolder(rowBinding.root) {

        fun bind(holder: VH, position: Int) {
            //val photoMeta = viewModel.getPhotoMeta(position)
            val photoMeta = getItem(position)
            viewModel.glideFetch(photoMeta.uuid, rowBinding.rowImageView)
            rowBinding.root.setOnClickListener {
                doOnePhotoViewing(rowBinding.root.context, photoMeta)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val rowBinding = RowBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return VH(rowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(holder, position)
    }
}