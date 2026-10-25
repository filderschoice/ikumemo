package com.ikumemo.mobile.shoppinglist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.ItemEdit
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.voice.FieldMic
import kotlinx.coroutines.launch

/**
 * 項目の編集シート（F-04）。品名・数量・メモ・買うお店・写真（F-07）。閉じたときに内容を保存する
 * （確認ダイアログは出さない、PLAN §1.3-4）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    item: ShoppingItemEntity,
    stores: List<StoreEntity>,
    photoStore: PhotoStore,
    onSave: (ItemEdit) -> Unit,
) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.displayName) }
    var quantity by rememberSaveable(item.id) { mutableStateOf(item.quantity.orEmpty()) }
    var note by rememberSaveable(item.id) { mutableStateOf(item.note.orEmpty()) }
    var storeId by rememberSaveable(item.id) { mutableStateOf(item.plannedStoreId) }
    var photo by rememberSaveable(item.id) { mutableStateOf(item.photoUri) }
    val save = { onSave(ItemEdit(name, quantity, note, storeId, photo)) }
    ModalBottomSheet(
        onDismissRequest = save,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditField(name, { name = it }, R.string.edit_name, TestTags.EDIT_NAME, singleLine = true)
            EditField(quantity, { quantity = it }, R.string.edit_quantity, TestTags.EDIT_QUANTITY, singleLine = true)
            EditField(note, { note = it }, R.string.edit_note, TestTags.EDIT_NOTE, singleLine = false)
            PlannedStore(stores = stores, selectedId = storeId, onSelect = { storeId = it })
            PhotoSection(photo = photo, photoStore = photoStore, onPhotoChange = { photo = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = save, modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.EDIT_CLOSE)) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@Composable
internal fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    tag: String,
    singleLine: Boolean,
) {
    // 各欄の横にマイク（F-04）。聞き取った文字は分割せずにそのまま入れる（R-32）
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        trailingIcon = { FieldMic(field = tag, singleLine = singleLine, value = value, onValueChange = onValueChange) },
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

/** 買うお店（F-04）。最近使った順の上位のお店と「なし」から選ぶ。記録の編集（F-16）でも使う。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlannedStore(
    stores: List<StoreEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    title: Int = R.string.edit_planned_store,
) {
    if (stores.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.edit_planned_store_none)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        stores.take(StoreRepository.TOP_STORE_COUNT).forEach { store ->
            FilterChip(
                selected = store.id == selectedId,
                onClick = { onSelect(store.id) },
                label = { Text(store.name) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

/**
 * 写真（F-07）。「撮る」は標準のカメラアプリ（TakePicture、CAMERA 権限は不要）、「選ぶ」は Photo Picker（権限不要）。
 * 取り込んだ写真はアプリ専用領域へ縮小・Exif を消して保存する（[PhotoStore]）。
 */
@Composable
internal fun PhotoSection(
    photo: String?,
    photoStore: PhotoStore,
    onPhotoChange: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val importPhoto = { uri: Uri ->
        scope.launch {
            photoStore.import(uri)?.let(onPhotoChange)
            photoStore.deleteCameraFiles()
        }
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
            cameraUri?.takeIf { taken }?.let { importPhoto(it) }
        }
    val pickPhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { importPhoto(it) }
        }
    Text(stringResource(R.string.edit_photo), style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (photo != null) {
            AsyncImage(
                model = photoStore.fileOf(photo),
                contentDescription = stringResource(R.string.edit_photo_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).testTag(TestTags.EDIT_PHOTO),
            )
        }
        OutlinedButton(
            onClick = { photoStore.newCameraUri().also { cameraUri = it }.let(takePicture::launch) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
            Text(stringResource(R.string.edit_photo_take), modifier = Modifier.padding(start = 4.dp))
        }
        OutlinedButton(
            onClick = {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
            Text(stringResource(R.string.edit_photo_pick), modifier = Modifier.padding(start = 4.dp))
        }
    }
    if (photo != null) {
        TextButton(onClick = { onPhotoChange(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.edit_photo_remove))
        }
    }
}
