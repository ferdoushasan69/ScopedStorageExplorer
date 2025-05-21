package com.example.mediavault

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(fileManager: FileManager) {

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    var isDeleting by remember { mutableStateOf(false) }
    var summaryEdit by remember { mutableStateOf(Summary("", "")) }
    var isEdit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(emptyList<Summary>()) }

    var type by remember { mutableStateOf(Type.INTERNAL) }

    val permission = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE

            ),
    )
    val context = LocalContext.current

    val activityResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {result->
            result.data?.data?.let { folderUri ->
                context.contentResolver.takePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                fileManager.setUri(folderUri)

            }

    }

    LaunchedEffect(key1 = Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            activityResult.launch(intent)
        } else {
            permission.launchMultiplePermissionRequest()
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(fileManager.getSummaryFlow()) {
        fileManager.getSummaryFlow().flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest {
                uiState = it
            }
    }



    if (showBottomSheet){

    ModalBottomSheet(
        onDismissRequest = {
            showBottomSheet = false
            scope.launch {
                sheetState.hide()
            }
        },
        modifier = Modifier.safeContentPadding(),
        sheetState = sheetState,
        content = {
            Form(
                summary = summaryEdit,
                type = type,
                targetType = Type.INTERNAL,
                onTypeChanged = { type = it },
                onClick = { title, desc ->
                    scope.launch {
                        val updateList = withContext(Dispatchers.IO) {

                    if (isEdit) {
                        val summary = Summary(title.trim(), desc.trim(), type)
                        fileManager.update(summary)
                    } else {
                        val summary = Summary(title.trim(), desc.trim(), type)
                        fileManager.save(summary)
                    }
                        }
                        uiState = updateList
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        },
    )
    }

    // Observe sheet state changes
    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            showBottomSheet = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showBottomSheet = true
                isEdit = false // Explicitly set to false for new items
                summaryEdit = Summary("", "") // Reset the form
                type = Type.INTERNAL // Reset the type
                showBottomSheet = true // Duplicate
                scope.launch {
                    sheetState.show()
                }
                scope.launch { // Duplicate launch and show
                    sheetState.show()
                }
            }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) {
        if (uiState.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                Text("No Data found")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(it)) {
                items(uiState) {
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                isEdit = true
                                summaryEdit = it
                                showBottomSheet = true
                                type = it.type
                                scope.launch { sheetState.show() }
                            }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    it.fileName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    it.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = it.type.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            IconButton(onClick = {
                                isDeleting = true
                                scope.launch {
                                    val updateList = withContext(Dispatchers.IO) {

                                     fileManager.delete(it)
                                    }
                                    uiState =updateList
                                    isDeleting = false
                                }


                            }, enabled = !isDeleting) {
                                if (isDeleting){
                                    CircularProgressIndicator()
                                }else{

                                Icon(
                                    Icons.Default.Delete,
                                    modifier = Modifier.size(48.dp),
                                    contentDescription = null
                                )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Form(
    summary: Summary,
    type: Type,
    targetType: Type,
    onTypeChanged: (Type) -> Unit,
    onClick: (String, String) -> Unit
) {

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    LaunchedEffect(summary) {
        if (summary.fileName.isNotBlank()) {
            title = summary.fileName
            desc = summary.summary
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomTextField(
            placeHolderText = "Book Name",
            textValue = title
        ) { title = it }

        CustomTextField(
            placeHolderText = "Summary",
            textValue = desc
        ) { desc = it }

        CustomCheckBox(
            type = type,
            targetType = targetType,
            text = "Internal"
        ) {
            onTypeChanged(Type.INTERNAL)
        }


        CustomCheckBox(
            type = type,
            targetType = Type.PRIVATE_EXTERNAL,
            text = "Privet External"
        ) {
            onTypeChanged(Type.PRIVATE_EXTERNAL)
        }
        CustomCheckBox(
            type = type,
            targetType = Type.SHARED,
            text = "Shared"
        ) {
            onTypeChanged(Type.SHARED)
        }

        Button(
            onClick = { onClick(title, desc) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) { Text("Save") }
    }

}

@Composable
fun CustomCheckBox(
    modifier: Modifier = Modifier, type: Type,
    targetType: Type,
    text: String,
    onTypeChanged: (Type) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = type == targetType,
            onCheckedChange = {
                onTypeChanged(targetType)
            },
        )
        Text(text = text)
    }
    Spacer(modifier = Modifier.height(8.dp))


}

@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    placeHolderText: String,
    textValue: String,
    onValueChangeText: (String) -> Unit
) {
    TextField(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .border(width = 1.dp, color = Color.LightGray)
            .clip(RoundedCornerShape(12.dp)),
        onValueChange = onValueChangeText,
        value = textValue,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,

            ),
        placeholder = { Text(placeHolderText) },
        singleLine = true,

        )
    Spacer(modifier = Modifier.height(8.dp))


}