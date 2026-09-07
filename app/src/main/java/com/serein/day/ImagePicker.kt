package com.serein.day

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 图片来源选择弹层（Days Matter 式）：拍照 / 系统相册 / 文件。
 * 「文件」走 DocumentsUI，可绕开部分 ROM 对系统相册的裁剪劫持。
 * 选中后通过 onImage 返回内容 Uri；回调触发时才关闭弹层，保证 launcher 在组合内存活。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageSourceDialog(
    title: String,
    hasCurrent: Boolean,
    onDismiss: () -> Unit,
    onImage: suspend (Uri) -> Boolean,
    onEditCurrent: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun import(uri: Uri?) {
        if (uri == null || importing) return
        scope.launch {
            importing = true
            importError = false
            val success = runCatching { onImage(uri) }.getOrDefault(false)
            importing = false
            if (success) onDismiss() else importError = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        import(uri)
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        import(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok) import(uri)
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val s = LocalSerein.current
    ModalBottomSheet(
        onDismissRequest = { if (!importing) onDismiss() },
        containerColor = if (s.isDark) s.container else androidx.compose.ui.graphics.Color.White
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            title,
            color = LocalSerein.current.onSurface,
            fontSize = 17.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Text(
            "选择后可预览、裁切并调整显示方式",
            color = s.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(6.dp))
        SourceRow(Icons.Outlined.PhotoCamera, "拍照", enabled = !importing) { launchCamera() }
        SourceRow(Icons.Outlined.PhotoLibrary, "从相册选择", enabled = !importing) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        SourceRow(Icons.Outlined.Folder, "从文件选择", enabled = !importing) {
            documentLauncher.launch("image/*")
        }
        if (importing) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), color = s.accent, strokeWidth = 2.dp)
                Text("正在读取图片…", color = s.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        if (importError) {
            Text(
                "无法读取这张图片，请换一张或改用“从文件选择”重试。",
                color = s.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        if (hasCurrent && onEditCurrent != null) {
            SourceRow(Icons.Outlined.Tune, "调整当前图片", enabled = !importing) {
                onEditCurrent()
                onDismiss()
            }
        }
        if (hasCurrent && onRemove != null) {
            SourceRow(Icons.Outlined.Delete, "移除当前图片", tint = LocalSerein.current.error, enabled = !importing) {
                onRemove()
                onDismiss()
            }
        }
        Spacer(Modifier.height(24.dp))
        }
    }
}

/** 选图后的独立编辑步骤：先看预览，再确认缩放方式和透明度。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageEditSheet(
    title: String,
    imageName: String,
    coverStore: CoverStore,
    initialScale: ImageScaleMode,
    initialOpacity: Float,
    targetAspectRatio: Float,
    targetLabel: String,
    cropOutputBase: String,
    onSave: (ImageScaleMode, Float, String?) -> Unit,
    onCancel: () -> Unit
) {
    val s = LocalSerein.current
    var scale by remember(imageName) { mutableStateOf(initialScale) }
    var opacity by remember(imageName) { mutableFloatStateOf(initialOpacity) }
    var cropZoom by remember(imageName) { mutableFloatStateOf(1f) }
    var cropCenterX by remember(imageName) { mutableFloatStateOf(0.5f) }
    var cropCenterY by remember(imageName) { mutableFloatStateOf(0.5f) }
    var previewSize by remember(imageName) { mutableStateOf(IntSize.Zero) }
    var saving by remember(imageName) { mutableStateOf(false) }
    var saveError by remember(imageName) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bitmap = rememberCoverBitmap(imageName, coverStore)
    val imageBounds = if (bitmap != null && previewSize.width > 0 && previewSize.height > 0) {
        fittedImageBounds(bitmap.width, bitmap.height, previewSize.width, previewSize.height)
    } else {
        NormalizedRect(0f, 0f, 1f, 1f)
    }
    val crop = cropFrameFor(
        imageBounds = imageBounds,
        targetAspectRatio = targetAspectRatio,
        previewAspectRatio = previewSize.width.toFloat() / previewSize.height.coerceAtLeast(1),
        zoom = cropZoom,
        centerX = cropCenterX,
        centerY = cropCenterY
    )

    fun saveImage() {
        if (saving || bitmap == null) return
        scope.launch {
            saving = true
            saveError = false
            val cropped = if (scale == ImageScaleMode.CROP) {
                val sourceRect = previewSelectionToSource(
                    selection = crop,
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    previewWidth = previewSize.width,
                    previewHeight = previewSize.height
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    coverStore.cropTo(
                        imageName,
                        cropOutputBase,
                        sourceRect.left,
                        sourceRect.top,
                        sourceRect.right,
                        sourceRect.bottom
                    )
                }
            } else null
            saving = false
            if (scale == ImageScaleMode.CROP && cropped == null) {
                saveError = true
            } else {
                onSave(scale, opacity, cropped)
            }
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = s.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(50)).clickable(enabled = !saving) { onCancel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "取消图片编辑", tint = s.onSurface, modifier = Modifier.size(24.dp))
                    }
                    Text(title, color = s.onSurface, fontSize = 19.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (bitmap != null && !saving) s.accent else s.highest)
                            .clickable(enabled = bitmap != null && !saving) { saveImage() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(16.dp), color = s.onSurfaceVariant, strokeWidth = 2.dp)
                        Text(
                            if (saving) "处理中" else "使用图片",
                            color = if (bitmap != null && !saving) s.onAccent else s.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("预览 · $targetLabel", color = s.onSurfaceVariant, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (targetAspectRatio < 0.8f) 400.dp else 260.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(InkElevated)
                            .onSizeChanged { previewSize = it }
                            .pointerInput(scale, crop, imageBounds) {
                                if (scale == ImageScaleMode.CROP) {
                                    detectDragGestures { change, delta ->
                                        change.consume()
                                        cropCenterX = (cropCenterX + delta.x / size.width).coerceIn(
                                            imageBounds.left + crop.width / 2f,
                                            imageBounds.right - crop.width / 2f
                                        )
                                        cropCenterY = (cropCenterY + delta.y / size.height).coerceIn(
                                            imageBounds.top + crop.height / 2f,
                                            imageBounds.bottom - crop.height / 2f
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap == null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = s.accent, strokeWidth = 2.dp)
                                Text("正在生成预览…", color = OnInkMuted, fontSize = 13.sp)
                            }
                        } else if (scale == ImageScaleMode.CROP) {
                            Canvas(Modifier.fillMaxSize()) {
                                val fit = minOf(size.width / bitmap.width, size.height / bitmap.height)
                                val dw = bitmap.width * fit
                                val dh = bitmap.height * fit
                                val ox = (size.width - dw) / 2f
                                val oy = (size.height - dh) / 2f
                                drawImage(
                                    bitmap,
                                    dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                                    dstSize = IntSize(dw.toInt(), dh.toInt()),
                                    alpha = opacity
                                )
                                val frame = Rect(crop.left * size.width, crop.top * size.height, crop.right * size.width, crop.bottom * size.height)
                                val shade = Color.Black.copy(alpha = 0.5f)
                                drawRect(shade, Offset.Zero, Size(size.width, frame.top))
                                drawRect(shade, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
                                drawRect(shade, Offset(0f, frame.top), Size(frame.left, frame.height))
                                drawRect(shade, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))
                                drawRect(Color.White, Offset(frame.left, frame.top), Size(frame.width, frame.height), style = Stroke(2.dp.toPx()))
                                val thirdX = frame.width / 3f
                                val thirdY = frame.height / 3f
                                for (i in 1..2) {
                                    drawLine(Color.White.copy(alpha = 0.45f), Offset(frame.left + thirdX * i, frame.top), Offset(frame.left + thirdX * i, frame.bottom))
                                    drawLine(Color.White.copy(alpha = 0.45f), Offset(frame.left, frame.top + thirdY * i), Offset(frame.right, frame.top + thirdY * i))
                                }
                            }
                        } else {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "$targetLabel 图片预览",
                                modifier = Modifier.fillMaxSize().alpha(opacity),
                                contentScale = scale.toContentScale()
                            )
                        }
                    }

                    if (scale == ImageScaleMode.CROP) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(s.container).padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("裁切范围", color = s.onSurface, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                Slider(
                                    value = cropZoom,
                                    onValueChange = { cropZoom = it },
                                    valueRange = 1f..3f,
                                    colors = SliderDefaults.colors(thumbColor = s.accent, activeTrackColor = s.accent, inactiveTrackColor = s.highest),
                                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                                )
                            }
                            Text("拖动裁切框选择保留区域；滑动可放大裁切。", color = s.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }

                    Text("显示方式", color = s.onSurfaceVariant, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageScaleMode.entries.forEach { mode ->
                            val selected = mode == scale
                            Text(
                                mode.label,
                                color = if (selected) OnInk else s.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) Ink else s.container)
                                    .clickable(enabled = !saving) { scale = mode }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(s.container).padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.width(76.dp)) {
                                Text("图片强度", color = s.onSurface, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                Text("${(opacity * 100).roundToInt()}%", color = s.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.1f..1f,
                                colors = SliderDefaults.colors(thumbColor = s.accent, activeTrackColor = s.accent, inactiveTrackColor = s.highest),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (saveError) {
                        Text("图片处理失败，请返回后换一张图片重试。", color = s.error, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(22.dp))
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = LocalSerein.current.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Spacer(Modifier.width(1.dp))
    }
}
