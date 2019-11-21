package com.mgrzeszczak.addictivegame.level4

import com.mgrzeszczak.addictivegame.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

val folder = "level4"
val files = listOf(
    "level4-0.in",
    "level4-1.in",
    "level4-2.in",
    "level4-3.in",
    "level4-4.in",
    "level4-5.in"
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

fun drawCanvas(canvas: Set<Int>, board: Board): BufferedImage {
    val image = BufferedImage(board.cols, board.rows, BufferedImage.TYPE_INT_RGB)
    (0 until board.rows).forEach { r ->
        (0 until board.cols).forEach { c ->
            val p = Coords(r + 1, c + 1).position(board)
            val color = if (canvas.contains(p)) Color.BLACK else Color.WHITE
            image.setRGB(c, r, color.rgb)
        }
    }
    return image
}

fun main() {
    files.forEach {
        val input = readInput("$folder/$it")
        val board = input.board
        val canvas = mutableSetOf<Int>()

        val pointsByPosition = input.points.associateBy { it.position }
        val colors = input.points.map { it.color }
            .distinct()
            .sorted()

        val pointsByColor = input.points.groupBy { it.color }

        val opt = OptimizedValidatePathInput(
            pointsByPosition,
            pointsByColor
        )

        var counter = 0
        input.paths.forEach { path ->
            counter += 1
            if (counter % 100 == 0) {
                println("$counter / ${input.paths.size}")
            }
            val result = validatePathV2(path, input.board, input.points, opt, canvas)
            if (result.valid) {
                var current = board.coords(path.startingPosition)
                canvas.add(current.position(board))
                path.steps.forEach {
                    current = current.move(it)
                    canvas.add(current.position(board))
                }
            }
        }
        input.points.forEach { canvas.add(it.position) }
        val image = drawCanvas(canvas, board)
        ImageIO.write(image, "PNG", File("$it.result.png"))
    }
}
