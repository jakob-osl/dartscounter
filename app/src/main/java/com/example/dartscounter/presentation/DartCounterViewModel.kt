package com.example.dartscounter.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DartAction(
    val player: Int,  // 1 or 2
    val scoreBefore: Int,
    val scoreAfter: Int
)

data class InputAction(
    val button: String,
    val currentThrowIndex: Int, //1, 2 or 3
    val player: Int,
    val value: String
)

class DartCounterViewModel : ViewModel() {
    // Names
    var playerOneName = "Player 1"
    var playerTwoName = "Player 2"

    // Scores
    private val _playerOneScore = MutableStateFlow(501)
    val playerOneScore = _playerOneScore.asStateFlow()
    private val _playerTwoScore = MutableStateFlow(501)
    val playerTwoScore = _playerTwoScore.asStateFlow()

    // Current Player
    var currentPlayer = 1

    // Current Input
    private val _currentInput = MutableStateFlow("")
    val currentInput = _currentInput.asStateFlow()

    // Throw Counter in Current Turn (1 - 3)
    private val _throwsInCurrentTurn = MutableStateFlow(0) // Made reactive
    val throwsInCurrentTurn = _throwsInCurrentTurn.asStateFlow()

    // History
    private val history = mutableListOf<InputAction>()

    private val actionHistory = mutableListOf<DartAction>()
    private val _currentTurnThrows = MutableStateFlow<List<String>>(emptyList())
    val currentTurnThrows = _currentTurnThrows.asStateFlow()

    // Current three throws with pending input. Used in CurrentThrows element
    private val _currentTurnThrowsWithPending = MutableStateFlow<List<String>>(listOf("-", "-", "-"))
    val currentTurnThrowsWithPending = _currentTurnThrowsWithPending.asStateFlow()

    // Set of valid scores for one throw without multiplier
    private val validBaseScores = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 25)
    private val validMultipliedScores = (1..20).flatMap { listOf(it, it * 2, it * 3) }.toSet() + setOf(25, 50)

    // List of all the available buttons identified by the label
    private val _availableButtons = MutableStateFlow<List<String>>(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "↩"))
    val availableButtons = _availableButtons.asStateFlow()

    private fun addScore(scoreAsString: String) {
        val score = parseInputToScore(scoreAsString) ?: return

        val currentScore = if (currentPlayer == 1) _playerOneScore.value else _playerTwoScore.value
        val newScore = currentScore - score

        val updatedThrows = _currentTurnThrows.value.toMutableList().apply { add(scoreAsString) }
        _currentTurnThrows.value = updatedThrows
        updateCurrentTurnThrowsWithPending()
        println("addScore: Updated throws: ${_currentTurnThrows.value}")

        if (currentPlayer == 1) {
            _playerOneScore.value = newScore
        } else {
            _playerTwoScore.value = newScore
        }

        _throwsInCurrentTurn.value++

        if (_throwsInCurrentTurn.value == 3) {
            println("addScore: Three throws reached, switching turn")
            switchTurn()
        }

        resetAvailableButtons()
    }

    private fun switchTurn() {
        currentPlayer = if (currentPlayer == 1) 2 else 1
        _throwsInCurrentTurn.value = 0
        _currentTurnThrows.value = emptyList()
        updateCurrentTurnThrowsWithPending()
        println("switchTurn: Switched to Player $currentPlayer, cleared throws")
    }

    private fun undoLastAction() {
        if (actionHistory.isNotEmpty()) {
            val lastAction = actionHistory.removeAt(actionHistory.lastIndex)
            println("undoLastAction: Undoing action for Player ${lastAction.player}, restoring score to ${lastAction.scoreBefore}")

            if (lastAction.player == 1) {
                _playerOneScore.value = lastAction.scoreBefore
            } else {
                _playerTwoScore.value = lastAction.scoreBefore
            }

            if (_throwsInCurrentTurn.value == 0) {
                println("undoLastAction: No throws in current turn, switching turn")
                switchTurn()
            } else {
                _throwsInCurrentTurn.value--
                val updatedThrows = _currentTurnThrows.value.toMutableList()
                if (updatedThrows.isNotEmpty()) {
                    updatedThrows.removeAt(updatedThrows.lastIndex)
                    _currentTurnThrows.value = updatedThrows
                }
                println("undoLastAction: Updated throws: ${_currentTurnThrows.value}")
            }
        } else {
            println("undoLastAction: No actions to undo")
        }
        updateAvailableButtons()
    }

    fun onButtonClick(button: String) {
        println("onButtonClick: Button pressed: $button")
        when (button) {
            "OK" -> {
                val points = parseInputToScore(_currentInput.value)
                val pointsAsString = _currentInput.value

                if (points == null) {
                    println("onButtonClick: Invalid input ${_currentInput.value}, ignoring")
                    return
                }
                addScore(pointsAsString)
                _currentInput.value = ""
            }
            "x2" -> {
                _currentInput.value = if (_currentInput.value.isNotEmpty()) {
                    "D" + _currentInput.value
                } else {
                    _currentInput.value
                }
            }
            "x3" -> {
                _currentInput.value = if (_currentInput.value.isNotEmpty()) {
                    "T" + _currentInput.value
                } else {
                    _currentInput.value
                }
            }
            "↩" -> undoLastAction()
            else -> {
                _currentInput.value += button
                println("onButtonClick: After adding $button, currentInput: ${_currentInput.value}")
            }
        }
        updateCurrentTurnThrowsWithPending()
        updateAvailableButtons()
    }

    private fun updateAvailableButtons() {

        val input = _currentInput.value
        println("updateAvailableButtons: new input: $input")

        val allButtons = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "x2", "x3", "OK", "↩")

        _availableButtons.value = when {
            input.isEmpty() -> listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "↩")
            input == "1" -> allButtons
            input == "2" -> listOf("0", "5", "x2", "x3", "OK", "↩")
            input.toIntOrNull() in validBaseScores -> listOf("x2", "x3", "OK", "↩")
            else -> listOf("OK", "↩")
        }

        println("updateAvailableButtons: Current Available Buttons: ${_availableButtons.value}")

    }

    private fun resetAvailableButtons() {
        _availableButtons.value = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "x2", "x3", "OK", "↩")
        updateAvailableButtons()
    }

    private fun updateCurrentTurnThrowsWithPending() {
        val result = MutableList(3) { "-" }

        _currentTurnThrows.value.forEachIndexed { index, throwX ->
            result[index] = throwX.toString()
        }

        if (_currentInput.value.isNotEmpty() && _throwsInCurrentTurn.value < 3) {
            result[_throwsInCurrentTurn.value] = _currentInput.value
        }

        _currentTurnThrowsWithPending.value = result
    }

    private fun parseInputToScore(input: String): Int? {
        if (input.isEmpty()) return null

        try {
            if (input.toIntOrNull() != null) {
                return input.toInt()
            }

            val multiplier = when (input[0]) {
                'D' -> 2
                'T' -> 3
                else -> 1
            }

            val numberStr = if (multiplier > 1) input.substring(1) else input
            val number = numberStr.toIntOrNull() ?: return null

            return number * multiplier

        } catch (e: Exception) {
            println("parseInputToScore: Error parsing input $input: ${e.message}")
            return null
        }
    }

}