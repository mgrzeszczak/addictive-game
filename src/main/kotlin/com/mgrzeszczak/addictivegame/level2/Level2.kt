package com.mgrzeszczak.addictivegame.level2

import com.mgrzeszczak.addictivegame.*
import java.util.*

data class Input(
    val board: Board,
    val points: Collection<Point>
) {
    companion object {
        fun read(scanner: Scanner): Input {
            val board = Board.read(scanner)
            return Input(
                board,
                Points.read(scanner, board).points
            )
        }
    }
}

fun readInput(file: String): Input {
    val scanner = prepareScanner(file)
    return Input.read(scanner)
}

fun main() {
    val files = (0 until 4).toList()
        .map { "level2/level2-$it.in" }
    files.forEach {
        val input = readInput(it)
        val byColor = input.points.groupBy { it.color }
        val colors = byColor.keys.sorted()
        val result = colors.map {
            val coords = byColor[it]!!.map { Coords.from(it.position, input.board.rows, input.board.cols) }
            coords[0].manhattanDist(coords[1])
        }.joinToString(separator = " ") { it.toString() }
        println(result)
    }
}
