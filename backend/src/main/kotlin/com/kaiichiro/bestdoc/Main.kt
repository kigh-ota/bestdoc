package com.kaiichiro.bestdoc

import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.WriteResult
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.CrudRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.servlet.http.HttpServletRequest


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

    val path = Path.of(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
    Files.deleteIfExists(path)
    Files.createFile(path);
    Files.writeString(path, System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON"), StandardOpenOption.WRITE)
    val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId(
        ServiceOptions.getDefaultProjectId()
    )
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()
    firestoreOptions.service.use { db ->
        val docRef = db.collection("users").document("alovelace")
// Add document data  with id "alovelace" using a hashmap
        val data: MutableMap<String, Any> = HashMap()
        data["first"] = "Ada"
        data["last"] = "Lovelace"
        data["born"] = 1815
//asynchronously write data
        val result: ApiFuture<WriteResult> = docRef.set(data)
// ...
// result.get() blocks on response
        System.out.println("Update time : " + result.get().getUpdateTime())
    }
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

@EnableWebSecurity
@Configuration
class WebSecurityConfig : WebSecurityConfigurerAdapter() {
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
