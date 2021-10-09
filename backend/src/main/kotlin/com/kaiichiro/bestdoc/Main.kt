package com.kaiichiro.bestdoc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootApplication
class BestDocApplication

fun main(args: Array<String>) {
    runApplication<BestDocApplication>(*args)
}

data class Note(
    val id: NoteId?,
    val title: String,
    val text: String,
    val tags: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun new(id: NoteId?, title: String, text: String, tags: List<String>): Note {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            return Note(id, title, text, tags, now, now)
        }
    }
}

typealias NoteId = String

interface NoteRepository {
    fun findAll(): Iterable<Note>
    fun findById(id: NoteId): Note
    fun save(note: Note): Note
    fun delete(id: NoteId)
}

@EnableWebSecurity
@Configuration
@Profile("production")
class ProductionWebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
        http.requiresChannel()
            .requestMatchers({ r ->
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
