package com.fourstack.game;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
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

class BlastEffect {
    float x, y, size, alpha, duration;

    BlastEffect(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.alpha = 1f;
        this.duration = 0.5f; // Animation lasts 0.5 seconds
    }

    void update(float deltaTime) {
        duration -= deltaTime;
        alpha = duration * 2f; // Fade out
        size *= 1 + (deltaTime * 2f); // Expand
    }

    boolean isFinished() {
        return duration <= 0;
    }
}

public class FourStack extends ApplicationAdapter {
        
        // Add these constants
    public static final float VIRTUAL_WIDTH = 1440;
    public static final float VIRTUAL_HEIGHT = 762;
    private com.badlogic.gdx.utils.viewport.Viewport viewport;

List<FallingPiece> activeFallingPieces = new ArrayList<>();
List<BlastEffect> blastEffects = new ArrayList<>();

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
    Texture background;
    Texture yellowPiece;
    Texture redPiece;
    // Texture blastTexture;
    BitmapFont font;

    // Sound effects
    Music backgroundMusic;
    Sound popSound;
    Sound winSound;
    Sound loseSound;
    // Sound blastSound; (Optional if needed later)

    // UI elements
    Stage stage;
    Skin skin;
    Table introTable;
    Table gameTable;
    Table messageTable;
    Label playerLabel;
    Label aiLabel;
    Label timeLabel;
    Label difficultyLabel;
    Label goalLabel;
    Label statusLabel;
    Label comboLabel;
    Label messageLabel;
    Label instructionsLabel;
    TextButton retryButton;

    // Game state
    enum GameState { INTRO, PLAYING, PLAYER_WIN, AI_WIN, TIME_UP }
    GameState gameState = GameState.INTRO;

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
        background = new Texture("border.png");
        board = new Texture("board.png");
        frame = new Texture("frame.png");
        yellowPiece = new Texture("piece_yellow.png");
        redPiece = new Texture("piece_red.png");
        // blastTexture = new Texture("blast.png");
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);

        // Load and play background music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("backround_audio.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        // Load sound effects
        popSound = Gdx.audio.newSound(Gdx.files.internal("pop_sound.mp3"));
        winSound = Gdx.audio.newSound(Gdx.files.internal("win_sound.wav"));
        loseSound = Gdx.audio.newSound(Gdx.files.internal("Lose_sound.wav"));

        // UI SETUP
        // Change ScreenViewport to FitViewport
        viewport = new com.badlogic.gdx.utils.viewport.FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage = new Stage(viewport); // The stage now uses the virtual resolution
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        createIntroUI();
        createGameUI();

        // Start with Intro
        introTable.setVisible(true);
        gameTable.setVisible(false);
    }

    private void createIntroUI() {
        introTable = new Table();
        introTable.setFillParent(true);
        introTable.setBackground(skin.getDrawable("alpha"));
        stage.addActor(introTable);

        Label titleLabel = new Label("FOUR STACK", skin, "subtitle");
        titleLabel.setFontScale(2f);
        titleLabel.setColor(Color.GOLD);

        Label difficultyTitle = new Label("SELECT DIFFICULTY", skin);
        final SelectBox<Difficulty> difficultySelect = new SelectBox<>(skin);
        difficultySelect.setItems(Difficulty.values());
        difficultySelect.setSelected(currentDifficulty);

        TextButton startButton = new TextButton("START GAME", skin);
        TextButton tutorialButton = new TextButton("TUTORIAL", skin);
        TextButton settingsButton = new TextButton("SETTINGS", skin);

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentDifficulty = difficultySelect.getSelected();
                if (currentDifficulty == Difficulty.EASY) scoreGoal = 2000;
                else if (currentDifficulty == Difficulty.MEDIUM) scoreGoal = 5000;
                else if (currentDifficulty == Difficulty.HARD) scoreGoal = 10000;
                
                startGame();
            }
        });

        introTable.add(titleLabel).padBottom(50).row();
        introTable.add(difficultyTitle).padBottom(10).row();
        introTable.add(difficultySelect).width(200).padBottom(30).row();
        introTable.add(startButton).width(250).height(60).padBottom(15).row();
        introTable.add(tutorialButton).width(200).height(50).padBottom(15).row();
        introTable.add(settingsButton).width(200).height(50);
    }

private void createGameUI() {
    gameTable = new Table();
    gameTable.setFillParent(true);
    stage.addActor(gameTable);

    // 1. INITIALIZE LABELS
    timeLabel = new Label("", skin);
    goalLabel = new Label("" , skin);
    playerLabel = new Label("", skin);
    aiLabel = new Label("", skin);
    difficultyLabel = new Label("", skin);
    statusLabel = new Label("", skin);
    comboLabel = new Label("", skin);

    // 2. SCALE / ENLARGE (Change these numbers to go bigger/smaller)
    timeLabel.setFontScale(3.4f);
    goalLabel.setFontScale(3.4f);
    difficultyLabel.setFontScale(3.75f);
    playerLabel.setFontScale(2.9f);
    aiLabel.setFontScale(2.9f);
    statusLabel.setFontScale(1.5f);
    comboLabel.setFontScale(2.9f);

    // 3. SET COLORS
    playerLabel.setColor(Color.YELLOW);
    aiLabel.setColor(Color.RED);
    comboLabel.setColor(Color.GOLD);
    difficultyLabel.setColor(Color.CYAN);

    // 4. POSITIONING (Separate Entities - Adjust X and Y here)
    // Note: 0,0 is Bottom-Left of the screen
    timeLabel.setPosition(1243, 653);
    goalLabel.setPosition(863, 653);
    
    difficultyLabel.setPosition(320, 28);
    
    statusLabel.setPosition(VIRTUAL_WIDTH / 2f - 50, VIRTUAL_HEIGHT - 50);
    
    playerLabel.setPosition(845, 577); // Moved to the right side area
    aiLabel.setPosition(845, 527);     // Stacked below player score
    
    comboLabel.setPosition(VIRTUAL_WIDTH / 2f - 100, 100);

    // 5. ADD DIRECTLY TO STAGE (Not the table!)
    stage.addActor(timeLabel);
    stage.addActor(goalLabel);
    stage.addActor(difficultyLabel);
    stage.addActor(statusLabel);
    stage.addActor(playerLabel);
    stage.addActor(aiLabel);
    stage.addActor(comboLabel);

    // 6. MESSAGE OVERLAY (Keep this as a table so it centers easily)
    messageLabel = new Label("", skin, "subtitle");
    messageLabel.setAlignment(Align.center);
    
    retryButton = new TextButton("RETRY", skin);
    retryButton.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            messageTable.setVisible(false);
            introTable.setVisible(true);
            gameTable.setVisible(false);
            gameState = GameState.INTRO;
        }
    });

    messageTable = new Table();
    messageTable.setFillParent(true);
    messageTable.add(messageLabel).padBottom(30).row();
    messageTable.add(retryButton).width(200).height(60);
    messageTable.setVisible(false);
    stage.addActor(messageTable);
}

    private void startGame() {
        grid = new int[ROWS][COLS];
        gameState = GameState.PLAYING;
        timeRemaining = 120f;
        currentPlayer = 1; 
        aiNeedsToMove = false;
        aiTimer = 0f;
        aiScore = 0;
        score = 0;
        comboMultiplier = 0;
        introTable.setVisible(false);
        gameTable.setVisible(true);
        messageTable.setVisible(false);
    }

    @Override
    public void resize(int width, int height) {
        // This maintains the aspect ratio automatically
        viewport.update(width, height, true); 
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

        // 2. COORDINATE MATH (Shifted to the left half)
        float scale = 2.5f;
        float frameWidth = frame.getWidth() * scale;
        float frameHeight = frame.getHeight() * scale;

        // Change frameX to align with the left side instead of center
        // We use a small padding (e.g., 20) or just 0 to stick it to the far left
        float frameX = 20f; 
        float frameY = (VIRTUAL_HEIGHT - frameHeight) / 2f;

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
        if (gameState == GameState.INTRO) {
            // Nothing special here, Scene2D handles the intro UI
        } else if (gameState == GameState.PLAYING) {
            timeRemaining -= deltaTime;
            if (timeRemaining <= 0) {
                timeRemaining = 0;
                gameState = GameState.TIME_UP;
                loseSound.play();
            }

            // PLAYER CLICK DETECTION
            if (currentPlayer == 1 && Gdx.input.justTouched()) {
                // 1. Create a temporary vector to hold the touch coordinates
                com.badlogic.gdx.math.Vector2 touch = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
                
                // 2. Translate the "Screen" click to our "Virtual" 1920x1016 world
                viewport.unproject(touch);
                
                // 3. Use touch.x instead of mouseX
                if (touch.x >= innerX && touch.x <= innerX + innerW) {
                    int col = (int) ((touch.x - innerX) / cellWidth);
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

    viewport.apply();
    batch.setProjectionMatrix(viewport.getCamera().combined);

            batch.begin();

        // --- LAYER 1: BACK (The very back background) ---
        // --- LAYER 1: BACK ---
    // This draws your "border.png" to fill the whole screen
    batch.draw(background, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // --- LAYER 2: PIECES (Falling and Dropped) ---
        if (gameState != GameState.INTRO) {
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
                    popSound.play();
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
        }

        // --- LAYER 3: BLAST EFFECTS ---
        /*
        for (int i = blastEffects.size() - 1; i >= 0; i--) {
            BlastEffect be = blastEffects.get(i);
            be.update(deltaTime);
            if (be.isFinished()) {
                blastEffects.remove(i);
            } else {
                batch.setColor(1, 1, 1, be.alpha);
                batch.draw(blastTexture, be.x - be.size / 2, be.y - be.size / 2, be.size, be.size);
                batch.setColor(Color.WHITE);
            }
        }
        */

        // --- LAYER 4: THE BOARD (The Mask with Holes) ---
        if (gameState != GameState.INTRO) {
            float boardDrawW = innerW * (board.getWidth() / 112f);
            float boardDrawH = innerH * (board.getHeight() / 96f);
            float boardDrawX = innerX - (boardDrawW * (63f / 240f));
            float boardDrawY = innerY - (boardDrawH * (97f / 256f));
            batch.draw(board, boardDrawX, boardDrawY, boardDrawW, boardDrawH);

            // --- LAYER 4: THE FRAME (The very front) ---
            batch.draw(frame, frameX, frameY, frameWidth, frameHeight);
        }

        // --- LAYER 5: PREVIEW & UI ---
        // (Keep your Preview Piece and UI logic here)
    if (gameState == GameState.PLAYING && currentPlayer == 1) {
        // Translate mouse to virtual coordinates
        com.badlogic.gdx.math.Vector2 mouse = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);

        if (mouse.x >= innerX && mouse.x <= innerX + innerW) {
            int hoverCol = (int) ((mouse.x - innerX) / cellWidth);
                        
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
        
        batch.end();

        // --- LAYER 6: SCENE2D UI ---
        stage.act(deltaTime);
        stage.draw();
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
        if (messageTable.isVisible()) return; // Already showing, no need to update
        
        messageTable.setVisible(true);
        messageLabel.setVisible(true);
        if (gameState == GameState.PLAYER_WIN) {
            messageLabel.setText("YOU WIN!");
            messageLabel.setColor(Color.YELLOW);
        } else if (gameState == GameState.AI_WIN) {
            messageLabel.setText("AI WINS!");
            messageLabel.setColor(Color.RED);
        } else if (gameState == GameState.TIME_UP) {
            messageLabel.setText("TIME'S UP!");
            messageLabel.setColor(Color.RED);
        }
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

        // 1. Horizontal Check (Clear entire ROW)
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r][c+1] &&
                    grid[r][c] == grid[r][c+2] &&
                    grid[r][c] == grid[r][c+3]) {
                    
                    // CLEAR ENTIRE ROW
                    for (int i = 0; i < COLS; i++) {
                        if (grid[r][i] != 0) createBlastEffect(r, i);
                        grid[r][i] = 0;
                    }
                    foundLine = true;
                    break; // Move to next row
                }
            }
        }

        // 2. Vertical Check (Clear entire COLUMN)
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 4; r++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r+1][c] &&
                    grid[r][c] == grid[r+2][c] &&
                    grid[r][c] == grid[r+3][c]) {
                    
                    // CLEAR ENTIRE COLUMN
                    for (int i = 0; i < ROWS; i++) {
                        if (grid[i][c] != 0) createBlastEffect(i, c);
                        grid[i][c] = 0;
                    }
                    foundLine = true;
                    break; // Move to next column
                }
            }
        }

        // 3. Diagonal Down-Right (\) - Clear Entire Diagonal
        for (int r = 0; r <= ROWS - 4; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r+1][c+1] &&
                    grid[r][c] == grid[r+2][c+2] &&
                    grid[r][c] == grid[r+3][c+3]) {
                    
                    // Identify the starting point of this specific diagonal
                    int startR = r, startC = c;
                    while (startR > 0 && startC > 0) { startR--; startC--; }
                    // Clear the whole diagonal from start to boundary
                    while (startR < ROWS && startC < COLS) {
                        if (grid[startR][startC] != 0) createBlastEffect(startR, startC);
                        grid[startR][startC] = 0;
                        startR++; startC++;
                    }
                    foundLine = true;
                }
            }
        }

        // 4. Diagonal Up-Right (/) - Clear Entire Diagonal
        for (int r = 3; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] != 0 &&
                    grid[r][c] == grid[r-1][c+1] &&
                    grid[r][c] == grid[r-2][c+2] &&
                    grid[r][c] == grid[r-3][c+3]) {
                    
                    // Identify the starting point of this specific diagonal
                    int startR = r, startC = c;
                    while (startR < ROWS - 1 && startC > 0) { startR++; startC--; }
                    // Clear the whole diagonal
                    while (startR >= 0 && startC < COLS) {
                        if (grid[startR][startC] != 0) createBlastEffect(startR, startC);
                        grid[startR][startC] = 0;
                        startR--; startC++;
                    }
                    foundLine = true;
                }
            }
        }

        if (foundLine) {
            popSound.play();
            float bonus = (score < 5000) ? 8f : 3f;
            timeRemaining += bonus;
        }
        
        return foundLine;
    }

    void updateAndDrawUI() {
        if (gameState == GameState.INTRO) return;
        
        int minutes = (int) (timeRemaining / 60);
        int seconds = (int) (timeRemaining % 60);
        timeLabel.setText(String.format("%d:%02d", minutes, seconds));
        goalLabel.setText("" + scoreGoal);
        difficultyLabel.setText("" + currentDifficulty);

        playerLabel.setText(score);
        aiLabel.setText (aiScore);

        if (comboMultiplier > 1) {
            comboLabel.setText("x" + comboMultiplier);
        } else {
            comboLabel.setText("");
        }

        if (gameState == GameState.PLAYING) {
            messageTable.setVisible(false);
            statusLabel.setText(currentPlayer == 1 ? "Your Turn" : "AI Thinking...");
        } else if (gameState != GameState.INTRO) {
            statusLabel.setText("GAME OVER");
            displayEndGameMessage();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        board.dispose();
        frame.dispose();
        background.dispose();
        yellowPiece.dispose();
        redPiece.dispose();
        // if (blastTexture != null) blastTexture.dispose();
        font.dispose();
        stage.dispose();
        skin.dispose();
        
        // Dispose audio
        if (backgroundMusic != null) backgroundMusic.dispose();
        if (popSound != null) popSound.dispose();
        if (winSound != null) winSound.dispose();
        if (loseSound != null) loseSound.dispose();
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
    if (score >= scoreGoal) {
        gameState = GameState.PLAYER_WIN;
        winSound.play();
    } else if (aiScore >= scoreGoal) {
        gameState = GameState.AI_WIN;
        loseSound.play();
    } else if (grid[0][col] != 0) {
        gameState = (playerID == 1) ? GameState.AI_WIN : GameState.PLAYER_WIN;
        if (gameState == GameState.AI_WIN) loseSound.play();
        else winSound.play();
    } else {
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
        float startY = VIRTUAL_HEIGHT;
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

    void createBlastEffect(int r, int c) {
        float size = Math.min(cellWidth, cellHeight);
        float pieceX = innerX + c * cellWidth + (cellWidth - size) / 2f;
        float pieceY = innerY + (ROWS - 1 - r) * cellHeight + (cellHeight - size) / 2f;
        blastEffects.add(new BlastEffect(pieceX + size / 2, pieceY + size / 2, size));
    }
}