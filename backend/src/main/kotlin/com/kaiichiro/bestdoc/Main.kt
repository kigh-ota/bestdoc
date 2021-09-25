package com.kaiichiro.bestdoc

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField.*
import java.util.*
import javax.servlet.http.HttpServletRequest


@SpringBootApplication
class BestDocApplication {

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

fun main(args: Array<String>) {
    runApplication<BestDocApplication>(*args)
}

class Note(
    val id: String?,
    val title: String,
    val text: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun new(title: String, text: String): Note {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            return Note(null, title, text, now, now)
        }
    }
}

interface NoteRepository {
    fun findAll(): List<Note>
    fun findById(id: Long): Optional<Note>
    fun save(note: Note): String
}

@Component
class FirestoreNoteRepository(private val db: Firestore) : NoteRepository {
    companion object {
        private const val COLLECTION = "notes"
        private val FORMATTER = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendOffsetId()
            .toFormatter()
    }

    override fun findAll(): List<Note> {
        return listOf() // TODO
    }

    override fun findById(id: Long): Optional<Note> {
        return Optional.empty() // TODO
    }

    override fun save(note: Note): String {
        val result = db.collection(COLLECTION).add(object {
            val title = note.title
            val text = note.text
            val createdAt = note.createdAt.format(FORMATTER)
            val updatedAt = note.updatedAt.format(FORMATTER)
        })
        val id = result.get().id
        System.out.println("id : " + id)
        return id
    }
}

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

    @MutationMapping
    fun addNote(@Argument title: String, @Argument text: String): String {
        return noteRepository.save(Note.new(title, text))
    }
}

@EnableWebSecurity
@Configuration
@Profile("production")
class ProductionWebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
        http.requiresChannel()
            .requestMatchers(RequestMatcher { r: HttpServletRequest ->
                r.getHeader(
                    "X-Forwarded-Proto"
                ) != null
            })
            .requiresSecure()
        http.authorizeRequests().anyRequest().authenticated().and().formLogin()
    }
}

@EnableWebSecurity
@Configuration
@Profile("dev")
class DevWebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
        http.authorizeRequests().anyRequest().permitAll()
    }
}
