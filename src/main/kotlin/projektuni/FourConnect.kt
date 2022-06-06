package projektuni

import java.io.File
import java.io.InputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis


interface FourCGame {
    val playBoard : LongArray
    val move : IntArray
    val heightCol : IntArray
    var count : Int
    val prememory : HashMap<ULong, Pair<Int,Int>>
    fun isWin(bitboard: Long) : Boolean
    fun makeMove(n: Int) : FourCGame
    fun undoMove() : FourCGame
    fun listMoves() : ArrayList<Int>
    override fun toString() : String
    }





@ExperimentalUnsignedTypes
class FourConnect(override val playBoard : LongArray , override val move : IntArray, override val heightCol : IntArray , override var count : Int = 0, override val prememory : HashMap<ULong, Pair<Int,Int>> = HashMap()) : FourCGame {
    val height = 6
    val width = 7
    val size = height*width
    val heightBottom = height + 1 // aConnect Four column and row are not 64 -> have to change
    val h2 = height + 2
    val size1 = heightBottom * width // The tables with the top
    val allL : Long = (1L shl size1)-1L // Get all of the field
    val column = (1 shl heightBottom)-1 //get the column 0 1 2 3 4 5 6
    val bottom : Long = allL / column // get the Bottom of each column
    val top : Long = bottom shl height // get all the top element of each column
    // This array represents the bottom line 0 , 7 , 14, 21, 28, 35, 42
    var testBoard = ArrayList<IntArray>()

    companion object{
        var preparedMemory = HashMap<Int, Pair<Int,Int>>() //HashMap memory
        var preparedMemory2 = HashMap<ULong, Pair<Int,Int>>()
        var zobristKey = ULongArray(49*2)
        //@TEST  Changing Test scenario
        val testBoard : ArrayList<FourConnect> = arrayListOf(FourConnect(longArrayOf(13471164940419,242399313920),IntArray(42),intArrayOf(2, 8, 15, 22, 32, 39, 44), count = 1)
                , FourConnect(longArrayOf(34359754881,14680064),IntArray(42),intArrayOf(1, 8, 15, 24, 28, 36, 42), count = 1), FourConnect(longArrayOf(4398046511107,270532608),IntArray(42),intArrayOf(2, 7, 14, 22, 29, 35, 43), count = 1),
                FourConnect(longArrayOf(7,16512),IntArray(42),intArrayOf(3, 8, 15, 21, 28, 35, 42), count = 1), FourConnect(longArrayOf(270532608,1),IntArray(42),intArrayOf(1, 7, 14, 22, 29, 35, 42), count = 1) )
    }


    fun initZobrist(){
        for(i in zobristKey.indices){
            zobristKey[i] = (ULong.MIN_VALUE..ULong.MAX_VALUE).random()
        }
    }



    fun hashZobrist(board : LongArray) : ULong{
        var result = 0UL
        var piece = 0
        for (h in height-1 downTo 0){

            var w = h
            while (w < size1) {
                val mask = 1L shl w
                if ((board[0] and mask) != 0L) piece = 0 else if ((board[1] and mask) != 0L) piece = 1 else piece = -1
                if (piece != -1 && w < size1){
                    result = result xor  zobristKey[w*2+piece]
                }
                w += heightBottom
            }

        }

        return result
    }


    //Reset the game -> In addition we need to fill the heightCol Array again.
     fun reset() {
         count = 0
         playBoard[0] = 0L
         playBoard[1] = 0L
         for (i in 0 until width) {
             heightCol[i] = heightBottom * i
         }
     }


    override fun makeMove (n : Int) : FourConnect {
        if(!isPlayable(n)) return FourConnect(playBoard, move, heightCol, count)
        val changedBoard = playBoard.copyOf()
        changedBoard[(count and 1)] = changedBoard[count and 1] xor (1L shl heightCol[n]++)
        move[count++] = n

        return FourConnect(changedBoard,move,heightCol,count)
    }

    override fun undoMove () : FourConnect{
        val col = move[--count]
        val revertBoard = playBoard.copyOf()
        val moves : Long = 1L shl  --heightCol[col]
        revertBoard[count and 1] = revertBoard[count and 1 ] xor moves
        return FourConnect(revertBoard,move, heightCol, count)
    }



    fun islegal(bitboard : Long): Boolean { // Needed isLegal to make sure all the moves are legal and playable.
        return (bitboard and top) == 0L
    }

    fun isPlayable(col : Int) : Boolean {
        return islegal(playBoard[count and 1] or (1L shl heightCol[col])) //Make sure the move is playabele
    }

    //MonteCarlo -> Play Random
    fun playRandomly(playBoard: FourConnect) : Int {
        var game = playBoard
        var value = if (isWin(game.playBoard[0])) return 1 else if (isWin(game.playBoard[1])) return -1 else 0
        while (value == 0){
            val moves = game.listMoves()
            if (moves.isEmpty()) return 0
            var randomMove = moves[(0 until moves.size).random()]
             game = game.makeMove(randomMove)
            val boardHash = game.playBoard.contentHashCode()
            value = if (isWin(game.playBoard[0])){
                return 1
            } else if (isWin(game.playBoard[1])){
                return -1
            } else 0
        }
        return value
    }
    //MonteCarlo -> Evaluation
    fun evaluateMoves(playBoards: FourConnect, number: Int =100) : ArrayList<IntArray>{
        var game = FourConnect(playBoards.playBoard.copyOf(), playBoards.move.copyOf(), playBoards.heightCol.copyOf(), playBoards.count)
        val moves = game.listMoves()
        val values = ArrayList<IntArray>()
        for (move in moves){
            game = game.makeMove(move)
            values.add(simulatePlays(game, number))
            game = game.undoMove()
        }
        return values
    }

    // MonteCarlo -> Simulate Play
    fun simulatePlays(playBoards: FourConnect, number : Int = 100) : IntArray{
        var num = number
        val counter = intArrayOf(0 , 0 , 0)

        while (num > 0 ){
            val game = FourConnect(playBoards.playBoard.copyOf(), playBoards.move.copyOf(), playBoards.heightCol.copyOf(), playBoards.count)
            counter[playRandomly(game)+1] +=1
            num--
        }
        return counter
    }


    //BestMove for montecarlo
    fun chooseBestMove(playBoards: FourConnect, number: Int = 100): Int{
        val moves = playBoards.listMoves()
        val evaluate = playBoards.evaluateMoves(playBoards, number)
        val values = IntArray(evaluate.size)
        for (i in 0 until evaluate.size ){
            var turn = if (playBoards.count and 1 == 0) 1 else -1
            values[i] = evaluate[i][2] * turn
        }

        val bestvalue = values.max()
        if (bestvalue == null) return 0
        val bestindx = values.indexOf(bestvalue)
        return if (bestindx >= moves.size){
            moves[moves.size-1]
        } else {
            moves[bestindx]
        }

    }


    override fun toString() : String {
        var s = ""

        for (i in 0 until width) {
            s+= " $i"
            s+= ""

        }

        s+= "\n"

        for (h in height-1 downTo 0){

            var w = h
            while (w < size1) {
                val mask = 1L shl w
                if ((playBoard[0] and mask) != 0L) s += " X" else if ((playBoard[1] and mask) != 0L) s += " O" else s += " ."
                w += heightBottom
            }
            s+= "\n"
        }
        if (isWin(playBoard[0])) s+= "\n" + " X WON"
        if (isWin(playBoard[1])) s+= "\n" + " O WON"
        return s
    }


    fun toStringWeb() : String {
        var s = ""
        for (h in height-1 downTo 0) {
            s += "<tr>\n"
            var w = h
            while (w < size1) {
                val mask = 1L shl w
                val color = (if (playBoard[0] and mask != 0L) "yellow" else if (playBoard[1] and mask != 0L) "red" else "free")
                w += heightBottom
                s += String.format("<td>\n   <svg height=\"70\" width=\"70\">\n   <circle cx=\"35\" cy=\"35\" r=\"32\" stroke=\"black\" stroke-width=\"3\" class=\"%s\" />\n</svg>\n</td>\n", color)
            }
            s += "</tr>\n"
        }

        val openMoves = listMoves()
        if(isWin(playBoard[0])){
            s+= "<div  id=\"myAlert\"  class=\"alert alert-success alert-dismissible\">\n" +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>\n" +
                    "  <strong>YELLOW WON</strong>" +
                    "</div>"
            return s
        }else if(isWin(playBoard[1])){
            s+= "<div  id=\"myAlert\"    class=\"alert alert-success alert-dismissible\">\n" +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>\n" +
                    "  <strong>RED WON</strong>" +
                    "</div>"
            return s
        }else if(openMoves.isEmpty()){
            s+= "<div  id=\"myAlert\"    class=\"alert alert-success alert-dismissible\">\n" +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>\n" +
                    "  <strong>ITS A TIE!!</strong>" +
                    "</div>"
            return s
        } else if (count and 1 == 0){
            s+= "<div  id=\"myAlert\"    class=\"alert alert-success alert-dismissible\">\n" +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>\n" +
                    "  <strong>YELLOW TURN</strong>" +
                    "</div>"
        } else if (count and 1 == 1){
            s+= "<div  id=\"myAlert\"    class=\"alert alert-success alert-dismissible\">\n" +
                    "  <button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>\n" +
                    "  <strong>RED TURN</strong>" +
                    "</div>"
        }

        return s
    }

    //Checkt the winner
    override fun isWin(bitboard : Long) : Boolean {
        val directions = intArrayOf(1,7,6,8) // Winning directions
        var bb : Long
        for (dir in directions) {
            bb = bitboard and (bitboard shr dir)
            val b2 = bb shr (2*dir)
            if ((bb and b2)!= 0L) {
                var b1 = bb and (bb shr (2*dir))
                return true
            }
        }
        return false
    }

    override fun listMoves() : ArrayList<Int> {
        val moves = ArrayList<Int>()
        for (i in 0 until 7) {
            if ((top and (1L shl heightCol[i])) == 0L) moves.add(i)
        }

        return moves
    }






    //Minimax -> returns a Pair of int and hashmap
    fun minimaxNext(board: FourConnect,depth: Int ,memory: HashMap<ULong, Pair<Int, Int>> = HashMap()) : Pair<Int, HashMap<ULong,Pair<Int,Int>>> {
        var game = FourConnect(board.playBoard.copyOf(),board.move.copyOf(),board.heightCol.copyOf(),board.count,board.prememory)
        var movePair = Pair(-1, memory)
        var bestScore = Int.MIN_VALUE
        val listM = game.listMoves()
        var montecarlo = Pair(listM.first(),0)


        for (moves in listM) {
            game = game.makeMove(moves)
            val boardH = hashZobrist(game.playBoard)
            var score = 0
            if (preparedMemory2.containsKey(boardH)) {
                score = preparedMemory2[boardH]!!.second
            } else{
                val result = mini(game, depth, Int.MIN_VALUE, Int.MAX_VALUE, memory )
                score =  result.second
            }
            game = game.undoMove()
            if (score > bestScore){
                bestScore = score
                memory[boardH] = Pair(moves,bestScore)
                movePair = Pair(moves, memory)
                preparedMemory2[boardH] = Pair(moves,bestScore)
                montecarlo = Pair(moves, bestScore)
            }
            preparedMemory2[boardH] = Pair(moves,score)
        }

        if (montecarlo == Pair(listM.first(),0)) {
            val boardH2 = hashZobrist(game.playBoard)
            preparedMemory2[boardH2] = Pair(chooseBestMove(game),bestScore)
            return Pair(preparedMemory2[boardH2]!!.first, memory)
        }



        return movePair
    }

    //mini and maxi for minimax
    fun mini(board: FourConnect, depth: Int ,alpha: Int,beta : Int, memory: HashMap<ULong, Pair<Int, Int>>) : Pair<Int,Int>{
        var game = FourConnect(board.playBoard.copyOf(),board.move.copyOf(),board.heightCol.copyOf(),board.count,board.prememory)
        if (isWin(game.playBoard[1])) return Pair(-1, 100000+depth) else if (depth == 0) return Pair(-1,0 ) else if (isWin(game.playBoard[0])) return Pair(-1, -100000)
        //if (preparedMemory.containsKey(boardHash)) return preparedMemory[boardHash]!! rintln("Tiefe : $depth")
        var bestScore = beta
        val listM = game.listMoves()
        var movePair = Pair(-1,0)
        for (moves in listM) {
            game = game.makeMove(moves)
            val boardH = hashZobrist(game.playBoard)
            var score = 0
            if (preparedMemory2.containsKey(boardH)) {
                score = preparedMemory2[boardH]!!.second
            } else{
                val result = maxi(game, depth-1, alpha, bestScore, memory )
                score =  result.second
            }
            game = game.undoMove()
            if (score < bestScore) {
                bestScore = score
                movePair = Pair(moves,bestScore)
                if (bestScore <= alpha) break
            }
        }

        return movePair
    }


    //mini and maxi for minimax
    fun maxi(board : FourConnect , depth: Int,alpha: Int,beta : Int, memory: HashMap<ULong, Pair<Int, Int>>) : Pair<Int,Int> {
        var game = FourConnect(board.playBoard.copyOf(),board.move.copyOf(),board.heightCol.copyOf(),board.count,board.prememory)
        if (isWin(game.playBoard[0])) return Pair(-1, -100000) else if (depth == 0) return Pair(-1, 0) else if (isWin(game.playBoard[1])) return Pair(-1, 100000+depth)
        //if (preparedMemory.containsKey(boardHash)) return preparedMemory[boardHash]!!
        var bestScore = alpha
        val listM = game.listMoves()
        var movePair = Pair(-1,0)
        for (moves in listM) {
            game = game.makeMove(moves)
            val boardH = hashZobrist(game.playBoard)
            var score = 0
            if (preparedMemory2.containsKey(boardH)) {
                score = preparedMemory2[boardH]!!.second
            } else{
                val result = mini(game, depth-1, bestScore, beta , memory )
                score =  result.second
            }
            game = game.undoMove()
            if (score > bestScore){
                bestScore = score
                movePair = Pair(moves, bestScore)
                if (bestScore >= beta) break
            }
        }
        return movePair
    }


    // returns a bestMove using Minimax
    fun bestMoveNxt(board: FourConnect, depth: Int = 10) : Int {
        val game = FourConnect(board.playBoard.copyOf(),board.move.copyOf(),board.heightCol.copyOf(),board.count,board.prememory)
        var moves = 0
        val second = measureTimeMillis { moves = minimaxNext(game, depth, game.prememory).first  }

       // moved.second.forEach{ key , value -> println("KEY $key + Value $value")}
        println("Minimax made move in : $second ms")
        return moves
    }

    fun getBoardCode(game : FourConnect) : String {
        var s="FourConnect("
        s+="longArrayOf(${game.playBoard[0]},${game.playBoard[1]}),IntArray(42),intArrayOf(${game.heightCol[0]}"
        for (i in 1 until game.heightCol.size){
            s+=",${game.heightCol[i]}"
        }
        s+="))"

        return s
    }


    //Needed to prepare HashMap with the database.
    fun simulateMinimax(board: FourConnect , depth: Int) : Int {
        var game = FourConnect(board.playBoard.copyOf(),board.move.copyOf(),board.heightCol.copyOf(),board.count,board.prememory)
        val list = game.listMoves()
        if (depth == 10) return  1 else if (list.isEmpty()) return 1
        if(depth and 1 == 1){
            while (!isWin(game.playBoard[0]) && !isWin(game.playBoard[1]) ) {
                val list2 = game.listMoves()
                if (list2.isEmpty()) break
                val moves = list2.random()
                game = game.makeMove(moves)
                if (game.listMoves().isEmpty()) break
                val bestM = game.bestMoveNxt(game, 10)
                if (bestM == -1) break
                game = game.makeMove(bestM)

            }
            if (isWin(game.playBoard[0])) println("X won") else if (isWin(game.playBoard[1])) println("O Won") else if (game.listMoves().isEmpty()) println("Its a tie")
            game.reset()
        } else {
            while (!isWin(game.playBoard[0]) && !isWin(game.playBoard[1]) ) {
                val list2 = game.listMoves()
                if (list2.isEmpty()) break
                val moves = game.chooseBestMove(game)
                game = game.makeMove(moves)
                if (game.listMoves().isEmpty()) break
                val bestM = game.bestMoveNxt(game, 10)
                if (bestM == -1) break
                game = game.makeMove(bestM)

            }
            if (isWin(game.playBoard[0])) println("X won") else if (isWin(game.playBoard[1])) println("O Won") else if (game.listMoves().isEmpty()) println("Its a tie")
            game.reset()
        }
        return simulateMinimax(game, depth +1)
    }

}


@ExperimentalUnsignedTypes
fun main(){
    var game = FourConnect(longArrayOf(0L,0L), IntArray(42), IntArray(7))
    game.reset()

    val inputStream: InputStream = File("zobrist.txt").inputStream()
    val lineList = mutableListOf<String>()
    inputStream.bufferedReader().useLines { lines -> lines.forEach { lineList.add(it)} }
    var i = 0
    println("Initializing Memory")
    for (element in lineList) {
        FourConnect.zobristKey[i] = element.toULong()
        i++
    }

    val inputStream2: InputStream = File("databasezobrist.txt").inputStream()
    val lineList2 = mutableListOf<String>()

    inputStream2.bufferedReader().useLines { lines -> lines.forEach { lineList2.add(it)} }
    for(element in lineList2){
        var element1 = element
        element1 = element1.replace("\\(".toRegex(), "")
        element1 = element1.replace("\\)".toRegex(), "")
        element1 = element1.replace(" ".toRegex(), "")
        val index = element1.indexOf('=')
        val num = if(index == -1) 0 else element1.substring(0,index).toULong()
        val ulong = num.toString()
        val num4 = ulong.toULong()
        val index2 = element1.indexOf(',')
        val num1 = if(index2 == -1) -1 else element1.substring(index+1,index2).toInt()
        val num2 = if(index2 == -1) -1 else element1.substring(index2+1,element1.length).toInt()

        FourConnect.preparedMemory2[num4] = Pair(num1,num2)
    }

    println("Initializing Complete!")


    var j = 0
    while (j<50){
        game.simulateMinimax(game, 0)
        game.reset()
        j++
    }

}