package com.fuwanime.data

/**
 * Taste profile for Beranda's "Rekomendasi": genre weights learned from the collection and watch history.
 * Favourites weigh double a watched title, because saving is a stronger signal than pressing play.
 * Keys are trimmed/lowercased so "Action" and "action " are the same genre.
 */
fun tasteWeights(favouriteGenres: List<List<String>>, watchedGenres: List<List<String>>): Map<String, Int> {
    val weights = HashMap<String, Int>()
    favouriteGenres.forEach { genres ->
        genres.forEach { g ->
            val key = g.trim().lowercase()
            if (key.isNotEmpty()) weights[key] = (weights[key] ?: 0) + 2
        }
    }
    watchedGenres.forEach { genres ->
        genres.forEach { g ->
            val key = g.trim().lowercase()
            if (key.isNotEmpty()) weights[key] = (weights[key] ?: 0) + 1
        }
    }
    return weights
}

/** How well a title matches the learned taste; 0 means "no signal", which keeps it out of the personal quota. */
fun genreScore(anime: Anime, taste: Map<String, Int>): Int =
    anime.genres.sumOf { taste[it.trim().lowercase()] ?: 0 }

/**
 * Builds the "Rekomendasi" row. Rules:
 * - no taste yet (new user) -> general picks by popularity, so the row is never empty;
 * - taste present -> personal picks first (up to [limit] - 3), general fills the rest for exploration;
 * - one entry per title id, and titles already saved/watched are kept out of the personal quota;
 * - if exclusions would empty the row, general titles are allowed back rather than showing nothing.
 *
 * [personal] is the genre-similar pool, [general] the popular/trending/season pool. UI is unchanged.
 */
fun pickForYou(
    personal: List<Anime>,
    general: List<Anime>,
    taste: Map<String, Int>,
    exclude: Set<String>,
    limit: Int = 10,
): List<Anime> {
    val out = ArrayList<Anime>(limit)
    val used = HashSet<String>()
    fun add(anime: Anime) {
        if (anime.id.isBlank() || !used.add(anime.id)) return
        out += anime
    }

    val rankedPersonal = personal.asSequence()
        .filter { it.id.isNotBlank() && it.id !in exclude }
        .distinctBy { it.id }
        .filter { taste.isEmpty() || genreScore(it, taste) > 0 }
        .sortedWith(compareByDescending<Anime> { genreScore(it, taste) }.thenByDescending { it.views })
        .toList()
    // Unfiltered copy for the last resort: exclusions must not be able to empty the row.
    val personalFallback = personal.asSequence()
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .sortedWith(compareByDescending<Anime> { genreScore(it, taste) }.thenByDescending { it.views })
        .toList()
    val rankedGeneral = general.asSequence()
        .filter { it.id.isNotBlank() }
        .distinctBy { it.id }
        .sortedByDescending { it.views }
        .toList()

    val personalQuota = if (taste.isEmpty()) 0 else (limit - 3).coerceAtLeast(1)
    for (anime in rankedPersonal) {
        if (out.size >= personalQuota || out.size >= limit) break
        add(anime)
    }
    for (anime in rankedGeneral) {
        if (out.size >= limit) break
        if (anime.id !in exclude) add(anime)
    }
    // Last resort: an empty row is worse than a repeat, so allow excluded/unscored titles back in.
    for (anime in rankedGeneral) {
        if (out.size >= limit) break
        add(anime)
    }
    for (anime in personalFallback) {
        if (out.size >= limit) break
        add(anime)
    }
    return out
}
