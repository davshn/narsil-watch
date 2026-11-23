package tech.davshn.narsilwatch.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import tech.davshn.narsilwatch.data.StatusRepository
import tech.davshn.narsilwatch.domain.Limit
import tech.davshn.narsilwatch.presentation.MainActivity

// Claude's 5-hour limit on the watch face: an arc that turns yellow at 60 %
// and red at 85 %, as Narsil colors it.
class LimitComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val limit = StatusRepository(this).cached()?.limits?.find { it.key == "fiveHour" }
            ?: return NoDataComplicationData()
        return data(request.complicationType, limit)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        data(type, Limit("fiveHour", "5 horas", 62.0, "#F5B400"))

    private fun data(type: ComplicationType, limit: Limit): ComplicationData? {
        val percent = limit.percent.coerceIn(0.0, 100.0).toFloat()
        val value = PlainComplicationText.Builder("${percent.toInt()}%").build()
        val title = PlainComplicationText.Builder("5h").build()
        val description = PlainComplicationText.Builder("Claude, límite de 5 horas: ${percent.toInt()} %").build()
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return when (type) {
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(percent, 0f, 100f, description)
                    .setText(value)
                    .setTitle(title)
                    // seven equal steps (the most a ramp takes): green to 57 %,
                    // yellow to 86 %, red after, close to Narsil's 60 / 85
                    .setColorRamp(ColorRamp(intArrayOf(GREEN, GREEN, GREEN, GREEN, YELLOW, YELLOW, RED), false))
                    .setTapAction(tap)
                    .build()
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(value, description)
                    .setTitle(title)
                    .setTapAction(tap)
                    .build()
            else -> null
        }
    }

    private companion object {
        const val GREEN = 0xFF22B573.toInt()
        const val YELLOW = 0xFFF5B400.toInt()
        const val RED = 0xFFF0435A.toInt()
    }
}
