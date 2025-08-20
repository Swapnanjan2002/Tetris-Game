# Tetris Game in Java

This is a classic Tetris game built entirely in Java using the Swing library for the graphical user interface. It is a single-file project designed to be a comprehensive and feature-rich implementation of the timeless puzzle game.

<img width="672" height="858" alt="Tetris1" src="https://github.com/user-attachments/assets/b9f8d5d3-ebc3-4cf6-abfb-e9fd879edf39" />

<img width="672" height="862" alt="Tetris2" src="https://github.com/user-attachments/assets/b586dcab-2ddf-483f-acd5-5111916abe1a" />

<img width="672" height="861" alt="Tetris3" src="https://github.com/user-attachments/assets/81103e48-e004-40da-8821-a9b58e688e34" />



---

## Features

This Tetris clone includes a wide range of modern and classic features:

* **Classic Tetris Gameplay:** Move, rotate, and drop tetrominoes to clear lines.
* **Scoring System:** Includes a score and a persistent high score tracker that saves to a local file.
* **Dynamic Difficulty:** The game speed increases every 5 minutes for an escalating challenge.
* **Hold Piece:** Swap the current piece with a held piece once per turn by pressing 'C'.
* **Ghost Piece:** A transparent outline shows exactly where the current piece will land.
* **Hard Drop:** Instantly drop the current piece to the bottom with the Spacebar.
* **Line Clear Animation:** Completed lines flash briefly before disappearing.
* **Game States:** A full game experience with a Main Menu, Pause Screen, and Game Over Screen.
* **Professional UI:** A clean interface with a side panel that displays the score, next piece, and held piece.
* **Resizable Window:** The game window can be resized and maximized for a full-screen experience, with the game board remaining centered.

---

## Controls

The game is controlled using the keyboard:

| Key         | Action                |
| :---------- | :-------------------- |
| **← Left** | Move piece left       |
| **→ Right** | Move piece right      |
| **↓ Down** | Soft drop (move down) |
| **↑ Up** | Rotate piece          |
| **Spacebar**| Hard drop             |
| **C** | Hold / Swap piece     |
| **P** | Pause / Resume game   |
| **Enter** | Start game (from menu)|

---

## How to Run

This project is contained within a single Java file and has no external dependencies, making it very simple to run.

### Prerequisites

* Java Development Kit (JDK) 8 or higher.

### Compilation & Execution

1.  **Open a terminal** or command prompt.
2.  **Navigate to the directory** where you saved the `Tetris.java` file.
3.  **Compile the code** using the Java compiler:
    ```bash
    javac Tetris.java
    ```
4.  **Run the compiled game:**
    ```bash
    java Tetris
    ```

A window will appear, and you can start playing!
