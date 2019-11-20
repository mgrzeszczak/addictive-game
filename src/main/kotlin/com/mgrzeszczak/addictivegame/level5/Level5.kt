package com.mgrzeszczak.addictivegame.level5

import com.mgrzeszczak.addictivegame.*
import java.util.*


val folder = "level5"
val files = listOf(
    "level5-0.in",
    "level5-1.in",
    "level5-2.in",
    "level5-3.in",
    "level5-4.in"
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

const val connected = 1
const val connectable = 2
const val notConnectable = 3

data class DistancedCoords(
    val coords: Coords,
    val distance: Int
)

fun reconstructPath(cameFrom: Map<Coords, Coords>, to: Coords): List<Coords> {
    var current = to
    val path = mutableListOf<Coords>(current)
    while (current in cameFrom) {
        current = cameFrom[current]!!
        path.add(current)
    }
    return path.reversed()
}

fun findPath(from: Coords, target: Coords, test: Test, blocked: Set<Int>): List<Coords>? {
    val openSet = PriorityQueue(Comparator<DistancedCoords> { a, b -> a.distance - b.distance })
    openSet.add(DistancedCoords(from, 0))
    val cameFrom = mutableMapOf<Coords, Coords>()
    val gScore = mutableMapOf(from to 0)

    while (!openSet.isEmpty()) {
        val current = openSet.poll().coords
        if (current == target) {
            return reconstructPath(cameFrom, current)
        }

        val neighbors = current.neighbors(test.board).filter {
            !blocked.contains(it.position(test.board))
        }

        for (n in neighbors) {
            val score = gScore.getOrDefault(current, Int.MAX_VALUE) + 1
            if (score < gScore.getOrDefault(n, Int.MAX_VALUE)) {
                cameFrom[n] = current
                gScore[n] = score
                openSet.find { it.coords == n }?.let { openSet.remove(it) }
                openSet.add(DistancedCoords(n, score + n.manhattanDist(target)))
            }
        }
    }
    return null
}

fun findPathForColor(color: Int, test: Test, blocked: MutableSet<Int>): List<Coords>? {
    val points = test.points.filter { it.color == color }
    val p1 = points[0]
    val p2 = points[1]
    blocked.remove(p1.position)
    blocked.remove(p2.position)
    val result = findPath(p1.coords, p2.coords, test, blocked)
    blocked.add(p1.position)
    blocked.add(p2.position)
    return result
}

fun calculateBlocked(test: Test): MutableSet<Int> {
    val blocked = mutableSetOf<Int>()
    test.points.forEach { blocked.add(it.position) }
    test.paths.forEach { path ->
        var current = test.board.coords(path.startingPosition)
        blocked.add(path.startingPosition)
        path.steps.forEach { dir ->
            current = current.move(dir)
            blocked.add(current.position(test.board))
        }
    }
    return blocked
}

fun main() {

    files.forEach {
        val input = readInput("$folder/$it")
        val results = input.tests.flatMap { test ->
            val board = test.board
            val paths = test.paths
            val points = test.points

            val colors = points.map { it.color }.distinct().sorted()
            val validColors = paths.map { it.color }.toSet()

            val blocked = calculateBlocked(test)

            val results = colors.map {
                if (validColors.contains(it)) {
                    connected
                } else {
                    val path = findPathForColor(it, test, blocked)
                    if (path != null) {
                        connectable
                    } else {
                        notConnectable
                    }
                }
            }
            results
        }

        println(results.joinToString(separator = " ") { it.toString() })
    }
}
