package com.nadmm.airports.dof

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.databinding.DofListItemBinding
import com.nadmm.airports.utils.FormatUtils

/** The nearby obstacles, in the order given: tallest first. */
class DofRecyclerViewAdapter(
    private val obstacles: List<DofListDataModel>
): RecyclerView.Adapter<DofRecyclerViewAdapter.ViewHolder>()
{
    init {
        setHasStableIds(true)
    }

    inner class ViewHolder(val binding: DofListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: DofListDataModel) {
            with(binding) {
                obstacleType.text = model.obstacleType
                heightMsl.text = FormatUtils.formatFeetMsl(model.mslHeight.toFloat())
                heightAgl.text = FormatUtils.formatFeetAgl(model.aglHeight.toFloat())
                markingType.text = model.markingType
                lightingType.text = model.lightingType
                location.text = model.location
            }
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = obstacles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DofListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(obstacles[position])
    }
}
