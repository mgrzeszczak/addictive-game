package com.mgrzeszczak.addictivegame.level3

import com.mgrzeszczak.addictivegame.*
import java.util.*

val folder = "level3"
val files = listOf(
    "level3-0.in",
    "level3-1.in",
    "level3-01.in",
    "level3-2.in",
    "level3-02.in",
    "level3-3.in",
    "level3-03.in",
    "level3-4.in",
    "level3-04.in",
    "level3-5.in",
    "level3-6.in",
    "level3-7.in"
)


data class Input(
    val board: Board,
    val points: List<Point>,
    val paths: List<Path>
) {
    companion object {
        fun read(scanner: Scanner): Input {
            val board = Board.read(scanner)
            return Input(
                board,
                Points.read(scanner, board).points,
                Paths.read(scanner).paths
            )
        }
    }
}


fun readInput(file: String): Input {
    val scanner = prepareScanner(file)
    return Input.read(scanner)
}



fun main() {
    files.forEach {
        val input = readInput("$folder/$it")
        val result = validatePath(input.paths[0], input.board, input.points)
        val path = input.paths[0].steps.joinToString(separator = " ") { it.name }
        println("$it ${result.code} ${result.index}")
    }
}

