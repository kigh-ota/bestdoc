package com.kaiichiro.bestdoc

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
class NoteController(private val noteRepository: NoteRepository) {

    @QueryMapping
    fun allNotes(): Iterable<GraphqlNote> {
        return noteRepository.findAll().map(GraphqlNote::from)
    }

    @QueryMapping
    fun getNote(@Argument id: NoteId): GraphqlNote {
        return GraphqlNote.from(noteRepository.findById(id))
    }

    @MutationMapping
    fun addNote(@Argument title: String, @Argument text: String): Note {
        return noteRepository.save(Note.new(null, title, text))
    }

    @MutationMapping
    fun updateNote(@Argument id: NoteId, @Argument title: String, @Argument text: String): Note {
        return noteRepository.save(Note.new(id, title, text))
    }
}

data class GraphqlNote(
    val id: NoteId,
    val title: String,
    val text: String,
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
            note.id!!, note.title, note.text,
            note.createdAt.format(DATE_TIME_FORMATTER),
            note.updatedAt.format(DATE_TIME_FORMATTER)
        )
    }
}
