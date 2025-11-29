package com.example.mynotesapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mynotesapp.data.Note
import com.example.mynotesapp.viewmodel.NoteViewModel
import com.example.mynotesapp.ui.theme.MynotesappTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val noteViewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request microphone permission at startup
        requestAudioPermission()

        setContent {
            MynotesappTheme {
                NotesScreen(noteViewModel)
            }
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteViewModel) {
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as Activity
    val dictationHelper = remember { DictationHelper(activity, viewModel) }
    val dictatedText by dictationHelper.recognizedText   // ✅ now defined

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("My Notes") }) }
    ) { innerPadding ->
        if (selectedNote == null) {
            // List view
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = if (content.isNotBlank()) content else dictatedText,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Row {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                scope.launch {
                                    viewModel.insert(Note(title = title, content = content))
                                    title = ""
                                    content = ""
                                    snackbarHostState.showSnackbar("Note added")
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp).weight(1f)
                    ) {
                        Text("Add Note")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { dictationHelper.toggleListening() },   // ✅ single toggle
                        modifier = Modifier.padding(top = 8.dp).weight(1f)
                    ) {
                        Text(if (dictationHelper.isListening) "Stop Dictation" else "Dictate Note")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(notes) { note ->
                        NotePreviewItem(
                            note = note,
                            onSelect = { selectedNote = note }
                        )
                    }
                }
            }
        } else {
            // Detail view
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(text = selectedNote!!.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = selectedNote!!.content, style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { showEditDialog = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Edit")
                    }
                    Button(
                        onClick = {
                            val noteToDelete = selectedNote
                            if (noteToDelete != null) {
                                selectedNote = null
                                scope.launch {
                                    viewModel.delete(noteToDelete)
                                    snackbarHostState.showSnackbar("Note deleted")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Delete")
                    }
                    Button(onClick = { selectedNote = null }) {
                        Text("Back")
                    }
                }
            }

            // Edit dialog
            if (showEditDialog) {
                var editedTitle by remember { mutableStateOf(selectedNote!!.title) }
                var editedContent by remember { mutableStateOf(selectedNote!!.content) }

                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Note") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editedContent,
                                onValueChange = { editedContent = it },
                                label = { Text("Content") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            scope.launch {
                                val updatedNote = selectedNote!!.copy(
                                    title = editedTitle,
                                    content = editedContent,
                                    timestamp = System.currentTimeMillis()
                                )
                                viewModel.update(updatedNote)
                                selectedNote = updatedNote
                                snackbarHostState.showSnackbar("Note updated")
                            }
                            showEditDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NotePreviewItem(note: Note, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = note.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotesPreview() {
    MynotesappTheme {
        NotePreviewItem(
            Note(title = "Sample Note", content = "Preview content that is longer than two lines to demonstrate truncation."),
            onSelect = {}
        )
    }
}