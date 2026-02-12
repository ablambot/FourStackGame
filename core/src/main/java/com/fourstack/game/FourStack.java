package com.fourstack.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FourStack extends ApplicationAdapter {

    int ROWS = 6;
    int COLS = 7;

    int[][] grid = new int[ROWS][COLS];
    // 0 = empty, 1 = yellow (player), 2 = red (AI)

    int currentPlayer = 1; // 1 = player (yellow), 2 = AI (red)

    SpriteBatch batch;
    Texture board;
    Texture frame;
    Texture yellowPiece;
    Texture redPiece;
    BitmapFont font;

    // Game state
    enum GameState { PLAYING, PLAYER_WIN, AI_WIN, TIME_UP }
    GameState gameState = GameState.PLAYING;

    // Timer (2 minutes = 120 seconds)
    float timeRemaining = 120f;

    // AI
    Random random = new Random();
    float aiMoveDelay = 0.5f; // AI waits 0.5s before moving
    float aiTimer = 0f;
    boolean aiNeedsToMove = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        board = new Texture("board.png");
        frame = new Texture("frame.png");
        yellowPiece = new Texture("piece_yellow.png");
        redPiece = new Texture("piece_red.png");
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Update timer
        if (gameState == GameState.PLAYING) {
            timeRemaining -= deltaTime;
            if (timeRemaining <= 0) {
                timeRemaining = 0;
                gameState = GameState.TIME_UP;
            }
        }

        // AI move logic
        if (gameState == GameState.PLAYING && aiNeedsToMove) {
            aiTimer += deltaTime;
            if (aiTimer >= aiMoveDelay) {
                makeAIMove();
                aiTimer = 0f;
                aiNeedsToMove = false;
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float scale = 2.5f;

        float frameWidth = frame.getWidth() * scale;
        float frameHeight = frame.getHeight() * scale;

        // Center the frame on screen
        float frameX = (Gdx.graphics.getWidth() - frameWidth) / 2f;
        float frameY = (Gdx.graphics.getHeight() - frameHeight) / 2f;

        // Frame inner hole: (62,62) to (176,161) in the 240x256 image
        float framePadLeft = frameWidth * 0.2583f;
        float framePadRight = frameWidth * 0.2667f;
        float framePadTop = frameHeight * 0.2422f;
        float framePadBottom = frameHeight * 0.3711f;

        // The frame inner opening in screen coords
        float innerX = frameX + framePadLeft;
        float innerY = frameY + framePadBottom;
        float innerW = frameWidth - framePadLeft - framePadRight;
        float innerH = frameHeight - framePadTop - framePadBottom;

        // Board grid occupies (63,63) to (175,159) inside board.png (240x256)
        // Grid size in board.png: 112x96 px
        // Board.png total: 240x256
        // We scale board.png so its GRID area matches the frame INNER HOLE
        float gridToTotalScaleX = board.getWidth() / 112f; // 240/112 = 2.143
        float gridToTotalScaleY = board.getHeight() / 96f; // 256/96 = 2.667

        // Board drawn size = inner opening * grid-to-total ratio
        float boardDrawW = innerW * gridToTotalScaleX;
        float boardDrawH = innerH * gridToTotalScaleY;

        // Offset the board so its grid aligns with the inner hole
        // Grid starts 63px from left and 97px from bottom in the 240x256 image
        float gridOffsetX = boardDrawW * (63f / 240f); // left border fraction
        float gridOffsetY = boardDrawH * (97f / 256f); // bottom border fraction

        float boardDrawX = innerX - gridOffsetX;
        float boardDrawY = innerY - gridOffsetY;

        // Grid (piece placement) coords = exactly the frame inner hole
        float boardX = innerX;
        float boardY = innerY;
        float boardWidth = innerW;
        float boardHeight = innerH;

        batch.begin();

        // 1. Draw board — scaled so grid fills the frame opening exactly
        batch.draw(board, boardDrawX, boardDrawY, boardDrawW, boardDrawH);

        // 2. Handle player input (only if it's player's turn and game is playing)
        if (gameState == GameState.PLAYING && currentPlayer == 1 && Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (mouseX >= boardX && mouseX <= boardX + boardWidth &&
                    mouseY >= boardY && mouseY <= boardY + boardHeight) {

                float cellWidth = boardWidth / COLS;
                int col = (int) ((mouseX - boardX) / cellWidth);
                
                if (dropPiece(col, currentPlayer)) {
                    // Check for vanish-cascade first
                    checkAndRemoveLines();
                    
                    // Then check for diagonal win
                    if (checkDiagonalWin(1)) {
                        gameState = GameState.PLAYER_WIN;
                    } else {
                        // Switch to AI turn
                        currentPlayer = 2;
                        aiNeedsToMove = true;
                        aiTimer = 0f;
                    }
                }
            }
        }

        // 3. Draw pieces
        float cellWidth = boardWidth / COLS;
        float cellHeight = boardHeight / ROWS;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != 0) {
                    float size = Math.min(cellWidth, cellHeight) * 1.0f;

                    float pieceX = boardX + c * cellWidth + (cellWidth - size) / 2f;
                    float pieceY = boardY + (ROWS - 1 - r) * cellHeight + (cellHeight - size) / 2f;

                    Texture piece = (grid[r][c] == 1) ? yellowPiece : redPiece;
                    batch.draw(piece, pieceX, pieceY, size, size);
                }
            }
        }

        // 4. Draw frame LAST so it appears on top of board and pieces
        batch.draw(frame, frameX, frameY, frameWidth, frameHeight);

        // 5. Draw UI (timer and game state)
        int minutes = (int) (timeRemaining / 60);
        int seconds = (int) (timeRemaining % 60);
        String timerText = String.format("Time: %d:%02d", minutes, seconds);
        font.draw(batch, timerText, 20, Gdx.graphics.getHeight() - 20);

        String turnText = currentPlayer == 1 ? "Your Turn (Yellow)" : "AI Turn (Red)";
        if (gameState == GameState.PLAYING) {
            font.draw(batch, turnText, 20, Gdx.graphics.getHeight() - 60);
        } else if (gameState == GameState.PLAYER_WIN) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "YOU WIN! Diagonal Four-Stack!", 
                Gdx.graphics.getWidth() / 2 - 200, Gdx.graphics.getHeight() / 2);
        } else if (gameState == GameState.AI_WIN) {
            font.setColor(Color.RED);
            font.draw(batch, "AI WINS! Diagonal Four-Stack!", 
                Gdx.graphics.getWidth() / 2 - 200, Gdx.graphics.getHeight() / 2);
        } else if (gameState == GameState.TIME_UP) {
            font.setColor(Color.RED);
            font.draw(batch, "TIME'S UP! You Lose!", 
                Gdx.graphics.getWidth() / 2 - 150, Gdx.graphics.getHeight() / 2);
        }
        font.setColor(Color.WHITE);

        batch.end();
    }

    boolean dropPiece(int col, int player) {
        if (col < 0 || col >= COLS) return false;
        
        for (int row = ROWS - 1; row >= 0; row--) {
            if (grid[row][col] == 0) {
                grid[row][col] = player;
                return true;
            }
        }
        return false; // Column is full
    }

    void makeAIMove() {
        // Simple AI: try to block player's diagonal or make random move
        List<Integer> validCols = new ArrayList<>();
        for (int c = 0; c < COLS; c++) {
            if (grid[0][c] == 0) { // Top row empty = column not full
                validCols.add(c);
            }
        }

        if (validCols.isEmpty()) return;

        // Pick random valid column
        int col = validCols.get(random.nextInt(validCols.size()));
        dropPiece(col, 2);

        // Check for vanish-cascade
        checkAndRemoveLines();

        // Check if AI won
        if (checkDiagonalWin(2)) {
            gameState = GameState.AI_WIN;
        } else {
            // Switch back to player
            currentPlayer = 1;
        }
    }

    void checkAndRemoveLines() {
        boolean foundLine = false;

        // Check horizontal lines
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r][c+1] &&
                    grid[r][c] == grid[r][c+2] &&
                    grid[r][c] == grid[r][c+3]) {
                    // Remove this line
                    grid[r][c] = 0;
                    grid[r][c+1] = 0;
                    grid[r][c+2] = 0;
                    grid[r][c+3] = 0;
                    foundLine = true;
                }
            }
        }

        // Check vertical lines
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r+1][c] &&
                    grid[r][c] == grid[r+2][c] &&
                    grid[r][c] == grid[r+3][c]) {
                    // Remove this line
                    grid[r][c] = 0;
                    grid[r+1][c] = 0;
                    grid[r+2][c] = 0;
                    grid[r+3][c] = 0;
                    foundLine = true;
                }
            }
        }

        // If any lines were removed, apply gravity cascade
        if (foundLine) {
            applyGravity();
            // Recursively check again in case new lines formed
            checkAndRemoveLines();
        }
    }

    void applyGravity() {
        // Drop pieces down to fill empty spaces
        for (int c = 0; c < COLS; c++) {
            // Collect all non-empty pieces in this column
            List<Integer> pieces = new ArrayList<>();
            for (int r = ROWS - 1; r >= 0; r--) {
                if (grid[r][c] != 0) {
                    pieces.add(grid[r][c]);
                }
            }

            // Clear column
            for (int r = 0; r < ROWS; r++) {
                grid[r][c] = 0;
            }

            // Place pieces back from bottom
            for (int i = 0; i < pieces.size(); i++) {
                grid[ROWS - 1 - i][c] = pieces.get(i);
            }
        }
    }

    boolean checkDiagonalWin(int player) {
        // Check all diagonal directions (/ and \)
        
        // Check \ diagonals (top-left to bottom-right)
        for (int r = 0; r <= ROWS - 4; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player &&
                    grid[r+1][c+1] == player &&
                    grid[r+2][c+2] == player &&
                    grid[r+3][c+3] == player) {
                    return true;
                }
            }
        }

        // Check / diagonals (bottom-left to top-right)
        for (int r = 3; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] == player &&
                    grid[r-1][c+1] == player &&
                    grid[r-2][c+2] == player &&
                    grid[r-3][c+3] == player) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void dispose() {
        batch.dispose();
        board.dispose();
        frame.dispose();
        yellowPiece.dispose();
        redPiece.dispose();
        font.dispose();
    }
}