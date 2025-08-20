import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * The main class for the Tetris game. It sets up the window (JFrame) and
 * contains the main game panel (JPanel).
 */
public class Tetris extends JPanel implements Runnable, KeyListener {

    // --- Game States ---
    private enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER, LINE_CLEAR_ANIMATION
    }

    // --- Game Constants ---
    public static final int BOARD_WIDTH_IN_BLOCKS = 12;
    public static final int BOARD_HEIGHT_IN_BLOCKS = 22;
    public static final int BLOCK_SIZE = 30;
    public static final int SIDE_PANEL_WIDTH = 180;

    public static final int BOARD_WIDTH_PX = BOARD_WIDTH_IN_BLOCKS * BLOCK_SIZE;
    public static final int PANEL_WIDTH = BOARD_WIDTH_PX + SIDE_PANEL_WIDTH;
    public static final int PANEL_HEIGHT = BOARD_HEIGHT_IN_BLOCKS * BLOCK_SIZE;
    public static final String TITLE = "Tetris";
    private static final int FPS = 60;
    private static final long TARGET_TIME = 1000 / FPS;
    
    private static final int SPEED_INCREASE_INTERVAL = 18000;
    private static final int MIN_FALL_DELAY = 5;
    private static final int ANIMATION_DELAY = 20; // 1/3 of a second for animation
    private static final String HIGHSCORE_FILE = System.getProperty("user.home") + File.separator + "tetris_highscore.txt";


    // --- Piece Definitions ---
    private static final int[][][] SHAPES = {
            {{1, 1, 1, 1}}, // I
            {{1, 1}, {1, 1}}, // O
            {{0, 1, 0}, {1, 1, 1}}, // T
            {{0, 0, 1}, {1, 1, 1}}, // L
            {{1, 0, 0}, {1, 1, 1}}, // J
            {{0, 1, 1}, {1, 1, 0}}, // S
            {{1, 1, 0}, {0, 1, 1}}  // Z
    };
    private static final Color[] COLORS = {
            Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.ORANGE,
            Color.BLUE, Color.GREEN, Color.RED
    };


    // --- Game State Variables ---
    private Thread gameThread;
    private boolean running = false;
    private Color[][] board;
    private Piece currentPiece, nextPiece, holdPiece, ghostPiece;
    private boolean canHold = true;
    private Random random = new Random();
    private int fallCounter = 0;
    private int score = 0;
    private int highScore = 0;
    private GameState gameState = GameState.MENU;
    private int currentFallDelay = 60;
    private long gameTimeCounter = 0;
    
    // Animation variables
    private int animationTimer = 0;
    private List<Integer> linesToClear = new ArrayList<>();

    /**
     * Constructor for the main game panel.
     */
    public Tetris() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        loadHighScore();
    }

    private void startGame() {
        board = new Color[BOARD_HEIGHT_IN_BLOCKS][BOARD_WIDTH_IN_BLOCKS];
        score = 0;
        currentFallDelay = 60;
        gameTimeCounter = 0;
        holdPiece = null;
        canHold = true;
        nextPiece = null;
        spawnPiece(); 
        spawnPiece(); 
        gameState = GameState.PLAYING;
    }

    private void spawnPiece() {
        currentPiece = nextPiece;
        int randomIndex = random.nextInt(SHAPES.length);
        nextPiece = new Piece(SHAPES[randomIndex], COLORS[randomIndex]);
        
        if (currentPiece != null) {
            currentPiece.x = BOARD_WIDTH_IN_BLOCKS / 2 - currentPiece.shape[0].length / 2;
            currentPiece.y = 0;
            updateGhostPiece();
            if (collides()) {
                if (score > highScore) {
                    highScore = score;
                    saveHighScore();
                }
                gameState = GameState.GAME_OVER;
            }
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (gameThread == null) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            long startTime = System.nanoTime();
            
            if (gameState == GameState.PLAYING) {
                update();
            } else if (gameState == GameState.LINE_CLEAR_ANIMATION) {
                updateAnimation();
            }

            repaint();
            long elapsedTime = (System.nanoTime() - startTime) / 1000000;
            long waitTime = TARGET_TIME - elapsedTime;

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void update() {
        gameTimeCounter++;
        if (gameTimeCounter % SPEED_INCREASE_INTERVAL == 0 && currentFallDelay > MIN_FALL_DELAY) {
            currentFallDelay -= 5;
        }
        
        fallCounter++;
        if (fallCounter >= currentFallDelay) {
            fallCounter = 0;
            movePieceDown();
        }
    }
    
    private void updateAnimation() {
        animationTimer++;
        if (animationTimer > ANIMATION_DELAY) {
            animationTimer = 0;
            
            // Actually remove the lines from the board data
            for (int row : linesToClear) {
                for (int moveRow = row; moveRow > 0; moveRow--) {
                    System.arraycopy(board[moveRow - 1], 0, board[moveRow], 0, BOARD_WIDTH_IN_BLOCKS);
                }
                board[0] = new Color[BOARD_WIDTH_IN_BLOCKS];
            }
            
            // Update score
            score += 100 * Math.pow(2, linesToClear.size() - 1);
            linesToClear.clear();
            gameState = GameState.PLAYING;
        }
    }

    private void movePieceDown() {
        if (currentPiece == null) return;
        currentPiece.y++;
        if (collides()) {
            currentPiece.y--;
            lockPiece();
        }
    }

    private void hardDrop() {
        if (currentPiece == null) return;
        currentPiece.y = ghostPiece.y;
        lockPiece();
    }

    private void hold() {
        if (!canHold) return;

        if (holdPiece == null) {
            holdPiece = new Piece(currentPiece.shape, currentPiece.color);
            spawnPiece();
        } else {
            Piece temp = new Piece(currentPiece.shape, currentPiece.color);
            currentPiece = new Piece(holdPiece.shape, holdPiece.color);
            holdPiece = temp;
            currentPiece.x = BOARD_WIDTH_IN_BLOCKS / 2 - currentPiece.shape[0].length / 2;
            currentPiece.y = 0;
        }
        canHold = false;
        updateGhostPiece();
    }

    private void rotatePiece() {
        if (currentPiece == null) return;
        int[][] currentShape = currentPiece.shape;
        int rows = currentShape.length;
        int cols = currentShape[0].length;
        int[][] newShape = new int[cols][rows];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                newShape[c][rows - 1 - r] = currentShape[r][c];
            }
        }

        int[][] originalShape = currentPiece.shape;
        currentPiece.shape = newShape;
        int originalX = currentPiece.x;

        if (collides()) {
            currentPiece.x++;
            if (collides()) {
                currentPiece.x = originalX - 1;
                if (collides()) {
                    currentPiece.x = originalX;
                    currentPiece.shape = originalShape;
                }
            }
        }
        updateGhostPiece();
    }

    private boolean collides() {
        if (currentPiece == null) return false;
        return pieceCollidesAt(currentPiece, currentPiece.x, currentPiece.y);
    }
    
    private boolean pieceCollidesAt(Piece piece, int x, int y) {
        if (piece == null) return false;
        for (int row = 0; row < piece.shape.length; row++) {
            for (int col = 0; col < piece.shape[row].length; col++) {
                if (piece.shape[row][col] == 1) {
                    int boardX = x + col;
                    int boardY = y + row;

                    if (boardX < 0 || boardX >= BOARD_WIDTH_IN_BLOCKS || boardY >= BOARD_HEIGHT_IN_BLOCKS) {
                        return true;
                    }
                    if (boardY < 0) continue;
                    if (board[boardY][boardX] != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void lockPiece() {
        if (currentPiece == null) return;
        for (int row = 0; row < currentPiece.shape.length; row++) {
            for (int col = 0; col < currentPiece.shape[row].length; col++) {
                if (currentPiece.shape[row][col] == 1) {
                    if (currentPiece.y + row >= 0) {
                       board[currentPiece.y + row][currentPiece.x + col] = currentPiece.color;
                    }
                }
            }
        }
        canHold = true;
        clearLines();
        if (gameState == GameState.PLAYING) { // Only spawn new piece if not animating
            spawnPiece();
        }
    }

    private void clearLines() {
        for (int row = BOARD_HEIGHT_IN_BLOCKS - 1; row >= 0; row--) {
            boolean lineIsFull = true;
            for (int col = 0; col < BOARD_WIDTH_IN_BLOCKS; col++) {
                if (board[row][col] == null) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                linesToClear.add(row);
            }
        }
        
        if (!linesToClear.isEmpty()) {
            gameState = GameState.LINE_CLEAR_ANIMATION;
        }
    }
    
    private void updateGhostPiece() {
        if (currentPiece == null) return;
        ghostPiece = new Piece(currentPiece.shape, currentPiece.color);
        ghostPiece.x = currentPiece.x;
        ghostPiece.y = currentPiece.y;
        while (!pieceCollidesAt(ghostPiece, ghostPiece.x, ghostPiece.y + 1)) {
            ghostPiece.y++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case PLAYING:
            case LINE_CLEAR_ANIMATION: // Draw board during animation too
                drawPlayingState(g2d);
                break;
            case PAUSED:
                drawPlayingState(g2d);
                drawPauseScreen(g2d);
                break;
            case GAME_OVER:
                drawPlayingState(g2d);
                drawGameOver(g2d);
                break;
        }
        g2d.dispose();
    }

    private void drawPlayingState(Graphics2D g2d) {
        drawBoard(g2d);
        if (ghostPiece != null) drawPiece(g2d, ghostPiece, true);
        if (currentPiece != null) drawPiece(g2d, currentPiece, false);
        drawSidePanel(g2d);
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.drawString("TETRIS", PANEL_WIDTH / 2 - 100, PANEL_HEIGHT / 3);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("High Score: " + highScore, PANEL_WIDTH / 2 - 80, PANEL_HEIGHT / 2);
        g2d.drawString("Press Enter to Start", PANEL_WIDTH / 2 - 110, PANEL_HEIGHT / 2 + 60);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.drawString("GAME OVER", PANEL_WIDTH / 2 - 150, PANEL_HEIGHT / 3);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.drawString("Your Score: " + score, PANEL_WIDTH / 2 - 85, PANEL_HEIGHT / 2);
        g2d.drawString("Press Enter to Play Again", PANEL_WIDTH / 2 - 140, PANEL_HEIGHT / 2 + 60);
    }
    
    private void drawPauseScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.drawString("PAUSED", PANEL_WIDTH / 2 - 100, PANEL_HEIGHT / 2);
    }

    private void drawSidePanel(Graphics2D g2d) {
        g2d.setColor(new Color(20, 20, 20));
        g2d.fillRect(BOARD_WIDTH_PX, 0, SIDE_PANEL_WIDTH, PANEL_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        
        g2d.drawString("SCORE", BOARD_WIDTH_PX + 45, 50);
        g2d.drawString(String.valueOf(score), BOARD_WIDTH_PX + 45, 80);
        g2d.drawString("HIGH SCORE", BOARD_WIDTH_PX + 20, 150);
        g2d.drawString(String.valueOf(highScore), BOARD_WIDTH_PX + 45, 180);

        g2d.drawString("HOLD", BOARD_WIDTH_PX + 55, 250);
        if (holdPiece != null) {
            drawPiece(g2d, holdPiece, BOARD_WIDTH_PX + 40, 280, false);
        }

        g2d.drawString("NEXT", BOARD_WIDTH_PX + 55, 420);
        if (nextPiece != null) {
            drawPiece(g2d, nextPiece, BOARD_WIDTH_PX + 40, 450, false);
        }
    }

    private void drawBoard(Graphics2D g2d) {
        for (int row = 0; row < BOARD_HEIGHT_IN_BLOCKS; row++) {
            for (int col = 0; col < BOARD_WIDTH_IN_BLOCKS; col++) {
                // Animation logic: flash the line to be cleared
                if (linesToClear.contains(row) && animationTimer % 10 < 5) { // Flash effect
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                } else if (board[row][col] != null) {
                    drawBlock(g2d, col * BLOCK_SIZE, row * BLOCK_SIZE, board[row][col], false);
                }
            }
        }
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i <= BOARD_WIDTH_IN_BLOCKS; i++) {
            g2d.drawLine(i * BLOCK_SIZE, 0, i * BLOCK_SIZE, PANEL_HEIGHT);
        }
        for (int i = 0; i <= BOARD_HEIGHT_IN_BLOCKS; i++) {
            g2d.drawLine(0, i * BLOCK_SIZE, BOARD_WIDTH_PX, i * BLOCK_SIZE);
        }
    }

    private void drawPiece(Graphics2D g2d, Piece piece, boolean isGhost) {
        for (int row = 0; row < piece.shape.length; row++) {
            for (int col = 0; col < piece.shape[row].length; col++) {
                if (piece.shape[row][col] == 1) {
                    int drawX = (piece.x + col) * BLOCK_SIZE;
                    int drawY = (piece.y + row) * BLOCK_SIZE;
                    drawBlock(g2d, drawX, drawY, piece.color, isGhost);
                }
            }
        }
    }

    private void drawPiece(Graphics2D g2d, Piece piece, int startX, int startY, boolean isGhost) {
         for (int row = 0; row < piece.shape.length; row++) {
            for (int col = 0; col < piece.shape[row].length; col++) {
                if (piece.shape[row][col] == 1) {
                    drawBlock(g2d, startX + col * BLOCK_SIZE, startY + row * BLOCK_SIZE, piece.color, isGhost);
                }
            }
        }
    }

    private void drawBlock(Graphics2D g2d, int x, int y, Color color, boolean isGhost) {
        if (isGhost) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g2d.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
        } else {
            g2d.setColor(color);
            g2d.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
            g2d.setColor(color.darker().darker());
            g2d.drawRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameState == GameState.PLAYING) {
            if (key == KeyEvent.VK_LEFT) {
                currentPiece.x--;
                if (collides()) currentPiece.x++;
                else updateGhostPiece();
            } else if (key == KeyEvent.VK_RIGHT) {
                currentPiece.x++;
                if (collides()) currentPiece.x--;
                else updateGhostPiece();
            } else if (key == KeyEvent.VK_DOWN) {
                movePieceDown();
            } else if (key == KeyEvent.VK_UP) {
                rotatePiece();
            } else if (key == KeyEvent.VK_SPACE) {
                hardDrop();
            } else if (key == KeyEvent.VK_C) {
                hold();
            } else if (key == KeyEvent.VK_P) {
                gameState = GameState.PAUSED;
            }
        } else if (gameState == GameState.PAUSED) {
            if (key == KeyEvent.VK_P) {
                gameState = GameState.PLAYING;
            }
        } else if (gameState == GameState.MENU || gameState == GameState.GAME_OVER) {
            if (key == KeyEvent.VK_ENTER) {
                startGame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    private void loadHighScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGHSCORE_FILE))) {
            highScore = Integer.parseInt(reader.readLine());
        } catch (IOException | NumberFormatException e) {
            highScore = 0; // Default to 0 if file not found or invalid
        }
    }

    private void saveHighScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGHSCORE_FILE))) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            e.printStackTrace(); // Could show an error message to the user
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Create a container panel to center the game panel
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(Color.DARK_GRAY);
        
        // Add the game panel to the container
        container.add(new Tetris());
        
        frame.add(container);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static class Piece {
        private int[][] shape;
        private Color color;
        private int x, y;

        public Piece(int[][] shape, Color color) {
            this.shape = shape;
            this.color = color;
        }
    }
}
