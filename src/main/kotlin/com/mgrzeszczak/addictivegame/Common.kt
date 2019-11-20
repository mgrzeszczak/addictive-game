package com.mgrzeszczak.addictivegame

import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

data class Point(
    val position: Int,
    val color: Int,
    val coords: Coords
) {
    companion object {
        fun read(scanner: Scanner, board: Board): Point {
            val position = scanner.nextInt()
            val color = scanner.nextInt()
            return Point(position, color, board.coords(position))
        }
    }
}

data class Coords(
    val row: Int,
    val col: Int
) {
    fun manhattanDist(other: Coords) = abs(row - other.row) + abs(col - other.col)

    fun move(dir: Dir) = dir.move(this)

    fun position(board: Board) = (row - 1) * board.cols + col

    fun valid(board: Board) = row > 0 && col > 0 && row <= board.rows && col <= board.cols

    fun neighbors(board: Board) =
        Dir.values().map { move(it) }
            .filter { it.valid(board) }

    companion object {
        fun from(position: Int, rows: Int, cols: Int): Coords {
            val row = (position - 1) / cols
            return Coords(
                row + 1,
                (position - (row) * cols)
            )
        }
    }

}

enum class Dir {
    N {
        override fun move(c: Coords): Coords = Coords(c.row - 1, c.col)
        override fun reverse() = S
    },
    E {
        override fun move(c: Coords): Coords = Coords(c.row, c.col + 1)
        override fun reverse() = W
    },
    S {
        override fun move(c: Coords): Coords = Coords(c.row + 1, c.col)
        override fun reverse() = N
    },
    W {
        override fun move(c: Coords): Coords = Coords(c.row, c.col - 1)
        override fun reverse() = E
    };

    abstract fun move(c: Coords): Coords

    abstract fun reverse(): Dir

    companion object {
        fun read(scanner: Scanner): Dir = valueOf(scanner.next())
    }
}

data class Paths(
    val paths: List<Path>
) {

    companion object {
        fun read(scanner: Scanner): Paths {
            val count = scanner.nextInt()
            return Paths((0 until count).map { Path.read(scanner) })
        }
    }

}

data class Path(
    val color: Int,
    val startingPosition: Int,
    val length: Int,
    val steps: Collection<Dir>
) {
    companion object {
        fun read(scanner: Scanner): Path {
            val color = scanner.nextInt()
            val startingPoint = scanner.nextInt()
            val length = scanner.nextInt()
            return Path(
                color,
                startingPoint,
                length,
                (0 until length).map { Dir.read(scanner) }
            )
        }
    }
}

data class Points(
    val points: List<Point>
) {

    companion object {
        fun read(scanner: Scanner, board: Board): Points {
            val count = scanner.nextInt()
            val points = (0 until count).map { Point.read(scanner, board) }
            return Points(points)
        }
    }

}

data class Board(
    val rows: Int,
    val cols: Int
) {
    fun coords(position: Int): Coords = Coords.from(position, rows, cols)

    companion object {
        fun read(scanner: Scanner): Board {
            return Board(scanner.nextInt(), scanner.nextInt())
        }
    }
}

fun prepareScanner(file: String): Scanner {
    val inputStream = ClassLoader.getSystemClassLoader()
        .getResource(file)
        .openStream()
    val scanner = Scanner(inputStream)
    scanner.useDelimiter(Pattern.compile("\\s+"))
    return scanner
}


enum class PathInvalidityStatus {
    VALID,
    CROSSES_ITSELF,
    OUT_OF_BOUNDS,
    TOUCHES_POINT_OF_DIFFERENT_COLOR,
    ENDS_IN_WRONG_PLACE,
    CROSSES_OTHER_LINE
}

data class PathValidationResult(
    val valid: Boolean,
    val reason: PathInvalidityStatus,
    val index: Int
) {
    val code: Int
        get() = if (valid) 1 else -1
}


fun validatePath(
    path: Path,
    board: Board,
    points: List<Point>,
    takenPositions: Set<Int> = setOf()
): PathValidationResult {
    val startingPoint = points.first { it.position == path.startingPosition }
    val visitedPositions = mutableSetOf(startingPoint.position)
    val positionsOfPointsOfOtherColors = points.filter { it.color != startingPoint.color }
        .map { it.position }
        .toSet()
    val pointsOfSameColor = points.filter { it.color == path.color }
    val otherPoint =
        if (pointsOfSameColor[0].position == path.startingPosition) pointsOfSameColor[1] else pointsOfSameColor[0]
    var current = startingPoint.coords
    path.steps.forEachIndexed { ind, it ->
        current = current.move(it)
        val currentPosition = current.position(board)
        if (takenPositions.contains(currentPosition)) {
            return PathValidationResult(false, PathInvalidityStatus.CROSSES_OTHER_LINE, ind + 1)
        }
        if (current.col < 1 || current.row < 1 || current.row > board.rows || current.col > board.cols) {
            return PathValidationResult(false, PathInvalidityStatus.OUT_OF_BOUNDS, ind + 1)
        }
        if (visitedPositions.contains(currentPosition)) {
            return PathValidationResult(false, PathInvalidityStatus.CROSSES_ITSELF, ind + 1)
        }
        if (positionsOfPointsOfOtherColors.contains(currentPosition)) {
            return PathValidationResult(false, PathInvalidityStatus.TOUCHES_POINT_OF_DIFFERENT_COLOR, ind + 1)
        }
        visitedPositions.add(currentPosition)
    }
    if (current.position(board) != otherPoint.position) {
        return PathValidationResult(false, PathInvalidityStatus.ENDS_IN_WRONG_PLACE, path.length)
    }
    return PathValidationResult(true, PathInvalidityStatus.VALID, path.length)
}

data class OptimizedValidatePathInput(
    val pointsByPosition: Map<Int, Point>,
    val pointsByColor: Map<Int, List<Point>>
)

fun validatePathV2(
    path: Path,
    board: Board,
    points: List<Point>,
    opt: OptimizedValidatePathInput,
    takenPositions: Set<Int> = setOf()
): PathValidationResult {
    val startingPoint = opt.pointsByPosition[path.startingPosition]!!
    val visitedPositions = mutableSetOf(startingPoint.position)
//    val positionsOfPointsOfOtherColors = opt.pointPositionsForOtherColors[path.color]!!
    val pointsOfSameColor = opt.pointsByColor[path.color]!!
    val sameColorPointPositions = pointsOfSameColor.map { it.position }.toSet()
    val otherPoint =
        if (pointsOfSameColor[0].position == path.startingPosition) pointsOfSameColor[1] else pointsOfSameColor[0]
    var current = startingPoint.coords
    path.steps.forEachIndexed { ind, it ->
        current = current.move(it)
        val currentPosition = current.position(board)
        if (takenPositions.contains(currentPosition)) {
            return PathValidationResult(false, PathInvalidityStatus.CROSSES_OTHER_LINE, ind + 1)
        }
        if (current.col < 1 || current.row < 1 || current.row > board.rows || current.col > board.cols) {
            return PathValidationResult(false, PathInvalidityStatus.OUT_OF_BOUNDS, ind + 1)
        }
        if (visitedPositions.contains(currentPosition)) {
            return PathValidationResult(false, PathInvalidityStatus.CROSSES_ITSELF, ind + 1)
        }
//        if (positionsOfPointsOfOtherColors.contains(currentPosition)) {
//            return PathValidationResult(false, PathInvalidityStatus.TOUCHES_POINT_OF_DIFFERENT_COLOR, ind + 1)
//        }
        if (opt.pointsByPosition.keys.contains(currentPosition) && !sameColorPointPositions.contains(currentPosition)) {
            return PathValidationResult(false, PathInvalidityStatus.TOUCHES_POINT_OF_DIFFERENT_COLOR, ind + 1)
        }
        visitedPositions.add(currentPosition)
    }
    if (current.position(board) != otherPoint.position) {
        return PathValidationResult(false, PathInvalidityStatus.ENDS_IN_WRONG_PLACE, path.length)
    }
    return PathValidationResult(true, PathInvalidityStatus.VALID, path.length)
}
