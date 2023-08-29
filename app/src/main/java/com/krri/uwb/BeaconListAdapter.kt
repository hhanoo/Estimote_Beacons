package com.krri.uwb

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krri.uwb.databinding.ItemListBinding

class BeaconListAdapter(diffUtil: DiffUtil.ItemCallback<BeaconData>) :
    ListAdapter<BeaconData, BeaconListAdapter.BeaconViewHolder>(diffUtil) {

    // 각 뷰들을 binding 사용하여 View 연결
    inner class BeaconViewHolder(var binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: BeaconData) {
            binding.apply {
                binding.deviceId.text = data.id
                binding.deviceRssi.text = data.rssi.toString()
                binding.deviceDistance.text = data.distance.toString()
            }
        }
    }

    // View 생성될 때 호출되는 method
    // View 생성, RecyclerView가 생성될 때 호출
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BeaconViewHolder {
        val binding =
            ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BeaconViewHolder(binding)
    }

    // View 바인드 될 때 호출되는 method
    // View에 내용이 작성되는 method, 스크롤을 올리거나 내릴 때마다 호출
    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}
