package com.kaiichiro.bestdoc.migrate

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.firestore.FirestoreOptions
import com.kaiichiro.bestdoc.FirestoreNoteRepository
import com.kaiichiro.bestdoc.Note
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import kotlin.io.path.extension

// TODO tags
fun main() {
    val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId(
        ServiceOptions.getDefaultProjectId()
    )
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()
    val repository = FirestoreNoteRepository(firestoreOptions.service)

    val notableDirectory = Path.of(System.getenv("NOTABLE_NOTES_DIRECTORY"))
    Files.list(notableDirectory).filter { it.extension == "md" }
        .forEach {
            val note = parseFile(it.toFile())
            repository.save(note)
        }
}

private fun parseFile(file: File): Note {
    var headerCount = 0 // 3 means that beginning of the body reached
    var title: String? = null
    var text: String = ""
    var createdAt: OffsetDateTime? = null
    var updatedAt: OffsetDateTime? = null
    file.forEachLine {
        when (headerCount) {
            0 -> {
                if (it != "---") {
                    throw RuntimeException()
                }
                headerCount++
            }
            1 -> {
                if (it == "---") {
                    headerCount++
                    return@forEachLine
                }
                val (prop, value) = it.split(": ")
                when (prop) {
                    "title" -> title = value
                    "created" -> createdAt = OffsetDateTime.parse(value.trim('\''))
                    "modified" -> updatedAt = OffsetDateTime.parse(value.trim('\''))
                    else -> {
                    }
                }
            }
            2 -> {
                if (it.isNotEmpty()) {
                    throw RuntimeException()
                }
                headerCount++
            }
            3 -> text += it + "\n"
        }
    }
    if (title == null || createdAt == null || updatedAt == null) {
        throw IllegalStateException()
    }
    return Note(null, title!!, text, createdAt!!, updatedAt!!)
}
