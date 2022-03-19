package com.kaiichiro.bestdoc

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.Query
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
class FirestoreConfiguration() {
    @Bean
    fun firestore(): Firestore {
        val path = Path.of(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"))
        Files.deleteIfExists(path)
        Files.createFile(path)
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
        private val log = LoggerFactory.getLogger(FirestoreNoteRepository::class.java)
    }

    override fun findAll(): Iterable<Note> {
        val start = System.currentTimeMillis()
        val query = db.collection(COLLECTION).whereEqualTo("deleted", false).get()
        return query.get().documents.map(::docToNote).also {
            val end = System.currentTimeMillis()
            log.info("findAll() took ${end - start} milliseconds (${it.size} notes)")
        }
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
            doc.get("tags") as List<String>,
            OffsetDateTime.parse(doc.get("createdAt") as String, DATE_TIME_FORMATTER),
            OffsetDateTime.parse(doc.get("updatedAt") as String, DATE_TIME_FORMATTER)
        )
    }

    override fun save(note: Note): Note {
        return when (note.id) {
            null -> add(note)
            else -> update(note)
        }
    }

    override fun delete(id: NoteId) {
        val docRef = db.collection(COLLECTION).document(id)
        val future = docRef.update("deleted", true)
        System.out.println("delete time : " + future.get().updateTime)
    }

    override fun findUpdatedLaterThan(dt: OffsetDateTime): Iterable<Note> {
        val query = db.collection(COLLECTION).orderBy("updatedAt", Query.Direction.DESCENDING)
            .whereGreaterThan("updatedAt", dt.format(DATE_TIME_FORMATTER)).get()
        return query.get().documents.map(::docToNote)
    }

    private fun add(note: Note): Note {
        val result = db.collection(COLLECTION).add(
            mapOf(
                Pair("title", note.title),
                Pair("text", note.text),
                Pair("tags", note.tags),
                Pair("createdAt", note.createdAt.format(DATE_TIME_FORMATTER)),
                Pair("updatedAt", note.updatedAt.format(DATE_TIME_FORMATTER)),
                Pair("deleted", false),
            )
        )
        val docRef = result.get()
        val doc = docRef.get().get()
        return docToNote(doc)
    }

    private fun update(note: Note): Note {
        val docRef = db.collection(COLLECTION).document(note.id!!)
        val future = docRef.update(
            mapOf(
                Pair("title", note.title),
                Pair("text", note.text),
                Pair("updatedAt", note.updatedAt.format(DATE_TIME_FORMATTER))
            )
        )
        System.out.println("update time : " + future.get().updateTime)
        return findById(note.id)
    }
}
