package com.example.projectandroid.data

interface TrackRepository {
    fun getTracks(): List<Track>
    fun getTrack(id: Long): Track?
}

/** Hand-authored fixtures, not an API response. IDs and prices are illustrative. */
object MockTrackRepository : TrackRepository {
    private val tracks = album(
        "Random Access Memories", "2013-05-17T07:00:00Z", 1_000L,
        listOf(
            "Give Life Back to Music" to 274,
            "The Game of Love" to 322,
            "Giorgio by Moroder" to 544,
            "Within" to 228,
            "Instant Crush" to 337,
            "Lose Yourself to Dance" to 353,
            "Touch" to 498,
            "Get Lucky" to 369,
            "Beyond" to 290,
            "Motherboard" to 341,
            "Fragments of Time" to 279,
            "Doin’ It Right" to 251,
            "Contact" to 383,
        ),
    ) + album(
        "Discovery", "2001-03-12T08:00:00Z", 2_000L,
        listOf(
            "One More Time" to 320,
            "Aerodynamic" to 212,
            "Digital Love" to 301,
            "Harder, Better, Faster, Stronger" to 224,
            "Crescendolls" to 211,
            "Nightvision" to 104,
            "Superheroes" to 237,
            "High Life" to 201,
            "Something About Us" to 232,
            "Voyager" to 227,
            "Veridis Quo" to 345,
            "Short Circuit" to 206,
            "Face to Face" to 240,
            "Too Long" to 600,
        ),
    )

    override fun getTracks(): List<Track> = tracks
    override fun getTrack(id: Long): Track? = tracks.find { it.trackId == id }

    private fun album(name: String, date: String, firstId: Long, songs: List<Pair<String, Int>>) =
        songs.mapIndexed { index, (title, seconds) ->
            Track(
                trackId = firstId + index,
                trackName = title,
                artistName = "Daft Punk",
                collectionName = name,
                trackNumber = index + 1,
                trackCount = songs.size,
                trackTimeMillis = seconds * 1_000L,
                primaryGenreName = "Electronic",
                releaseDate = date,
                trackPrice = 1.29,
                currency = "USD",
                country = "USA",
                trackExplicitness = "notExplicit",
            )
        }
}

