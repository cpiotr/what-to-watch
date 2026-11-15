package pl.ciruk.whattowatch.neural

import pl.ciruk.whattowatch.core.suggest.ScoredTitle
import pl.ciruk.whattowatch.core.title.Title
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE

/**
 * The films and series I rated, kept in one CSV file the app writes, and how I'd rate the ones I haven't, learned
 * from them again after every change. If the file doesn't exist yet, it starts as a copy of the ratings bundled with
 * the app. A film I watched without rating it isn't known.
 */
class MyRatings private constructor(
        private val file: Path,
        private val learn: (List<RatedTitle>) -> PersonalRatings,
        ratedTitles: List<RatedTitle>) {

    // Requests read the ratings and their model on several threads, so a change replaces both at once
    @Volatile
    private var current = Learned(ratedTitles, learn(ratedTitles))
    private val changing = Any()

    fun all(): List<RatedTitle> = current.ratedTitles

    fun ratingOf(title: Title): Int? = current.ratedTitles.firstOrNull { it.isOf(title) }?.rating

    /**
     * Returns null for a film with neither a Filmweb nor an IMDb score.
     */
    fun predict(film: ScoredTitle): Double? = current.model.predict(film)

    /**
     * Saves my rating of a title, in place of any earlier rating of it, so a title is never rated twice.
     */
    fun rate(ratedTitle: RatedTitle) = synchronized(changing) {
        val changed = current.ratedTitles.toMutableList()
        // A changed rating keeps its row, so the file's history shows one line changed
        val earlier = changed.indexOfFirst { it.isOf(ratedTitle.title) }
        changed.removeAll { it.isOf(ratedTitle.title) }
        changed.add(if (earlier == -1) changed.size else earlier, ratedTitle)
        change(changed)
    }

    /**
     * Returns whether there was a rating to remove.
     */
    fun remove(title: Title): Boolean = synchronized(changing) {
        val changed = current.ratedTitles.toMutableList()
        if (!changed.removeAll { it.isOf(title) }) {
            return false
        }
        change(changed)
        return true
    }

    fun toCsv(): String = RatedTitle.toCsv(current.ratedTitles)

    // The file changes first, so a rating that couldn't be saved is never shown
    private fun change(ratedTitles: List<RatedTitle>) {
        save(file, ratedTitles)
        current = Learned(ratedTitles, learn(ratedTitles))
    }

    private class Learned(val ratedTitles: List<RatedTitle>, val model: PersonalRatings)

    companion object {
        /**
         * Reads my ratings from [file], or from the bundled ones if it doesn't exist yet, and has [learn] teach the
         * model from them.
         */
        @JvmStatic
        fun open(file: Path, learn: (List<RatedTitle>) -> PersonalRatings): MyRatings {
            if (Files.exists(file)) {
                return MyRatings(file, learn, RatedTitle.parseAll(Files.readString(file)))
            }
            val bundled = RatedTitle.readBundled()
            file.toAbsolutePath().parent?.let(Files::createDirectories)
            save(file, bundled)
            return MyRatings(file, learn, bundled)
        }

        // A crash while writing leaves the previous file whole, and the new one is on disk before it takes its place
        private fun save(file: Path, ratedTitles: List<RatedTitle>) {
            val written = file.resolveSibling("${file.fileName}.tmp")
            FileChannel.open(written, CREATE, TRUNCATE_EXISTING, WRITE).use { channel ->
                val csv = Charsets.UTF_8.encode(RatedTitle.toCsv(ratedTitles))
                while (csv.hasRemaining()) {
                    channel.write(csv)
                }
                channel.force(true)
            }
            Files.move(written, file, ATOMIC_MOVE, REPLACE_EXISTING)
        }
    }
}
