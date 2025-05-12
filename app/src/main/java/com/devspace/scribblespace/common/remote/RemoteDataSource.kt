package com.devspace.scribblespace.common.remote

import com.devspace.scribblespace.common.model.NoteData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.Exception

private const val NOTE_COLLECTION = "notes"

class RemoteDataSource private constructor(
    private val database: FirebaseFirestore
) {

    suspend fun addNote(title: String, description: String): Result<String> {
        return try {
            val noteMap = hashMapOf(
                "title" to title,
                "description" to description
            )

            val addedDocument = withTimeout(2000) {
                database.collection(NOTE_COLLECTION)
                    .add(noteMap)
                    .await()
            }

            Result.success(addedDocument.id)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }


    suspend fun getNotes(): Result<List<NoteData>> {
        return try {
            //Snapshot query
            val querySnapshot = database
                .collection(NOTE_COLLECTION)
                .get()
                .await()

            //Get notes
            val notesFromRemote = querySnapshot.documents.mapNotNull { noteFromDb ->
                noteFromDb.toObject(NoteRemoteData::class.java)
                    ?.copy(id = noteFromDb.id)
            }

            val notesData: MutableList<NoteData> = mutableListOf()
            notesFromRemote.forEach { note ->
                if (note.id != null && note.title != null && note.description != null) {
                    notesData.add(
                        NoteData(
                            key = note.id,
                            title = note.title,
                            description = note.description
                        )
                    )
                }
            }
            Result.success(notesData)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }


    suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            withTimeout(2000) {


                database.collection(NOTE_COLLECTION)
                    .document(id)
                    .delete()
                    .await()
                Result.success(Unit)
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    companion object {
        fun create(): RemoteDataSource {
            return RemoteDataSource(database = FirebaseFirestore.getInstance())
        }
    }
}