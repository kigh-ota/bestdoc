package com.kaiichiro.bestdoc

import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Controller
@SchemaMapping(typeName = "Note")
class NoteController(private val noteRepository: CachedNoteRepository) {
    companion object {
        private val log = LoggerFactory.getLogger(NoteController::class.java)
    }

    @QueryMapping
    fun allNotes(@Argument keyword: String?): Iterable<GraphqlNote> {
        val start = System.currentTimeMillis()
        val allNotes = noteRepository.findAll()
        return when (keyword) {
            null -> allNotes.map(GraphqlNote::from)
            else -> allNotes.filter { it.title.contains(keyword) || it.text.contains(keyword) }
                .map(GraphqlNote::from)
        }.also {
            val end = System.currentTimeMillis()
            log.info("allNotes() took ${end - start} milliseconds (${it.size} notes)")
        }
    }

    @QueryMapping
    fun getNote(@Argument id: NoteId): GraphqlNote {
        return GraphqlNote.from(noteRepository.findById(id))
    }

    @MutationMapping
    fun addNote(@Argument title: String, @Argument text: String): Note {
        return noteRepository.save(Note.new(null, title, text, listOf()))
    }

    @MutationMapping
    fun updateNote(
        @Argument id: NoteId,
        @Argument title: String,
        @Argument text: String
    ): Note {
        return noteRepository.save(Note.new(id, title, text, listOf()))
    }

    @MutationMapping
    fun deleteNote(@Argument id: NoteId): NoteId {
        noteRepository.delete(id)
        return id
    }
}

data class GraphqlNote(
    val id: NoteId,
    val title: String,
    val text: String,
    val tags: List<String>,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
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

        fun from(note: Note) = GraphqlNote(
            note.id!!, note.title, note.text, note.tags,
            note.createdAt.format(DATE_TIME_FORMATTER),
            note.updatedAt.format(DATE_TIME_FORMATTER)
        )
    }
}
