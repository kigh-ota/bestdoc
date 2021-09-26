package com.kaiichiro.bestdoc

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Configuration
class FirestoreConfiguration {
    @Bean
    fun firestore(): Firestore {
        val path = Path.of(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
        Files.deleteIfExists(path)
        Files.createFile(path);
        Files.writeString(
            path,
            System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON"),
            StandardOpenOption.WRITE
        )
        val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId(
            ServiceOptions.getDefaultProjectId()
        )
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
        return firestoreOptions.service
    }
}

@Component
class FirestoreNoteRepository(private val db: Firestore) : NoteRepository {
    companion object {
        private const val COLLECTION = "notes"
        private val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendOffsetId()
            .toFormatter()
    }

    override fun findAll(): Iterable<Note> {
        val query = db.collection(COLLECTION).get()
        return query.get().documents.map(::docToNote)
    }

    override fun findById(id: NoteId): Note {
        val docRef = db.collection(COLLECTION).document(id)
        val doc = docRef.get().get()
        return docToNote(doc)
    }

    private fun docToNote(doc: DocumentSnapshot): Note {
        if (doc.data == null) {
            throw RuntimeException()
        }
        return Note(
            doc.id,
            doc.get("title") as String,
            doc.get("text") as String,
            OffsetDateTime.parse(doc.get("createdAt") as String, DATE_TIME_FORMATTER),
            OffsetDateTime.parse(doc.get("updatedAt") as String, DATE_TIME_FORMATTER)
        )
    }

    override fun save(note: Note): NoteId {
        return when (note.id) {
            null -> add(note)
            else -> {
                update(note)
                return note.id
            }
        }
    }

    private fun add(note: Note): NoteId {
        val result = db.collection(COLLECTION).add(
            mapOf(
                Pair("title", note.title),
                Pair("text", note.text),
                Pair("createdAt", note.createdAt.format(DATE_TIME_FORMATTER)),
                Pair("updatedAt", note.updatedAt.format(DATE_TIME_FORMATTER)),
            )
        )
        val id = result.get().id
        System.out.println("id : " + id)
        return id
    }

    private fun update(note: Note) {
        val docRef = db.collection(COLLECTION).document(note.id!!)
        val future = docRef.update(
            mapOf(
                Pair("title", note.title),
                Pair("text", note.text),
                Pair("updatedAt", note.updatedAt.format(DATE_TIME_FORMATTER))
            )
        )
        System.out.println("update time : " + future.get().updateTime)
    }
}
