package tech.davshn.narsilwatch.tile

import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import tech.davshn.narsilwatch.R
import tech.davshn.narsilwatch.data.StatusRepository
import tech.davshn.narsilwatch.domain.Limit
import tech.davshn.narsilwatch.domain.NarsilState
import tech.davshn.narsilwatch.domain.Palette
import tech.davshn.narsilwatch.domain.ageLabel
import tech.davshn.narsilwatch.domain.needsYou
import tech.davshn.narsilwatch.domain.parseColor
import tech.davshn.narsilwatch.domain.statusColor
import tech.davshn.narsilwatch.presentation.MainActivity
import tech.davshn.narsilwatch.sync.Refresh

// The tile: the 5-hour limit as the outer ring, the week as the inner one,
// Narsil in the middle in her current pose, and who needs you.
class NarsilTileService : TileService() {
    private val repo by lazy { StatusRepository(this) }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture {
            if (System.currentTimeMillis() - repo.checkedAt > STALE_MS) Refresh.now(this@NarsilTileService)
            val state = repo.cached()
            TileBuilders.Tile.Builder()
                .setResourcesVersion(resourcesVersion())
                .setFreshnessIntervalMillis(STALE_MS)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(state)))
                .build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture {
            val art = repo.art()
            val image = if (art != null) {
                ResourceBuilders.ImageResource.Builder()
                    .setInlineResource(
                        ResourceBuilders.InlineImageResource.Builder()
                            .setData(art)
                            .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                            .setWidthPx(ART_W)
                            .setHeightPx(ART_H)
                            .build(),
                    )
                    .build()
            } else {
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(R.drawable.narsil_preview).build(),
                    )
                    .build()
            }
            ResourceBuilders.Resources.Builder()
                .setVersion(requestParams.version)
                .addIdToImageMapping(ART_ID, image)
                .build()
        }

    private fun resourcesVersion(): String = repo.art()?.contentHashCode()?.toString() ?: "preview"

    private fun layout(state: NarsilState?): LayoutElement {
        val fiveHour = state?.limits?.find { it.key == "fiveHour" }
        val week = state?.limits?.find { it.key == "week" }
        val center = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Image.Builder()
                    .setResourceId(ART_ID)
                    .setWidth(dp(40f))
                    .setHeight(dp(66f))
                    .build(),
            )
            .addContent(text(limitLine(fiveHour, "5 h"), 13f, fiveHour?.let { parseColor(it.color) } ?: Palette.DIM, bold = true))
            .addContent(text(limitLine(week, "sem"), 11f, week?.let { parseColor(it.color) } ?: Palette.DIM))
            .addContent(text(fleetLine(state), 11f, fleetColor(state)))
            .addContent(text(state?.let { ageLabel(it.at, System.currentTimeMillis() / 1000) } ?: "configúrame", 9f, Palette.DIM))
            .build()

        val inner = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(RING + GAP)).build()).build())
            .addContent(ring(week, RING_INNER))
            .addContent(center)
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(ModifiersBuilders.Background.Builder().setColor(argb(Palette.BG)).build())
                    .setClickable(openApp())
                    .build(),
            )
            .addContent(ring(fiveHour, RING))
            .addContent(inner)
            .build()
    }

    // A full ring: the used part in the limit's color, the rest as track.
    private fun ring(limit: Limit?, thickness: Float): LayoutElement {
        val used = ((limit?.percent ?: 0.0).coerceIn(0.0, 100.0) * 3.6).toFloat()
        val arc = LayoutElementBuilders.Arc.Builder()
            .setAnchorAngle(degrees(0f))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        if (used > 0f) arc.addContent(arcLine(used, thickness, limit?.let { parseColor(it.color) } ?: Palette.DIM))
        if (used < 360f) arc.addContent(arcLine(360f - used, thickness, Palette.TRACK))
        return arc.build()
    }

    private fun arcLine(length: Float, thickness: Float, color: Int) =
        LayoutElementBuilders.ArcLine.Builder()
            .setLength(degrees(length))
            .setThickness(dp(thickness))
            .setColor(argb(color))
            .build()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(1)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb(color))
                    .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                    .build(),
            )
            .build()

    private fun limitLine(limit: Limit?, short: String) = limit?.let { "$short ${it.percent.toInt()}%" } ?: "$short –"

    private fun fleetLine(state: NarsilState?): String {
        val sessions = state?.sessions.orEmpty()
        val waiting = needsYou(sessions)
        return when {
            waiting == 1 -> "1 te necesita"
            waiting > 1 -> "$waiting te necesitan"
            sessions.isEmpty() -> "sin conversaciones"
            else -> sessions.first().label.lowercase()
        }
    }

    private fun fleetColor(state: NarsilState?): Int =
        state?.sessions?.firstOrNull()?.let { statusColor(it.status) } ?: Palette.DIM

    private fun openApp() = ModifiersBuilders.Clickable.Builder()
        .setId("open")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build(),
                )
                .build(),
        )
        .build()

    private companion object {
        const val ART_ID = "narsil"
        // the art Narsil publishes is 91 x 150
        const val ART_W = 91
        const val ART_H = 150
        const val RING = 7f
        const val RING_INNER = 5f
        const val GAP = 3f
        const val STALE_MS = 15 * 60_000L
    }
}
