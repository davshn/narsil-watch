package tech.davshn.narsilwatch.presentation

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import java.time.LocalTime
import kotlinx.coroutines.launch
import tech.davshn.narsilwatch.R
import tech.davshn.narsilwatch.data.StatusRepository
import tech.davshn.narsilwatch.data.StatusResult
import tech.davshn.narsilwatch.domain.Limit
import tech.davshn.narsilwatch.domain.NarsilState
import tech.davshn.narsilwatch.domain.Palette
import tech.davshn.narsilwatch.domain.Session
import tech.davshn.narsilwatch.domain.ageLabel
import tech.davshn.narsilwatch.domain.isDay
import tech.davshn.narsilwatch.domain.parseColor
import tech.davshn.narsilwatch.domain.resetLabel
import tech.davshn.narsilwatch.domain.statusColor
import tech.davshn.narsilwatch.sync.Refresh

// The palette of the time of day, light from 6 to 18 and dark otherwise.
private val LocalPalette = staticCompositionLocalOf { Palette.NIGHT }

private val Bg: Color @Composable @ReadOnlyComposable get() = Color(LocalPalette.current.bg)
private val Ink: Color @Composable @ReadOnlyComposable get() = Color(LocalPalette.current.text)
private val Dim: Color @Composable @ReadOnlyComposable get() = Color(LocalPalette.current.dim)
private val Track: Color @Composable @ReadOnlyComposable get() = Color(LocalPalette.current.track)
private val Accent: Color @Composable @ReadOnlyComposable get() = Color(LocalPalette.current.accent)

private fun narsilColors(p: Palette) = ColorScheme(
    primary = Color(p.accent),
    onPrimary = Color(p.text),
    background = Color(p.bg),
    onBackground = Color(p.text),
    onSurface = Color(p.text),
    onSurfaceVariant = Color(p.dim),
    surfaceContainer = Color(p.track),
)

class MainActivity : ComponentActivity() {
    private var isDayNow by mutableStateOf(isDay(LocalTime.now().hour))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = StatusRepository(this)
        setupFrom(intent, repo)
        setContent {
            val palette = if (isDayNow) Palette.DAY else Palette.NIGHT
            CompositionLocalProvider(LocalPalette provides palette) {
                MaterialTheme(colorScheme = narsilColors(palette)) {
                    AppScaffold { NarsilScreen(repo, isDayNow) }
                }
            }
        }
    }

    // Back to the app after 6 or 18: turn with the hour.
    override fun onResume() {
        super.onResume()
        isDayNow = isDay(LocalTime.now().hour)
    }

    // Setup from the PC, without typing on the watch:
    // adb shell am start -n tech.davshn.narsilwatch/.presentation.MainActivity --es topic narsil-... --es key ...
    private fun setupFrom(intent: Intent?, repo: StatusRepository) {
        val topic = intent?.getStringExtra("topic") ?: return
        repo.topic = topic
        intent.getStringExtra("key")?.let { repo.key = it }
        Refresh.now(this)
    }
}

@Composable
private fun NarsilScreen(repo: StatusRepository, day: Boolean) {
    var isSetup by remember { mutableStateOf(!repo.isConfigured) }
    var state by remember { mutableStateOf(repo.cached()) }
    var art by remember(day) { mutableStateOf(repo.art(day)) }
    var problem by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        isLoading = true
        scope.launch {
            val result = repo.refresh()
            problem = when (result) {
                is StatusResult.Ok -> null
                StatusResult.NeedsKey -> "El estado va sellado: falta la clave o no es la de /narsil movil clave."
                StatusResult.Empty -> "Aún no hay estado: Narsil lo publica cuando escribes lejos del PC."
            }
            state = repo.cached()
            art = repo.art(day)
            isLoading = false
            Refresh.redraw(repo.appContext)
        }
    }

    LaunchedEffect(isSetup) { if (!isSetup) refresh() }

    if (isSetup) {
        SetupScreen(repo) {
            isSetup = false
        }
        return
    }

    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = padding,
            modifier = Modifier.background(Bg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { NarsilHeader(art, state) }
            problem?.let { p -> item { Note(p) } }
            val limits = state?.limits.orEmpty()
            if (limits.isNotEmpty()) {
                item { ListHeader { Text("Límites de Claude") } }
                limits.forEach { limit -> item { LimitRow(limit) } }
            }
            val sessions = state?.sessions.orEmpty()
            item { ListHeader { Text(if (sessions.isEmpty()) "Sin conversaciones" else "Conversaciones") } }
            sessions.forEach { session -> item { SessionRow(session) } }
            item {
                Text(
                    state?.let { ageLabel(it.at, System.currentTimeMillis() / 1000) } ?: "",
                    color = Dim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            item {
                Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isLoading) "Leyendo…" else "Actualizar")
                }
            }
            item {
                Button(onClick = { isSetup = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Configurar")
                }
            }
        }
    }
}

@Composable
private fun NarsilHeader(art: ByteArray?, state: NarsilState?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val bitmap = remember(art) { art?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "Narsil", modifier = Modifier.size(width = 48.dp, height = 80.dp))
        } else {
            Image(painter = painterResource(R.drawable.narsil_preview), contentDescription = "Narsil", modifier = Modifier.size(width = 48.dp, height = 80.dp))
        }
        Text("Narsil", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        state?.xp?.let { Text(it, color = Dim, fontSize = 11.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun LimitRow(limit: Limit) {
    val color = Color(parseColor(limit.color))
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(limit.label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${limit.percent.toInt()} %", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Track)) {
            Box(
                Modifier
                    .fillMaxWidth((limit.percent / 100).toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
        val reset = resetLabel(limit.resetsAt, System.currentTimeMillis() / 1000)
        if (reset.isNotEmpty()) Text(reset, color = Dim, fontSize = 10.sp)
    }
}

@Composable
private fun SessionRow(session: Session) {
    val color = Color(statusColor(session.status))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Track)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(session.title, color = Ink, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(session.label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = Color(0xFFF5B400), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
}

@Composable
private fun SetupScreen(repo: StatusRepository, onDone: () -> Unit) {
    val topic = rememberTextFieldState(repo.topic)
    val key = rememberTextFieldState(repo.key)
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { padding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = padding,
            modifier = Modifier.background(Bg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { ListHeader { Text("Configurar") } }
            item { Text("El tema de /narsil movil", color = Dim, fontSize = 11.sp) }
            item { Field(topic, "narsil-…") }
            item { Text("La clave, si usas /narsil movil clave", color = Dim, fontSize = 11.sp, textAlign = TextAlign.Center) }
            item { Field(key, "opcional") }
            item {
                Button(
                    onClick = {
                        repo.topic = topic.text.toString()
                        repo.key = key.text.toString()
                        onDone()
                    },
                    enabled = topic.text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar") }
            }
        }
    }
}

@Composable
private fun Field(state: TextFieldState, hint: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Track)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (state.text.isEmpty()) Text(hint, color = Dim, fontSize = 13.sp)
        BasicTextField(
            state = state,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = TextStyle(color = Ink, fontSize = 13.sp),
            cursorBrush = SolidColor(Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
