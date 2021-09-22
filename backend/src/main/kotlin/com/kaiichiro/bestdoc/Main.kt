package com.kaiichiro.bestdoc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.CrudRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaRepositories("com.kaiichiro.bestdoc")
class BestDocApplication(private val jdbcTemplate: JdbcTemplate)

fun main(args: Array<String>) {
    val context = runApplication<BestDocApplication>(*args)
    val noteRepository = context.beanFactory.getBean(NoteRepository::class.java)
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    noteRepository.save(Note(null, "Title1", "Text1", now, now))
    noteRepository.save(Note(null, "Title2", "Text2", now, now))
    noteRepository.save(Note(null, "Empty", "", now, now))
}

@Entity
class Note(
    @Id @GeneratedValue(strategy = GenerationType.AUTO) val id: Long?,
    val title: String,
    val text: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

interface NoteRepository : CrudRepository<Note, Long>

@Controller
@SchemaMapping(typeName = "Note")
class NoteController(private val noteRepository: NoteRepository) {

    @QueryMapping
    fun allNotes(): List<Note> {
        return noteRepository.findAll().toList()
    }

    @QueryMapping
    fun getNote(@Argument id: Long): Note {
        return noteRepository.findById(id).orElseThrow { RuntimeException() }
    }
}
