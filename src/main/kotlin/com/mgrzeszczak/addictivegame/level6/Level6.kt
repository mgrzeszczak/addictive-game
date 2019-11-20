package com.mgrzeszczak.addictivegame.level6

import com.mgrzeszczak.addictivegame.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.abs
import java.util.*
import javax.imageio.ImageIO
import kotlin.random.Random


val folder = "level6"
val files = listOf(
    "level6-1.in"
//    "level6-2.in",
//    "level6-3.in",
//    "level6-4.in",
//    "level6-5.in"
)

data class Input(
    val tests: List<Test>
) {
    companion object {
        fun read(scanner: Scanner): Input {
            val count = scanner.nextInt()
            return Input((0 until count).map { Test.read(scanner) })
        }
    }
}

data class Test(
    val board: Board,
    val points: List<Point>,
    val paths: List<Path>
) {
    companion object {
        fun read(scanner: Scanner): Test {
            val board = Board.read(scanner)
            return Test(
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

data class PartialPath(
    val startingPoint: Point,
    val dirs: MutableList<Dir>,
    var current: Coords,
    val targetPoint: Point,
    val visited: MutableList<Coords> = mutableListOf(),
    var done: Boolean = false
) {
    fun encode(): List<String> {
        val result = mutableListOf<String>()
        result.add(startingPoint.color.toString())
        result.add(startingPoint.position.toString())
        result.add(dirs.size.toString())
        result.addAll(dirs.map { it.name })
        return result
    }
}

data class TestSolution(
    val paths: List<PartialPath>
) {
    fun encode(): List<String> {
        val result = mutableListOf<String>()
        result.add(paths.size.toString())
        paths.forEach { result.addAll(it.encode()) }
        return result
    }
}

fun randColor(): Color {
    return Color(
        abs(Random.nextInt().rem(255)),
        abs(Random.nextInt().rem(255)),
        abs(Random.nextInt().rem(255))
    )
}

fun drawTest(test: Test, solution: TestSolution): BufferedImage {
    val board = test.board
    val colors = test.points.map { it.color }.distinct().sorted()
        .associateWith { randColor() }
    val image = BufferedImage(board.cols, board.rows, BufferedImage.TYPE_INT_RGB)
    test.points.forEach {
        image.setRGB(it.coords.col-1, it.coords.row-1, colors[it.color]!!.rgb)
    }
    solution.paths.forEach {
        var current = it.startingPoint.coords
        val color = colors[it.startingPoint.color]!!
        for (d in it.dirs) {
            current = current.move(d)
            image.setRGB(current.col-1, current.row-1, color.rgb)
        }
    }
    return image
}

fun main() {
    files.forEach {
        val input = readInput("$folder/$it")
        val testResults = input.tests.map { test ->
            val points = test.points
            val board = test.board

            val colors = points.map { it.color }.distinct().sorted()

            val blocked = mutableSetOf<Int>()
            points.forEach { blocked.add(it.position) }
            val pointsByColor = points.groupBy { it.color }

            val targetPoints = points.associateWith {
                val sameColoredPoints = pointsByColor[it.color]!!
                if (sameColoredPoints[0].position == it.position) {
                    sameColoredPoints[1]
                } else {
                    sameColoredPoints[0]
                }
            }

            val open = points.map { PartialPath(it, mutableListOf(), it.coords, targetPoints[it]!!) }.toMutableList()

            val partialPathsByColor = open.groupBy { it.startingPoint.color }
                .mapValues {
                    val a = it.value[0]
                    val b = it.value[1]
                    mapOf(a.startingPoint to b, b.startingPoint to a)
                }

            var change = true
            while (change) {
                change = false
                for (p in open.toList()) {
                    if (p !in open) continue
                    val current = p.current
                    val currentPosition = current.position(board)
                    val neighbors = current.neighbors(board).filter {
                        !blocked.contains(it.position(board))
                                || p.targetPoint.position == it.position(board)
                                || partialPathsByColor[p.startingPoint.color]!![p.startingPoint]!!.current.position(board) == it.position(board)
                    }
                    if (neighbors.size == 1) {
                        val n = neighbors[0]
                        if (p.targetPoint.position == n.position(board)) {
                            p.done = true
                            val otherPath = partialPathsByColor[p.startingPoint.color]!![p.startingPoint]!!
                            open.remove(otherPath)
                            blocked.removeAll(otherPath.visited.map { it.position(board) })
                            blocked.add(n.position(board))
                            p.current = n
                            p.visited.add(n)
                            p.dirs.add(getDir(current, n, board))
                        } else if (partialPathsByColor[p.startingPoint.color]!![p.startingPoint]!!.current.position(board) == n.position(board)) {
                            // merge paths
                            var a = p
                            var b = partialPathsByColor[p.startingPoint.color]!![p.startingPoint]!!
                            if (a.startingPoint.position > b.startingPoint.position) {
                                val c = a
                                a = b
                                b = c
                            }
                            open.remove(b)
                            a.dirs.addAll(b.dirs.reversed().map { it.reverse() })
                        } else {
                            blocked.add(n.position(board))
                            p.current = n
                            p.visited.add(n)
                            p.dirs.add(getDir(current, n, board))
                        }
                        change = true
                    }
                }
            }

            val pathsByColor = open.filter { !it.dirs.isEmpty() }
                .groupBy { it.startingPoint.color }
                .entries
                .associateBy({ it.key }, { it.value.sortedBy { it.startingPoint.position } })
            val testResult = mutableListOf<PartialPath>()
            pathsByColor.keys
                .sorted()
                .forEach { testResult.addAll(pathsByColor[it]!!) }
            TestSolution(testResult)
        }

//        input.tests.zip(testResults).forEachIndexed { ind, p ->
//            ImageIO.write(drawTest(p.first, TestSolution(listOf())), "PNG", File("test_${ind}_start.png"))
//            ImageIO.write(drawTest(p.first, p.second), "PNG", File("test_${ind}_end.png"))
//        }




        val result = testResults.flatMap { it.encode() }
            .joinToString(separator = " ") { it }
        println(testResults.size.toString() + " " + result)
    }
}

fun getDir(a: Coords, b: Coords, board: Board) = Dir.values().first { a.move(it) == b }
