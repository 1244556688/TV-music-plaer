package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.R

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumArtRes: Int?, // Null indicates fallback default vinyl placeholder
    val genre: String,
    val fileUri: String? = null // Store Uri for local files
)

val SampleSongs = listOf(
    Song(
        id = "1",
        title = "Cosmic Serenade",
        artist = "Nebular Drift",
        durationMs = 215000, // 3:35
        albumArtRes = R.drawable.img_album_cyber_1782904917279,
        genre = "Ambient Sci-Fi"
    ),
    Song(
        id = "2",
        title = "Neon Horizon",
        artist = "Retro Glide",
        durationMs = 184000, // 3:04
        albumArtRes = R.drawable.img_album_neon_1782904897603,
        genre = "Synthwave"
    ),
    Song(
        id = "3",
        title = "Midnight Coffee",
        artist = "Rain Cafe",
        durationMs = 158000, // 2:38
        albumArtRes = R.drawable.img_album_lofi_1782904906868,
        genre = "Lofi Chill"
    ),
    Song(
        id = "4",
        title = "Starlight Voyage",
        artist = "Cyberwave Orchestra",
        durationMs = 242000, // 4:02
        albumArtRes = R.drawable.img_vinyl_placeholder_1782904928756,
        genre = "Electro Synth"
    ),
    Song(
        id = "5",
        title = "Echoes of Orion",
        artist = "Astral Chill Out",
        durationMs = 195000, // 3:15
        albumArtRes = null, // Falls back to default vinyl placeholder icon
        genre = "Space Ambient"
    )
)

