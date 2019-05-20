package register.before.production.playfinity.raw

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.SensorEventAttributes
import io.playfinity.sdk.bluetooth.BluetoothDataFlagsTrampoline
import io.playfinity.sdk.bluetooth.SensorFlags
import kotlinx.android.synthetic.main.sensor_raw_data_list_view.view.*
import java.util.*

data class JumpDataListItem(
        val actionCount: Int,
        val yaw: String,
        val pitch: String,
        val height: String,
        val airtime: String,
        val orientations: List<SensorEventAttributes>) {
    constructor(actionCount: Int, flags: SensorEvent) : this(
            actionCount,
            "${flags.yawRotation}",
            "${flags.pitchRotation}",
            "${flags.heightBallEvent}",
            valueFormat.format(formatLocale, flags.airTimeMilliseconds),
            flags.attributes
    )

    constructor(actionCount: Int) : this (
            actionCount,"Yaw","Pitch","Height","Airtime", listOf()
    ) {

    }

    companion object {
        private const val valueFormat = "%.1f"
        private val formatLocale = Locale.US
    }
}

class JumpDataViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(item: JumpDataListItem) {
        itemView.actionCount.text = "${item.actionCount}"
        itemView.yaw.text = item.yaw
        itemView.pitch.text = item.pitch
        itemView.heightLbl.text = item.height
        itemView.airtime.text = item.airtime
        itemView.jumpOrientation.text = "Jump"
        if (item.orientations.contains(SensorEventAttributes.LandFeet))
            itemView.landOrientation.text = "Land feet"
        if (item.orientations.contains(SensorEventAttributes.LandBack))
            itemView.landOrientation.text = "Land back"
        if (item.orientations.contains(SensorEventAttributes.LandFront))
            itemView.landOrientation.text = "Land front"
        if (item.orientations.contains(SensorEventAttributes.LandHand))
            itemView.landOrientation.text = "Land hand"
    }
}

class JumpDataListAdapter : RecyclerView.Adapter<JumpDataViewHolder>() {

    private val items = mutableListOf<JumpDataListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JumpDataViewHolder {
        return JumpDataViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.sensor_raw_data_list_view, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(viewHolder: JumpDataViewHolder, position: Int) {
        viewHolder.bind(items[position])
    }

    fun getItems(): List<JumpDataListItem> = items

    fun submit(jumpDataListItems: List<JumpDataListItem>) {
        val positionStart = items.size + 1
        items.addAll(jumpDataListItems)
        notifyItemRangeInserted(positionStart, jumpDataListItems.size)
        // scroll to end of table
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}