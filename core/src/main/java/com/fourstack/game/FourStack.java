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

class FallingPiece {
    float x, y, targetY, speed;
    int player;
    int col, row;
    // Keep track of the last few Y positions for the blur effect
    float[] previousY = new float[5]; 

    FallingPiece(float x, float y, float targetY, int player, int col, int row) {
        this.x = x;
        this.y = y;
        this.targetY = targetY;
        this.player = player;
        this.speed = 1500f; // Crank this up for a real "speed" feel!
        this.col = col;
        this.row = row;
        // Initialize trail
        for(int i = 0; i < previousY.length; i++) previousY[i] = y;
    }

    void update(float deltaTime) {
        // Shift old positions down the array
        for (int i = previousY.length - 1; i > 0; i--) {
            previousY[i] = previousY[i - 1];
        }
        previousY[0] = y; // Current becomes the new "previous"
        y -= speed * deltaTime;
    }
}

public class FourStack extends ApplicationAdapter {
        
List<FallingPiece> activeFallingPieces = new ArrayList<>();

    float innerX, innerY, innerW, innerH;
    float cellWidth, cellHeight;
    enum Difficulty { EASY, MEDIUM, HARD }
    Difficulty currentDifficulty = Difficulty.MEDIUM; // Default
    int scoreGoal = 2000; // The "High Score" to reach
    int score = 0;       // Player score
    int aiScore = 0;     // AI score
    int ROWS = 6;
    int COLS = 7;
    int comboMultiplier = 0;

    

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
        
        // 1. Handle Key Inputs (Difficulty & Reset)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_1)) { currentDifficulty = Difficulty.EASY; scoreGoal = 2000; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_2)) { currentDifficulty = Difficulty.MEDIUM; scoreGoal = 5000; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_3)) { currentDifficulty = Difficulty.HARD; scoreGoal = 10000; }
        
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
            grid = new int[ROWS][COLS];
            gameState = GameState.PLAYING;
            timeRemaining = 120f;
            currentPlayer = 1; 
            aiNeedsToMove = false;
            aiTimer = 0f;
            aiScore = 0;
            score = 0;
            comboMultiplier = 0;
        }

        // 2. COORDINATE MATH (Must be before Input and Drawing)
        float scale = 2.5f;
        float frameWidth = frame.getWidth() * scale;
        float frameHeight = frame.getHeight() * scale;
        float frameX = (Gdx.graphics.getWidth() - frameWidth) / 2f;
        float frameY = (Gdx.graphics.getHeight() - frameHeight) / 2f;

        float framePadLeft = frameWidth * 0.2583f;
        float framePadRight = frameWidth * 0.2667f;
        float framePadTop = frameHeight * 0.2422f;
        float framePadBottom = frameHeight * 0.3711f;

        innerX = frameX + framePadLeft;
        innerY = frameY + framePadBottom;
        innerW = frameWidth - framePadLeft - framePadRight;
        innerH = frameHeight - framePadTop - framePadBottom;

        cellWidth = innerW / COLS;
        cellHeight = innerH / ROWS;

        // 3. Update Game Logic & Input
        if (gameState == GameState.PLAYING) {
            timeRemaining -= deltaTime;
            if (timeRemaining <= 0) {
                timeRemaining = 0;
                gameState = GameState.TIME_UP;
            }

            // PLAYER CLICK DETECTION
            if (currentPlayer == 1 && Gdx.input.justTouched()) {
                float mouseX = Gdx.input.getX();
                // Check if within horizontal board bounds
                if (mouseX >= innerX && mouseX <= innerX + innerW) {
                    int col = (int) ((mouseX - innerX) / cellWidth);
                    executeMove(col); 
                }
            }

            // AI MOVE LOGIC
            if (aiNeedsToMove) {
                aiTimer += deltaTime;
                if (aiTimer >= aiMoveDelay) {
                    makeAIMove();
                    aiTimer = 0f;
                    aiNeedsToMove = false;
                }
            }
        }

        // 4. DRAWING SECTION
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // --- LAYER 1: BACK (The very back background) ---
        // If you have a separate background image, draw it here.

        // --- LAYER 2: PIECES (Falling and Dropped) ---
       // Draw Falling Animation
        for (int i = activeFallingPieces.size() - 1; i >= 0; i--) {
            FallingPiece p = activeFallingPieces.get(i);
            p.update(deltaTime);
            
            // Use 0.99f to match stationary pieces
            float size = Math.min(cellWidth, cellHeight) * 0.99f; 
            Texture pieceTex = (p.player == 1 ? yellowPiece : redPiece);

            // Speed Blur Trail
            for (int j = 0; j < p.previousY.length; j++) {
                float alpha = (j < 2) ? 1.0f : 1.0f - (j * 0.2f); 
                if (alpha < 0) alpha = 0;
                
                if (j % 2 == 0) batch.setColor(1, 1, 1, alpha);
                else batch.setColor(0.8f, 0.8f, 0.8f, alpha);

                // Center the trail on p.x (which already includes the +2.2f offset)
                float trailWidth = size * 0.8f;
                float trailX = p.x + (size - trailWidth) / 2f; 

                batch.draw(pieceTex, trailX, p.previousY[j], trailWidth, size + (j * 10f));
            }

            // Draw the main falling piece
            batch.setColor(Color.WHITE); 
            // p.x now contains the 2.2f offset, so no extra math needed here
            batch.draw(pieceTex, p.x, p.y, size, size); 

            if (p.y <= p.targetY) {
                grid[p.row][p.col] = p.player;
                activeFallingPieces.remove(i);
                finalizeTurn(p.col, p.player); 
            }
        }

        // Draw Stationary Pieces in the Grid
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] != 0) {
                    float size = Math.min(cellWidth, cellHeight) * 0.99f;
                    float pieceX = innerX + c * cellWidth + (cellWidth - size) / 1f + 2.2f;
                    float pieceY = innerY + (ROWS - 1 - r) * cellHeight + (cellHeight - size) / 1f - 3f;
                    Texture piece = (grid[r][c] == 1) ? yellowPiece : redPiece;
                    batch.draw(piece, pieceX, pieceY, size, size);
                }
            }
        }

        // --- LAYER 3: THE BOARD (The Mask with Holes) ---
        // By drawing the board NOW, it covers the pieces. 
        // For this to work, your "board.png" MUST have transparent holes!
        float boardDrawW = innerW * (board.getWidth() / 112f);
        float boardDrawH = innerH * (board.getHeight() / 96f);
        float boardDrawX = innerX - (boardDrawW * (63f / 240f));
        float boardDrawY = innerY - (boardDrawH * (97f / 256f));
        batch.draw(board, boardDrawX, boardDrawY, boardDrawW, boardDrawH);

        // --- LAYER 4: THE FRAME (The very front) ---
        batch.draw(frame, frameX, frameY, frameWidth, frameHeight);

        // --- LAYER 5: PREVIEW & UI ---
        // (Keep your Preview Piece and UI logic here)
                if (gameState == GameState.PLAYING && currentPlayer == 1) {
                    float mouseX = Gdx.input.getX();
                    if (mouseX >= innerX && mouseX <= innerX + innerW) {
                        int hoverCol = (int) ((mouseX - innerX) / cellWidth);
                        
                        float size = Math.min(cellWidth, cellHeight) * 0.99f;
                        float previewX = innerX + hoverCol * cellWidth + (cellWidth - size) / 1f + 2f;
                        float previewY = innerY + ROWS * cellHeight + 10; 

                        if (grid[0][hoverCol] == 0) {
                            batch.setColor(1, 1, 1, 0.5f);
                        } else {
                            batch.setColor(1, 0, 0, 0.7f); // Red if column full
                        }

                        batch.draw(yellowPiece, previewX, previewY, size, size);
                        batch.setColor(Color.WHITE); 
                    }
                }
                
        // 5. UI
        updateAndDrawUI();
        
        int minutes = (int) (timeRemaining / 60);
        int seconds = (int) (timeRemaining % 60);
        font.draw(batch, String.format("Time: %d:%02d", minutes, seconds), 20, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "DIFFICULTY: " + currentDifficulty, 20, 60);
        font.draw(batch, "GOAL: " + scoreGoal, Gdx.graphics.getWidth() - 280, Gdx.graphics.getHeight() - 20);

        if (gameState == GameState.PLAYING) {
            font.draw(batch, currentPlayer == 1 ? "Your Turn" : "AI Thinking...", 20, Gdx.graphics.getHeight() - 60);
        } else {
            displayEndGameMessage();
        }

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


    void displayEndGameMessage() {
    if (gameState == GameState.PLAYER_WIN) {
        font.setColor(Color.YELLOW);
        font.draw(batch, "YOU WIN!", Gdx.graphics.getWidth()/2 - 100, Gdx.graphics.getHeight()/2);
    } else if (gameState == GameState.AI_WIN) {
        font.setColor(Color.RED);
        font.draw(batch, "AI WINS!", Gdx.graphics.getWidth()/2 - 100, Gdx.graphics.getHeight()/2);
    } else if (gameState == GameState.TIME_UP) {
        font.setColor(Color.RED);
        font.draw(batch, "TIME'S UP!", Gdx.graphics.getWidth()/2 - 100, Gdx.graphics.getHeight()/2);
    }
    font.setColor(Color.WHITE);
    }

    void makeAIMove() {
        int chosenCol = -1;

        if (currentDifficulty == Difficulty.HARD) {
            // 1. Can I win right now?
            chosenCol = findWinningMove(2); 
            // 2. If not, is the player about to win? Block them!
            if (chosenCol == -1) chosenCol = findWinningMove(1);
        } 
        
        if (currentDifficulty == Difficulty.MEDIUM && chosenCol == -1) {
            // Medium AI only blocks the player 50% of the time
            if (random.nextFloat() > 0.5f) chosenCol = findWinningMove(1);
        }

        // Easy (or if no smart move found), just pick at random
        if (chosenCol == -1) {
            List<Integer> validCols = new ArrayList<>();
            for (int c = 0; c < COLS; c++) {
                if (grid[0][c] == 0) validCols.add(c);
            }
            if (!validCols.isEmpty()) {
                chosenCol = validCols.get(random.nextInt(validCols.size()));
            }
        }

        if (chosenCol != -1) {
            executeMove(chosenCol);
        }
    }

    // Helper to check if placing a piece in a column results in 4-in-a-row
    int findWinningMove(int player) {
        for (int c = 0; c < COLS; c++) {
            // Find where the piece would land
            int targetRow = -1;
            for (int r = ROWS - 1; r >= 0; r--) {
                if (grid[r][c] == 0) {
                    targetRow = r;
                    break;
                }
            }
            
            if (targetRow != -1) {
                grid[targetRow][c] = player; // Temporarily place
                boolean wins = checkWin(player);
                grid[targetRow][c] = 0; // Undo
                if (wins) return c;
            }
        }
        return -1;
    }

    boolean checkAndRemoveLines() {
        boolean foundLine = false;

        // Horizontal Check
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r][c+1] &&
                    grid[r][c] == grid[r][c+2] &&
                    grid[r][c] == grid[r][c+3]) {
                    
                    grid[r][c] = 0; grid[r][c+1] = 0; grid[r][c+2] = 0; grid[r][c+3] = 0;
                    foundLine = true;
                }
            }
        }

        // Vertical Check
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r+1][c] &&
                    grid[r][c] == grid[r+2][c] &&
                    grid[r][c] == grid[r+3][c]) {
                    
                    grid[r][c] = 0; grid[r+1][c] = 0; grid[r+2][c] = 0; grid[r+3][c] = 0;
                    foundLine = true;
                }
            }
        }
        // 3. Diagonal Up-Left (/)
        for (int r = 0; r <= ROWS - 4; r++) {
                for (int c = 0; c <= COLS - 4; c++) {
                    if (grid[r][c] != 0 &&
                        grid[r][c] == grid[r+1][c+1] &&
                        grid[r][c] == grid[r+2][c+2] &&
                        grid[r][c] == grid[r+3][c+3]) {
                        
                        grid[r][c] = 0; grid[r+1][c+1] = 0; 
                        grid[r+2][c+2] = 0; grid[r+3][c+3] = 0;
                        foundLine = true;
                    }
                }
            }

            // 4. Diagonal Up-Right (/)
            for (int r = 3; r < ROWS; r++) {
                for (int c = 0; c <= COLS - 4; c++) {
                    if (grid[r][c] != 0 &&
                        grid[r][c] == grid[r-1][c+1] &&
                        grid[r][c] == grid[r-2][c+2] &&
                        grid[r][c] == grid[r-3][c+3]) {
                        
                        grid[r][c] = 0; grid[r-1][c+1] = 0; 
                        grid[r-2][c+2] = 0; grid[r-3][c+3] = 0;
                        foundLine = true;
                    }
                }
            }

            if (foundLine) {
                float bonus = (score < 5000) ? 8f : 3f;
                timeRemaining += bonus;
            }
            
            return foundLine;
    }

    void updateAndDrawUI() {
        // Player Score (Left)
        font.setColor(Color.YELLOW);
        font.draw(batch, "PLAYER: " + score, 20, Gdx.graphics.getHeight() - 100);

        // AI Score (Right)
        font.setColor(Color.RED);
        font.draw(batch, "AI: " + aiScore, Gdx.graphics.getWidth() - 250, Gdx.graphics.getHeight() - 140);

        // Draw Combo if active
        if (comboMultiplier > 1) {
            font.setColor(Color.GOLD);
            font.draw(batch, "COMBO X" + comboMultiplier, 20, Gdx.graphics.getHeight() - 140);
        }

        font.setColor(Color.WHITE);
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

    // --- ADD THIS AT THE BOTTOM OF YOUR CLASS ---

    void bubbleSortColumn(int col) {
        // We run multiple passes to ensure a piece at the very top 
        // can "fall" all the way to the bottom if it's empty.
        for (int pass = 0; pass < ROWS; pass++) {
            for (int r = 0; r < ROWS - 1; r++) {
                // If current spot has a piece (1 or 2) AND the spot below (r+1) is empty (0)
                if (grid[r][col] != 0 && grid[r + 1][col] == 0) {
                    // Swap them so the piece "falls" into the empty space
                    int temp = grid[r][col];
                    grid[r][col] = grid[r + 1][col];
                    grid[r + 1][col] = temp;
                }
            }
        }
    }

void finalizeTurn(int col, int playerID) {
    boolean linesCleared = false;
    while (checkAndRemoveLines()) {
        linesCleared = true;
        comboMultiplier++;
        int points = (100 * comboMultiplier);
        if (playerID == 1) score += points;
        else aiScore += points;
        for(int c = 0; c < COLS; c++) bubbleSortColumn(c);
    }
    if (!linesCleared) comboMultiplier = 0;

    // Check Win/Loss
    if (score >= scoreGoal) gameState = GameState.PLAYER_WIN;
    else if (aiScore >= scoreGoal) gameState = GameState.AI_WIN;
    else if (grid[0][col] != 0) gameState = (playerID == 1) ? GameState.AI_WIN : GameState.PLAYER_WIN;
    else {
        // Switch turns
        currentPlayer = (playerID == 1) ? 2 : 1;
        if (currentPlayer == 2) {
            aiNeedsToMove = true;
            aiTimer = 0f;
        }
    }
}

void executeMove(int col) {
    if (gameState != GameState.PLAYING || currentPlayer == 0) return;

    int targetRow = -1;
    for (int r = ROWS - 1; r >= 0; r--) {
        if (grid[r][col] == 0) {
            targetRow = r;
            break;
        }
    }

    if (targetRow != -1) {
        float size = Math.min(cellWidth, cellHeight) * 0.99f;
        float startX = innerX + col * cellWidth + (cellWidth - size) / 1f + 2.2f;
        float startY = Gdx.graphics.getHeight(); 
        float targetY = innerY + (ROWS - 1 - targetRow) * cellHeight + (cellHeight - size) / 1f - 3f;

        activeFallingPieces.add(new FallingPiece(startX, startY, targetY, currentPlayer, col, targetRow));
        currentPlayer = 0; 
    }
}

    boolean checkWin(int player) {

        // Directions: right, down, diag-down-right, diag-up-right
        int[][] directions = {
            {0, 1},   // horizontal
            {1, 0},   // vertical
            {1, 1},   // diagonal \
            {-1, 1}   // diagonal /
        };

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {

                if (grid[r][c] != player) continue;

                // Check all directions
                for (int[] dir : directions) {

                    int count = 1;
                    int dr = dir[0];
                    int dc = dir[1];

                    int nr = r + dr;
                    int nc = c + dc;

                    while (nr >= 0 && nr < ROWS &&
                        nc >= 0 && nc < COLS &&
                        grid[nr][nc] == player) {

                        count++;

                        if (count == 4)
                            return true;

                        nr += dr;
                        nc += dc;
                    }
                }
            }
        }

        return false;
    }
}