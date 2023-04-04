import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.system.exitProcess

typealias Grid = Array<IntArray>

class Global {
    companion object {
        var SCORE = 0
    }
}

fun countScore(number: Int) {
    Global.SCORE += number * 2
}

fun newGrid(): Grid {
    return Array(4) {
        IntArray(4) { 0 }
    }
}

fun Grid.findMax(): Int {
    var max = 0
    for (row in this) {
        for (num in row) {
            if (num > max) {
                max = num
            }
        }
    }
    return max
}

fun Grid.print() {
    val cellWidth = this.findMax().toString().length
    val width = "-".repeat(cellWidth)
    println(" -$width-".repeat(4))
    for ((i, row) in this.withIndex()) {
        print("|")
        for ((j, num) in row.withIndex()) {
            val numStr = "%${cellWidth}d".format(num)
            print(if (num != 0) " $numStr " else " ${" ".repeat(cellWidth)} ")
            print(if (j != 3) "|" else "")
        }
        println("|")
        print(if (i != 3) "|${Array(4) { "-$width-" }.joinToString(" ")}|\n" else "")
    }
    println(" -$width-".repeat(4))

}

const val FOUR_RATE = 0.1
fun Grid.init() {
    val (x1, x2) = (0 until 4).shuffled().take(2)
    val (y1, y2) = (0 until 4).shuffled().take(2)
    this[x1][y1] = if (Math.random() < FOUR_RATE) 4 else 2
    this[x2][y2] = if (Math.random() < FOUR_RATE) 4 else 2
}

fun Grid.copy(): Grid {
    val new = newGrid()
    for ((i, row) in this.withIndex()) {
        for ((j, num) in row.withIndex()) {
            new[i][j] = num
        }
    }
    return new
}

data class Point(val x: Int, val y: Int)

fun Grid.get(p: Point): Int {
    return this[p.x][p.y]
}

fun Grid.set(p: Point, v: Int) {
    this[p.x][p.y] = v
}

fun Grid.getEmpty(): List<Point> {
    val empty = mutableListOf<Point>()
    for ((i, row) in this.withIndex()) {
        for ((j, num) in row.withIndex()) {
            if (num == 0) {
                empty.add(Point(i, j))
            }
        }
    }
    return empty
}

class FullGridException(msg: String = "The Grid is full") : Exception(msg)

fun Grid.pop() {
    // pop new number in remaining empty cell if there is one.
    val empty = this.getEmpty()
    if (empty.isEmpty()) {
        throw FullGridException()
    }
    val (x, y) = empty.shuffled().take(1).first()
    this[x][y] = if (Math.random() < FOUR_RATE) 4 else 2
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class InvalidMovementException(move: String) : Exception("Invalid Movement: $move")

fun readDirection(): Direction {
    val move = readlnOrNull() ?: throw InvalidMovementException("null")
    return when (move.lowercase()) {
        "u", "up" -> Direction.UP
        "d", "down" -> Direction.DOWN
        "l", "left" -> Direction.LEFT
        "r", "right" -> Direction.RIGHT
        else -> throw InvalidMovementException(move)
    }
}

fun getIterPoint(direction: Direction): List<Point> {
    val points = mutableListOf<Point>()
    when (direction) {
        Direction.LEFT -> {
            for (x in 0 until 4) {
                for (y in 0 until 4) {
                    points.add(Point(x, y))
                }
            }
        }

        Direction.UP -> {
            for (y in 0 until 4) {
                for (x in 0 until 4) {
                    points.add(Point(x, y))
                }
            }
        }

        Direction.RIGHT -> {
            for (x in 0 until 4) {
                for (y in 3 downTo 0) {
                    points.add(Point(x, y))
                }
            }
        }

        Direction.DOWN -> {
            for (y in 0 until 4) {
                for (x in 3 downTo 0) {
                    points.add(Point(x, y))
                }
            }
        }
    }
    return points
}

fun Grid.slide(direction: Direction): Boolean {
    // return if it really slides.
    val copied = this.copy()
    val shiftEmpty = when (direction) {
        Direction.UP -> Point(1, 0)
        Direction.RIGHT -> Point(0, -1)
        Direction.DOWN -> Point(-1, 0)
        Direction.LEFT -> Point(0, 1)
    }
    var currEmpty: Point? = null
    for ((idx, point) in getIterPoint(direction).withIndex()) {
        val num = this.get(point)
        if (idx % 4 == 0) {
            currEmpty = null
        }
        if (num == 0 && currEmpty == null) {
            currEmpty = point
        }
        if (num != 0 && currEmpty != null) {
            this.set(currEmpty, num)
            this.set(point, 0)
            currEmpty = Point(currEmpty.x + shiftEmpty.x, currEmpty.y + shiftEmpty.y)
        }
    }
    return !copied.contentDeepEquals(this)
}

fun Grid.merge(direction: Direction) {
    var currNumPoint: Point? = null
    for ((idx, point) in getIterPoint(direction).withIndex()) {
        val num = this.get(point)
        if (idx % 4 == 0) {
            currNumPoint = null
        }
        if (num == 0) {
            continue
        }
        if (currNumPoint == null) {
            currNumPoint = point
            continue
        }
        val currNum = this.get(currNumPoint)
        currNumPoint = if (num == currNum) {
            countScore(num)
            this.set(point, num * 2)
            this.set(currNumPoint, 0)
            null
        } else {
            point
        }
    }
}

class GameOverException(msg: String = "Game Over") : Exception(msg)

fun Grid.checkAvailable(): Boolean {
    val empty = this.getEmpty()
    if (empty.isEmpty()) {
        val copied = this.copy()
        for (move in Direction.values()) {
            copied.merge(move)
            if (!copied.contentDeepEquals(this)) {
                return true
            }
        }
        return false
    }
    return true
}

fun Grid.isWin(): Boolean {
    for (row in this) {
        for (num in row) {
            if (num >= 2048) {
                return true
            }
        }
    }
    return false
}

fun Grid.reset() {
    for (i in 0 until 4) {
        for (j in 0 until 4) {
            this[i][j] = 0
        }
    }
}

fun Grid.move() {
    // a combination each move of the player perform
    // include read movement from user input ect
    if (this.checkAvailable()) {
        var move: Direction? = null
        while (move == null) {
            try {
                move = readDirection()
            } catch (e: InvalidMovementException) {
                var code: String? = null
                val regex = Regex("^Invalid Movement: (.+)$")
                val matchResult = regex.matchEntire(e.message!!)
                if (matchResult != null) {
                    code = matchResult.groups[1]?.value
                }
                when (code) {
                    ":s" -> {
                        saveGame(this)
                        println("Save Success!")
                        exitProcess(0)
                    }

                    ":n" -> {
                        this.reset()
                        this.init()
                        Global.SCORE = 0
                        return
                    }

                    else -> {
                        println(e.message)
                        println("Please enter an valid movement: up, down, left, right.")
                    }
                }
            }
        }
        this.merge(move)
        val success = this.slide(move)
        if (success) {
            this.pop()
        }
    } else {
        throw GameOverException()
    }
}

class Game(val grid: Grid = newGrid())

fun startGame() {
    val game = openSave()
    if (game.grid.contentDeepEquals(newGrid())) {
        game.grid.init()
    } else {
        println("Save File Found and Load Success!\n")
    }
    try {
        while (!game.grid.isWin()) {
            println("Current Score: ${Global.SCORE}")
            game.grid.print()
            game.grid.move()
        }
        println("You Win! (score: ${Global.SCORE}")
    } catch (e: GameOverException) {
        println("${e.message} (score: ${Global.SCORE})")
    }
}

fun saveGame(grid: Grid) {
    val file = File(".2048Save")
    val numbers = mutableListOf<Int>()
    for (row in grid) {
        for (num in row) {
            numbers.add(num)
        }
    }
    if (file.exists()) {
        file.delete()
    }
    BufferedWriter(FileWriter(file)).use {
        it.write("${numbers.joinToString(" ")}\n")
        it.write(Global.SCORE.toString())
    }
}

fun openSave(): Game {
    val file = File(".2048Save")
    if (file.exists()) {
        val text = file.readText()
        try {
            val (number, score) = text.split("\n")
            val numbers = number.split(" ")
            val grid = newGrid()
            for (i in 0 until 4) {
                for (j in 0 until 4) {
                    grid[i][j] = numbers[i * 4 + j].toInt()
                }
            }
            Global.SCORE = score.toInt()
            return Game(grid)
        } catch (e: Exception) {
            println("Invalid Save File!")
        }
    }
    return Game()
}

fun printWelcome() {
    println("Hi! This is a little 2048 game.")
    println("You can type \"Up, Down, Left, Right\" to control the move.")
    println("\"u, d, l, r\" are also valid move.")
    println("To save and exit, you can type \":s\"")
    println("To restart the game, you can type \":n\"")
    println("I hope you enjoy the game. Thanks!")
    println()
}

fun main() {
    printWelcome()
    startGame()
}