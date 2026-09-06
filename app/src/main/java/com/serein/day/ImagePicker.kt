package com.serein.day

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * 图片来源选择弹层（Days Matter 式）：拍照 / 系统相册 / 文件。
 * 「文件」走 DocumentsUI，可绕开部分 ROM 对系统相册的裁剪劫持。
 * 选中后通过 onImage 返回内容 Uri；回调触发时才关闭弹层，保证 launcher 在组合内存活。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceDialog(
    title: String,
    hasCurrent: Boolean,
    onDismiss: () -> Unit,
    onImage: (Uri) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onImage(uri)
        }
        onDismiss()
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onImage(uri)
        }
        onDismiss()
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            onImage(uri)
        }
        onDismiss()
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            title,
            color = LocalSerein.current.onSurface,
            fontSize = 17.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        SourceRow(Icons.Outlined.PhotoCamera, "拍照") { launchCamera() }
        SourceRow(Icons.Outlined.PhotoLibrary, "从相册选择") {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        SourceRow(Icons.Outlined.Folder, "从文件选择") {
            documentLauncher.launch("image/*")
        }
        if (hasCurrent && onRemove != null) {
            SourceRow(Icons.Outlined.Delete, "恢复默认（移除图片）", tint = LocalSerein.current.error) {
                onRemove()
                onDismiss()
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = LocalSerein.current.onSurface,
    onClick: () -> Unit
) {
    val s = LocalSerein.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Spacer(Modifier.width(1.dp))
    }
}
