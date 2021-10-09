package com.kaiichiro.bestdoc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

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
    fun findUpdatedLaterThan(dt: OffsetDateTime): Iterable<Note>
}

interface CachedNoteRepository {
    fun findAll(): Iterable<Note>
    fun findById(id: NoteId): Note
    fun save(note: Note): Note
    fun delete(id: NoteId)
}

// FIXME refine concurrency
@Component
class CachedNoteRepositoryImpl(private val noteRepository: NoteRepository) : CachedNoteRepository {
    private val cache: ConcurrentMap<NoteId, Note> = ConcurrentHashMap()

    private fun lastCachedUpdatedAt(): OffsetDateTime? {
        return cache.maxOfOrNull { it.value.updatedAt }
    }

    private fun updateCache() {
        val last = lastCachedUpdatedAt()
        if (last == null) {
            val allNotes = noteRepository.findAll()
            cache.putAll(allNotes.associateBy { it.id })
        } else {
            val newNotes = noteRepository.findUpdatedLaterThan(last)
            cache.putAll(newNotes.associateBy { it.id })
        }
    }

    override fun findAll(): Iterable<Note> {
        updateCache()
        return cache.values
    }

    override fun findById(id: NoteId): Note {
        updateCache()
        return cache.get(id)!!
    }

    override fun save(note: Note): Note {
        updateCache()
        val saved = noteRepository.save(note)
        cache.put(saved.id, saved)
        return saved
    }

    override fun delete(id: NoteId) {
        noteRepository.delete(id)
        cache.remove(id)
    }
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
