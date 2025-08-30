package com.nadmm.airports.dof

import android.database.Cursor
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.databinding.DofListItemBinding
import com.nadmm.airports.utils.FormatUtils

class DofRecyclerAdapter(
    val cursor: Cursor
): RecyclerView.Adapter<DofRecyclerAdapter.ViewHolder>()
{
    private val idColumnIndex: Int

    init {
        setHasStableIds(true)
        idColumnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
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

    override fun getItemId(position: Int): Long {
        cursor.moveToPosition(position)
        return cursor.getLong(idColumnIndex)
    }

    override fun getItemCount() = cursor.count

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DofListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (cursor.moveToPosition(position)) {
            val model = DofListDataModel.fromCursor(cursor)
            holder.bind(model)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cursor.close()
    }
}