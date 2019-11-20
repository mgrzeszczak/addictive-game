package com.mgrzeszczak.addictivegame.level1

import com.mgrzeszczak.addictivegame.Board
import com.mgrzeszczak.addictivegame.prepareScanner
import java.util.*


data class Input(
    val board: Board,
    val points: Collection<Point>
)

data class Points(val points: Collection<Point>) {
    companion object {
        fun read(scanner: Scanner): Points {
            val count = scanner.nextInt()
            return Points((0 until count).map { Point.read(scanner) })
        }
    }
}

data class Point(val position: Int) {
    companion object {
        fun read(scanner: Scanner): Point {
            return Point(scanner.nextInt())
        }
    }
}

fun readInput(file: String): Input {
    val scanner = prepareScanner(file)
    return Input(Board.read(scanner), Points.read(scanner).points)
}

fun main() {
    val files = (0 until 4).toList()
        .map { "level1/level1-$it.in" }
    files.forEach {
        val input = readInput(it)
        println(input.points.map {
            val coords = input.board.coords(it.position)
            "${coords.row} ${coords.col}"
        }.joinToString(separator = " "))
    }
}
