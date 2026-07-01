package com.example.data

import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()

    suspend fun insertSong(song: Song) {
        songDao.insertSong(song)
    }

    suspend fun insertSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }

    suspend fun deleteSong(song: Song) {
        songDao.deleteSong(song)
    }

    suspend fun clearPlaylist() {
        songDao.clearPlaylist()
    }
}
