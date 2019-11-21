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
    "level6-1.in",
    "level6-2.in",
    "level6-3.in",
    "level6-4.in",
    "level6-5.in"
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
    val colors by lazy {
        points.map { it.color }.distinct().sorted()
    }

    val pairedPoints by lazy {
        val groupedByColor = points.groupBy { it.color }
        val result = mutableMapOf<Point, Point>()
        groupedByColor.forEach { (_, b) ->
            result[b[0]] = b[1]
            result[b[1]] = b[0]
        }
        result.toMap()
    }

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
    var startingPoint: Point,
    var dirs: MutableList<Dir>,
    var targetPoint: Point,
    var visited: MutableList<Coords> = mutableListOf(),
    var current: Coords,
    var complete: Boolean = false,
    var removed: Boolean = false
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
        image.setRGB(it.coords.col - 1, it.coords.row - 1, colors[it.color]!!.rgb)
    }
    solution.paths.forEach {
        var current = it.startingPoint.coords
        val color = colors[it.startingPoint.color]!!
        for (d in it.dirs) {
            current = current.move(d)
            image.setRGB(current.col - 1, current.row - 1, color.rgb)
        }
    }
    return image
}


fun solve(test: Test, visualize: Boolean = false): TestSolution {
    val colors = test.colors
    val pairedPoints = test.pairedPoints
    val board = test.board
    val points = test.points

    val blockedByPoints = test.points.map { it.position }.toMutableSet()
    val blockedByPaths = mutableSetOf<Int>()

    val openPaths = test.points.map {
        PartialPath(it, mutableListOf(), pairedPoints[it]!!, mutableListOf(it.coords), it.coords, false)
    }

    val pathsByStartingPoint = openPaths.associateBy { it.startingPoint }
    val pairedPaths = pairedPoints.entries.associate {
        it.key to pathsByStartingPoint[it.value]!!
    }

    var change = true
    while (change) {
        change = false
        for (p in openPaths) {
            if (p.complete || p.removed) {
                continue
            }
            val pairedPath = pairedPaths[p.startingPoint]!!
            val neighbors = p.current.neighbors(board).filter { n ->
                val nPos = n.position(board)
                (!blockedByPoints.contains(nPos) && !blockedByPaths.contains(nPos))
                        || p.targetPoint.position == nPos
                        || pairedPath.current.position(board) == nPos
            }
            if (neighbors.size != 1) {
                continue
            }
            val n = neighbors[0]
            val nPos = n.position(board)

            if (p.targetPoint.position == nPos) {
                // reached target point
                pairedPath.removed = true
                blockedByPaths.removeAll(pairedPath.visited.map { it.position(board) })
                p.complete = true

                p.dirs.add(getDir(p.current, n, board))
                p.visited.add(n)
                p.current = n
                blockedByPaths.add(nPos)

                if (p.startingPoint.position > pairedPath.startingPoint.position) {
                    // gotta inverse the path
                    p.startingPoint = pairedPath.startingPoint
                    p.targetPoint = pairedPath.targetPoint
                    p.dirs = p.dirs.reversed().map { it.reverse() }.toMutableList()
                }

            } else if (pairedPath.current.position(board) == nPos) {
                // merge with other path
                p.dirs.add(getDir(p.current, n, board))
                var a = p
                var b = pairedPath
                if (a.startingPoint.position > b.startingPoint.position) {
                    val c = a
                    a = b
                    b = c
                }
                a.complete = true
                b.removed = true
                a.current = a.targetPoint.coords
                a.dirs.addAll(b.dirs.reversed().map { it.reverse() })
                a.visited.addAll(b.visited.reversed())
            } else {
                // found sure move
                p.dirs.add(getDir(p.current, n, board))
                p.visited.add(n)
                p.current = n
                blockedByPaths.add(nPos)
            }

            change = true
        }
    }

    val paths = openPaths.filter { !it.removed && it.dirs.isNotEmpty() }
    val sortedGrouped = paths.groupBy { it.startingPoint.color }
        .mapValues {
            it.value.sortedBy { it.startingPoint.position }
        }
    return TestSolution(sortedGrouped.keys.sorted().flatMap { sortedGrouped[it]!! })
}

fun main() {
    files.forEach {file->
        val input = readInput("$folder/$file")

        val testSolutions = input.tests.map { solve(it) }

        input.tests.zip(testSolutions).forEachIndexed { ind, p ->
            ImageIO.write(drawTest(p.first, TestSolution(listOf())), "PNG", File("${file}_test_${ind}_start.png"))
            ImageIO.write(drawTest(p.first, p.second), "PNG", File("${file}_test_${ind}_end.png"))
        }

        val result = testSolutions.flatMap { it.encode() }
            .joinToString(separator = " ") { it }
        println(testSolutions.size.toString() + " " + result)
    }
}

fun getDir(a: Coords, b: Coords, board: Board) = Dir.values().first { a.move(it) == b }
