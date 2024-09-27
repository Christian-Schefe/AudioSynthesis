package app

import java.io.File

fun scanSongs(folder: String): List<String> {
    val songs = mutableListOf<String>()
    val dir = File(folder)
    for (file in dir.listFiles()!!) {
        if (file.extension == "json") {
            songs.add(file.path)
        }
    }
    return songs
}

fun askInput(songs: List<String>): String {
    println("Choose a song:")
    for ((i, song) in songs.withIndex()) {
        println("$i: $song")
    }
    val index = readLine()!!.toInt()
    return songs[index]
}

fun askShouldRender(): Boolean {
    println("Render the song? (y/n)")
    val input = readLine()!!
    return input == "y"
}