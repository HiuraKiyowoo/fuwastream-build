package com.fuwanime

import com.fuwanime.data.Anime
import com.fuwanime.data.genreScore
import com.fuwanime.data.pickForYou
import com.fuwanime.data.tasteWeights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForYouTest {
    private fun anime(id: String, genres: List<String> = emptyList(), views: Long = 0) =
        Anime(id = id, title = "T$id", genres = genres, views = views)

    private val general = listOf(
        anime("g1", listOf("Action"), 500),
        anime("g2", listOf("Comedy"), 400),
        anime("g3", listOf("Drama"), 300),
        anime("g4", listOf("Action"), 200),
    )
    private val personal = listOf(
        anime("p1", listOf("Action", "Ninja"), 10),
        anime("p2", listOf("Action"), 20),
        anime("p3", listOf("Comedy"), 30),
    )

    @Test fun newUser_getsGeneralPicksNotEmpty() {
        val out = pickForYou(personal, general, taste = emptyMap(), exclude = emptySet(), limit = 4)
        assertEquals(4, out.size)
        assertEquals(listOf("g1", "g2", "g3", "g4"), out.map { it.id })
    }

    @Test fun taste_personalFirstButGeneralKeptForExploration() {
        val taste = tasteWeights(listOf(listOf("Action", "Ninja")), emptyList())
        val out = pickForYou(personal, general, taste, exclude = emptySet(), limit = 6)
        // personal quota = limit - 3 = 3, so the rest must come from the general pool
        assertEquals(6, out.size)
        // p3 is Comedy only, so it carries no signal for an Action/Ninja taste and must not be passed off
        // as personal: 2 personal picks, then general ones fill the row.
        assertEquals(listOf("p1", "p2"), out.take(2).map { it.id })
        assertTrue(out.drop(2).all { it.id.startsWith("g") })
        assertEquals("p1", out.first().id) // strongest genre match first
    }

    @Test fun noDuplicateTitles() {
        val overlapping = personal + general + listOf(anime("p1"), anime("g1"))
        val out = pickForYou(overlapping, overlapping, tasteWeights(listOf(listOf("Action")), emptyList()), emptySet(), 10)
        assertEquals(out.size, out.map { it.id }.distinct().size)
    }

    @Test fun savedAndWatchedStayOutOfPersonalQuota() {
        val taste = tasteWeights(listOf(listOf("Action")), emptyList())
        val out = pickForYou(personal, general, taste, exclude = setOf("p1", "p2"), limit = 6)
        assertFalse(out.any { it.id == "p1" || it.id == "p2" })
    }

    @Test fun neverEmpty_evenWhenEverythingIsExcluded() {
        val taste = tasteWeights(listOf(listOf("Action")), emptyList())
        val out = pickForYou(personal, general, taste, exclude = setOf("p1", "p2", "p3", "g1", "g2", "g3", "g4"), limit = 5)
        assertTrue(out.isNotEmpty())
        assertEquals(5, out.size)
    }

    @Test fun emptyPools_yieldEmptyRowWithoutCrashing() {
        assertTrue(pickForYou(emptyList(), emptyList(), emptyMap(), emptySet()).isEmpty())
    }

    @Test fun favouritesWeighDoubleWatched() {
        val taste = tasteWeights(listOf(listOf("Action")), listOf(listOf("Action", "Comedy")))
        assertEquals(3, taste["action"])
        assertEquals(1, taste["comedy"])
    }

    @Test fun genreScore_ignoresUnrelatedGenres() {
        val taste = tasteWeights(listOf(listOf("Action")), emptyList())
        assertEquals(2, genreScore(anime("x", listOf("Action")), taste))
        assertEquals(0, genreScore(anime("y", listOf("Romance")), taste))
    }

    @Test fun blankIdsAreDropped() {
        val out = pickForYou(listOf(anime("", listOf("Action"))), general, emptyMap(), emptySet(), 3)
        assertTrue(out.none { it.id.isBlank() })
    }
}
