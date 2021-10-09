package com.kaiichiro.bestdoc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

internal class CachedNoteRepositoryImplTest {
    private val noteRepositoryMock: NoteRepository = mock()

    private fun newNote(n: Int) = Note(n.toString(), n.toString(), n.toString(), listOf(n.toString()), OffsetDateTime.MIN.plusMinutes(n.toLong()), OffsetDateTime.MIN.plusMinutes(n.toLong()))

    @Test
    fun allNotes() {
        whenever(noteRepositoryMock.findAll()).thenReturn(listOf(newNote(1), newNote(2)))
        whenever(noteRepositoryMock.findUpdatedLaterThan(OffsetDateTime.MIN.plusMinutes(2L))).thenReturn(listOf(newNote(3)))
        val sut = CachedNoteRepositoryImpl(noteRepositoryMock)

        val actual = sut.findAll()

        verify(noteRepositoryMock, times(1)).findAll()
        verify(noteRepositoryMock, times(0)).findUpdatedLaterThan(any())
        assertThat(actual).hasSize(2)

        val actual2 = sut.findAll()
        verify(noteRepositoryMock, times(1)).findAll()
        verify(noteRepositoryMock, times(1)).findUpdatedLaterThan(OffsetDateTime.MIN.plusMinutes(2L))
        assertThat(actual2).hasSize(3)
    }

    @Test
    fun save() {
        whenever(noteRepositoryMock.findAll()).thenReturn(listOf(newNote(1), newNote(2)))
        whenever(noteRepositoryMock.save(any())).thenAnswer {
            it.getArgument<Note>(0)
        }
        val sut = CachedNoteRepositoryImpl(noteRepositoryMock)

        sut.save(newNote(3))
        verify(noteRepositoryMock, times(1)).save(any())

        val actual = sut.findAll()
        assertThat(actual).hasSize(3)
    }

    @Test
    fun deleteNoteThatDoesNotExistInCache() {
        val sut = CachedNoteRepositoryImpl(noteRepositoryMock)

        sut.delete("1")

        verify(noteRepositoryMock, times(1)).delete("1")
    }

    @Test
    fun delete() {
        whenever(noteRepositoryMock.findAll()).thenReturn(listOf(newNote(1), newNote(2)))
        whenever(noteRepositoryMock.save(any())).thenAnswer {
            it.getArgument<Note>(0)
        }
        val sut = CachedNoteRepositoryImpl(noteRepositoryMock)
        sut.findAll() // fill cache

        sut.delete("1")
        verify(noteRepositoryMock, times(1)).delete("1")

        val actual = sut.findAll()
        assertThat(actual).hasSize(1)
        assertThat(actual.toList()[0].id).isEqualTo("2")
    }
}
