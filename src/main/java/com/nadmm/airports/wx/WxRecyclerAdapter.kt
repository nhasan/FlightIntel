package com.nadmm.airports.wx

import android.annotation.SuppressLint
import android.database.Cursor
import android.provider.BaseColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nadmm.airports.databinding.WxListItemBinding
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.WxUtils.getCeiling
import com.nadmm.airports.utils.WxUtils.setColorizedWxDrawable
import java.util.Locale

class WxRecyclerAdapter(
    val cursor: Cursor,
    private val onRecyclerItemClick: (WxListDataModel) -> Unit
): RecyclerView.Adapter<WxRecyclerAdapter.ViewHolder>()
{
    private val idColumnIndex: Int
    private val stationIdToPositionMap: MutableMap<String, Int> = mutableMapOf()
    private val stationMetars = HashMap<String, Metar>()

    init {
        setHasStableIds(true)
        idColumnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
    }

    inner class ViewHolder(val binding: WxListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(model: WxListDataModel) {
            with(binding) {
                wxStationId.text = model.stationId
                wxStationName.text = model.stationName
                wxStationInfo.text = model.stationInfo
                wxStationInfo2.text = model.stationInfo2
                wxStationFreq.text = model.stationFreq
                wxStationPhone.text = model.stationPhone

                root.setOnClickListener {
                    onRecyclerItemClick(model)
                }

            }
        }
    }

    override fun getItemId(position: Int): Long {
        cursor.moveToPosition(position)
        return cursor.getLong(idColumnIndex)
    }

    override fun getItemCount() = cursor.count

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = WxListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        cursor.moveToPosition(position)
        val model = WxListDataModel.fromCursor(cursor)
        holder.bind(model)
        stationIdToPositionMap.put(model.stationId, position)
        getMetar(model.stationId)?.let { metar ->
            showMetarInfo(holder, model, metar)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cursor.close()
    }

    fun onMetarFetched(metar: Metar) {
        metar.stationId?.let { stationId ->
            Log.d("WxRecyclerAdapter", "metar fetched for $stationId")
            stationMetars[stationId] = metar
            stationIdToPositionMap[stationId]?.let { position ->
                notifyItemChanged(position)
            }
        }
    }

    private fun getMetar(stationId: String) = stationMetars.getOrDefault(stationId, null)

    @SuppressLint("SetTextI18n")
    fun showMetarInfo(holder: ViewHolder, model: WxListDataModel, metar: Metar?) {
        with(holder.binding) {
            if (metar == null || !metar.isValid) {
                setColorizedWxDrawable(wxStationName, metar, 0f)
                if (metar != null) {
                    wxStationWx.text = "Wx station in inoperative"
                } else {
                    wxStationWx.text = "Wx not fetched"
                }
                wxStationWx2.visibility = View.GONE
                wxReportAge.visibility = View.GONE
                return
            }

            setColorizedWxDrawable(wxStationName, metar, model.declination)

            wxStationWx.text = buildString {
                if (metar.windSpeedKnots < Int.MAX_VALUE) {
                    if (metar.windSpeedKnots == 0) {
                        append("calm")
                    } else if (metar.windGustKnots < Int.MAX_VALUE) {
                        append(
                            String.format(
                                Locale.US, "%dG%dKT",
                                metar.windSpeedKnots, metar.windGustKnots
                            )
                        )
                    } else {
                        append(String.format(Locale.US, "%dKT", metar.windSpeedKnots))
                    }
                    if (metar.windSpeedKnots > 0 && metar.windDirDegrees >= 0 && metar.windDirDegrees < Int.MAX_VALUE) {
                        append("/")
                        append(FormatUtils.formatDegrees(metar.windDirDegrees))
                    }
                }
                if (isNotEmpty()) {
                    append(", ")
                }
                // Do some basic sanity checks on values
                if (metar.tempCelsius < Float.MAX_VALUE
                    && metar.dewpointCelsius < Float.MAX_VALUE
                ) {
                    append(FormatUtils.formatTemperatureF(metar.tempCelsius))
                    append("/")
                    append(FormatUtils.formatTemperatureF(metar.dewpointCelsius))
                    append(", ")
                }
                if (metar.altimeterHg < Float.MAX_VALUE) {
                    append(FormatUtils.formatAltimeterHg(metar.altimeterHg))
                }
            }
            wxStationWx.visibility = View.VISIBLE

            wxStationWx2.text = buildString {
                append(metar.flightCategory)
                if (metar.wxList.isNotEmpty()) {
                    for (wx in metar.wxList) {
                        if (wx.symbol != "NSW") {
                            append(", ")
                            append(wx.toString().lowercase())
                        }
                    }
                }
                if (metar.visibilitySM < Float.MAX_VALUE) {
                    append(", ")
                    append(FormatUtils.formatStatuteMiles(metar.visibilitySM))
                }
                append(", ")
                var sky = getCeiling(metar.skyConditions)
                val ceiling = sky.cloudBaseAGL
                var skyCover = sky.skyCover
                if (skyCover == "OVX") {
                    append("Ceiling indefinite")
                } else if (skyCover != "NSC") {
                    append("Ceiling ")
                    append(FormatUtils.formatFeet(ceiling.toFloat()))
                } else {
                    if (metar.skyConditions.isNotEmpty()) {
                        sky = metar.skyConditions[0]
                        skyCover = sky.skyCover
                        if (skyCover == "CLR" || skyCover == "SKC") {
                            append("Sky clear")
                        } else if (skyCover != "SKM") {
                            append(skyCover)
                            append(" ")
                            append(FormatUtils.formatFeet(sky.cloudBaseAGL.toFloat()))
                        }
                    }
                }
            }
            wxStationWx.visibility = View.VISIBLE

            wxReportAge.text = TimeUtils.formatElapsedTime(metar.observationTime)
        }
    }
}