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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

class SweepEffect {
    float x, y, width, height, alpha;
    float timer = 0, maxTime = 0.4f;

    SweepEffect(float x, float y, float width, float height) {
        this.x = x; this.y = y;
        this.width = width; this.height = height;
        this.alpha = 1.0f;
    }

    void update(float dt) {
        timer += dt;
        alpha = 1.0f - (timer / maxTime);
    }
}

class BlastEffect {
    float x, y, size, alpha, duration;

    BlastEffect(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.alpha = 1f;
        this.duration = 0.5f; 
    }

    void update(float deltaTime) {
        duration -= deltaTime;
        alpha = duration * 2f; 
        size *= 1 + (deltaTime * 1.25f); 
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

    Texture border2p;
    Texture menuBg, modeBg;
    Texture startImg, tutorialImg, settingsImg, exitImg, backImg;
    Texture easyImg, mediumImg, hardImg, p1Img, p2Img;
    boolean isTwoPlayer = false; // Track if we are in 2P mode

    Sound blast1, blast2;
    ShapeRenderer shapeRenderer;
    List<SweepEffect> activeSweeps = new ArrayList<>();
    List<FallingPiece> activeFallingPieces = new ArrayList<>();
    List<BlastEffect> blastEffects = new ArrayList<>();
    float masterVolume = 0.7f; // Default volume at 50%
    Table settingsTable;        // Table to hold our settings UI

    float innerX, innerY, innerW, innerH;
    float cellWidth, cellHeight;
    enum Difficulty { EASY, MEDIUM, HARD }
    Difficulty currentDifficulty = Difficulty.MEDIUM; // Default
    int scoreGoal = 1000; // The "High Score" to reach
    int score = 0;       // Player score
    int aiScore = 0;     // AI score
    int ROWS = 6;
    int COLS = 7;
    int comboMultiplier = 0;
    float shakeTimer = 0;
    float shakeIntensity = 0;
    

    int[][] grid = new int[ROWS][COLS];
    // 0 = empty, 1 = yellow (player), 2 = red (AI)

    int currentPlayer = 1; // 1 = player (yellow), 2 = AI (red)

    SpriteBatch batch;
    Texture board;
    Texture frame;
    Texture border;
    Texture yellowPiece;
    Texture redPiece;
    Texture background;
    Texture settingsBg;
    Texture volumeImg;
    BitmapFont font;
    Texture pauseBtnImg;
    Texture pausedBg;

    Texture resumeImg;
    Texture restartImg;
    Texture settingsPauseImg; // reuse if same as settings.png

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
    Table exitTable;
    Table messageTable;
    Table modeTable;
    Table pauseTable;
    Image pauseButton;
    Label playerLabel;
    Label aiLabel;
    Label timeLabel;
    Label difficultyLabel;
    Label goalLabel;
    Label statusLabel;
    Label comboLabel;
    Label messageLabel;
    Label instructionsLabel;
    Label p2TimeLabel;
    Label p2ComboLabel;
    com.badlogic.gdx.scenes.scene2d.Group gameHudGroup; // <--- ADD THIS

    Image easyBtn;
    Image medBtn;
    Image hardBtn;

    // Game state
    enum GameState { INTRO, MODE_SELECT, PLAYING, PAUSED, PLAYER_WIN, SETTINGS, AI_WIN, TIME_UP }
    GameState gameState = GameState.INTRO;

    // Timer (2 minutes = 120 seconds)
    float p1TimeRemaining;
    float p2TimeRemaining;
    int p1Combo = 1;
    int p2Combo = 1;
    // AI
    Random random = new Random();
    float aiMoveDelay = 0.5f; // AI waits 0.5s before moving
    float aiTimer = 0f;
    boolean aiNeedsToMove = false;
    private Image pauseTriggerBtn;
    private boolean comingFromPause = false; // Add this here
    private Image easyBtnPause, medBtnPause, hardBtnPause;
    private GameState stateBeforePause = GameState.PLAYING;

    @Override
    public void create() {
        batch = new SpriteBatch();
        border = new Texture("border.png");
        border2p = new Texture("border2p.png");
        background = new Texture("background.png");
        board = new Texture("board.png");
        frame = new Texture("frame.png");
        yellowPiece = new Texture("piece_yellow.png");
        redPiece = new Texture("piece_red.png");

        menuBg = new Texture("menubg.png");
        modeBg = new Texture("modebg.png");
        settingsBg = new Texture("settingsbg.png");
        
        // Main Menu Buttons
        startImg = new Texture("start.png");
        tutorialImg = new Texture("tutorial.png");
        settingsImg = new Texture("settings.png");
        exitImg = new Texture("exitgame.png");
        backImg = new Texture("back.png");
        volumeImg = new Texture("volume.png");
        
        // Mode Menu Buttons
        easyImg = new Texture("easy.png");
        mediumImg = new Texture("medium.png");
        hardImg = new Texture("hard.png");
        
        p1Img = new Texture("1p.png");
        p2Img = new Texture("2p.png");
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);

        pauseBtnImg = new Texture("pausebutton.png");
        pausedBg = new Texture("paused_clear.png");

        resumeImg = new Texture("resume.png");
        restartImg = new Texture("restart.png");
        settingsPauseImg = new Texture("settings.png"); // reuse
        // Load and play background music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("backround_audio.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f);
        backgroundMusic.play();

        // Load sound effects
        popSound = Gdx.audio.newSound(Gdx.files.internal("pop_sound.mp3"));
        winSound = Gdx.audio.newSound(Gdx.files.internal("win_sound.wav"));
        loseSound = Gdx.audio.newSound(Gdx.files.internal("Lose_sound.wav"));
        blast1 = Gdx.audio.newSound(Gdx.files.internal("blast1.mp3"));
        blast2 = Gdx.audio.newSound(Gdx.files.internal("blast2.mp3"));

        // UI SETUP
        // Change ScreenViewport to FitViewport
        viewport = new com.badlogic.gdx.utils.viewport.FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage = new Stage(viewport); // The stage now uses the virtual resolution
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        createIntroUI();
        createGameUI();
        createModeUI(); // New method
        createSettingsUI();
        createPauseUI();

        // Start with Intro
        introTable.setVisible(true);
        gameTable.setVisible(false);
    }

    private void createIntroUI() {
        // 1. Main Menu Table (Start, Tutorial, Settings)
        introTable = new Table();
        introTable.setFillParent(true);
        introTable.center().padTop(75); // Keeps your vertical alignment
        stage.addActor(introTable);

        // 2. Separate Table for Exit Button (Bottom Right)
        exitTable = new Table();
        exitTable.setFillParent(true);
        exitTable.bottom().right().pad(20).padBottom(10);; // Pins to corner with 30px breathing room
        stage.addActor(exitTable);

        // Create UI Images
        Image startBtn = new Image(startImg);
        Image tutorialBtn = new Image(tutorialImg);
        Image settingsBtn = new Image(settingsImg);
        Image exitBtn = new Image(exitImg);

        // --- BUTTON LOGIC ---
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameState = GameState.MODE_SELECT;
                introTable.setVisible(false);
                exitTable.setVisible(true); // Hide both tables
                modeTable.setVisible(true);
            }
        });
        // ADD THIS LISTENER:
        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameState = GameState.SETTINGS;
                introTable.setVisible(false);
                exitTable.setVisible(false); // Hide the main exit button
                settingsTable.setVisible(true);
            }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // --- MAIN LAYOUT ---
        introTable.add(startBtn).width(226).height(57).padBottom(30).row();
        introTable.add(tutorialBtn).width(226).height(57).padBottom(30).row();
        introTable.add(settingsBtn).width(226).height(57);
        exitTable.add(exitBtn).width(226).height(57);
    }

    private void createModeUI() {
        // 2. Setup the Mode Selection Table
        modeTable = new Table();
        modeTable.setFillParent(true);
        modeTable.center().padTop(194);
        stage.addActor(modeTable);
        modeTable.setVisible(false); // Hidden until Start is clicked

        easyBtn = new Image(easyImg);
        medBtn = new Image(mediumImg);
        hardBtn = new Image(hardImg);
        Image p1Btn = new Image(p1Img);
        Image p2Btn = new Image(p2Img);
        Image backBtn = new Image(backImg);
        
        // --- LOGIC ---
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameState = GameState.INTRO;
                modeTable.setVisible(false);
                introTable.setVisible(true);
                exitTable.setVisible(true);
            }
        });

        // --- DIFFICULTY LOGIC ---
    easyBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            scoreGoal = 1000;
            updateDifficultyGlow(easyBtn);
        }
    });
    medBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            scoreGoal = 3000;
            updateDifficultyGlow(medBtn);
        }
    });
    hardBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            scoreGoal = 5000;
            updateDifficultyGlow(hardBtn);
        }
    });

        // --- PLAYER MODE & START LOGIC ---
        p1Btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isTwoPlayer = false;
                modeTable.setVisible(false);
                startGame();
            }
        });

        p2Btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                isTwoPlayer = true;
                modeTable.setVisible(false);
                startGame();
            }
        });

    // --- LAYOUT ---
    Table diffContainer = new Table();
        diffContainer.add(easyBtn).width(226).height(57);
        diffContainer.add(medBtn).width(226).height(57);
        diffContainer.add(hardBtn).width(226).height(57);
        modeTable.add(diffContainer).padBottom(30).row();

        modeTable.add(p1Btn).width(226).height(57).padBottom(30).row();
        modeTable.add(p2Btn).width(226).height(57).padBottom(30).row();
        modeTable.add(backBtn).width(226).height(57).padBottom(30);
        scoreGoal = 3000;            
        updateDifficultyGlow(medBtn);
    }

    private void updateDifficultyGlow(Image selected) {
        easyBtn.setColor(Color.WHITE);
        medBtn.setColor(Color.WHITE);
        hardBtn.setColor(Color.WHITE);
        selected.setColor(Color.YELLOW);
    }
    
    private void createSettingsUI() {
        settingsTable = new Table();
        settingsTable.setFillParent(true);
        settingsTable.center().padTop(-13); // Match the alignment of other menus
        stage.addActor(settingsTable);
        settingsTable.setVisible(false); // Hidden initially

        Image volumeLabel = new Image(volumeImg);
        Image backBtnSettings = new Image(backImg);

        // Create the Volume Slider (min 0.0, max 1.0, step 0.05)
        com.badlogic.gdx.scenes.scene2d.ui.Slider volumeSlider = 
            new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.05f, false, skin);
        
        // Set its starting position to your default volume
        volumeSlider.setColor(Color.valueOf("ffffffff"));
        volumeSlider.setValue(masterVolume); 
        

        // Add a listener to change the volume when the slider moves
        volumeSlider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                masterVolume = volumeSlider.getValue();
                backgroundMusic.setVolume(masterVolume); // Updates music instantly
            }
        });

        // Back Button Logic
    backBtnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                
                if (comingFromPause) {
                    // GO BACK TO PAUSE MENU
                    gameState = GameState.PAUSED;
                    settingsTable.setVisible(false);
                    pauseTable.setVisible(true);
                    comingFromPause = false; // Reset the flag so it doesn't get stuck!
                } else {
                    // GO BACK TO MAIN MENU
                    gameState = GameState.INTRO;
                    settingsTable.setVisible(false);
                    introTable.setVisible(true);
                    if (exitTable != null) {
                        exitTable.setVisible(true);
                    }
                }
                
            }
        });

        // --- LAYOUT ---
        // Put the volume image and the slider on the same row
        settingsTable.add(volumeLabel).width(226).height(57).padBottom(30).padRight(20);
        settingsTable.add(volumeSlider).width(300).height(50).padBottom(30).row();
        settingsTable.add(backBtnSettings).colspan(2).width(226).height(57);
    }

    // Declare this at the top of your class with your other Tables
        private Table pauseExitTable;

        private void createPauseUI() {
            pauseTable = new Table();
            pauseTable.setFillParent(true);
            pauseTable.center().padTop(194);
            stage.addActor(pauseTable);
            pauseTable.setVisible(false);
            pauseExitTable = new Table();
            pauseExitTable.setFillParent(true);
            // Matching your IntroUI Exit location exactly:
            pauseExitTable.bottom().right().pad(20).padBottom(10); 
            stage.addActor(pauseExitTable);
            pauseExitTable.setVisible(false);

            // Buttons
            Image resumeBtn = new Image(resumeImg);
            Image restartBtn = new Image(restartImg);
            Image settingsBtn = new Image(settingsPauseImg);
            easyBtnPause = new Image(easyImg);
            medBtnPause = new Image(mediumImg);
            hardBtnPause = new Image(hardImg);
            Image exitBtnPause = new Image(exitImg); // Your Exit image

            // --- BUTTON LOGIC ---

            resumeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    gameState = stateBeforePause; // <--- Return to PLAYING or GAME OVER
                    pauseTable.setVisible(false);
                    pauseExitTable.setVisible(false); 
                    gameHudGroup.setVisible(true);
                }
            });

            restartBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    pauseTable.setVisible(false);
                    pauseExitTable.setVisible(false); // Hide the corner exit button
                    startGame();
                }
            });

            exitBtnPause.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    gameState = GameState.INTRO;
                    pauseTable.setVisible(false);
                    pauseExitTable.setVisible(false); // Hide this one
                    gameHudGroup.setVisible(false);
                    introTable.setVisible(true);
                    exitTable.setVisible(true);      // Show the Intro's exit button
                }
            });

            settingsBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    comingFromPause = true;
                    gameState = GameState.SETTINGS;
                    pauseTable.setVisible(false);
                    settingsTable.setVisible(true);
                }
            });
            
            // --- DIFFICULTY LOGIC IN createPauseUI ---
            easyBtnPause.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    scoreGoal = 1000;
                    currentDifficulty = Difficulty.EASY; // Update the Enum for the HUD label
                    p1TimeRemaining = 120f;              // Reset/Update time for Easy
                    p2TimeRemaining = 120f;
                    updateDifficultyGlowPause(easyBtnPause);
                }
            });

            medBtnPause.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    scoreGoal = 3000;
                    currentDifficulty = Difficulty.MEDIUM;
                    p1TimeRemaining = 100f;
                    p2TimeRemaining = 100f;
                    updateDifficultyGlowPause(medBtnPause);
                }
            });

            hardBtnPause.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    scoreGoal = 5000;
                    currentDifficulty = Difficulty.HARD;
                    p1TimeRemaining = 80f;
                    p2TimeRemaining = 80f;
                    updateDifficultyGlowPause(hardBtnPause);
                }
            });

            // --- LAYOUT ---
            pauseTable.setBackground(new Image(pausedBg).getDrawable());
            
            // Main Pause Menu Layout (No Exit Button here)
            pauseTable.add(resumeBtn).width(226).height(57).colspan(3).padBottom(30).row();
            pauseTable.add(easyBtnPause).width(226).height(57).padBottom(30);
            pauseTable.add(medBtnPause).width(226).height(57).padBottom(30);
            pauseTable.add(hardBtnPause).width(226).height(57).padBottom(30).row();
            pauseTable.add(restartBtn).width(226).height(57).colspan(3).padBottom(30).row();
            pauseTable.add(settingsBtn).width(226).height(57).colspan(3).padBottom(30);

            // --- CORNER LAYOUT ---
            // Adding the exit button to the separate corner table
            pauseExitTable.add(exitBtnPause).width(226).height(57);
        }
        private void updateDifficultyGlowPause(Image selected) {
            easyBtnPause.setColor(Color.WHITE);
            medBtnPause.setColor(Color.WHITE);
            hardBtnPause.setColor(Color.WHITE);
            selected.setColor(Color.YELLOW); // Highlighting the selected difficulty
        }


        
    private void createGameUI() {
        gameTable = new Table();
        gameTable.setFillParent(true);
        stage.addActor(gameTable);

        // 1. INITIALIZE LABELS
        timeLabel = new Label("", skin); // P1 Timer
        p2TimeLabel = new Label("", skin); // P2 Timer
        goalLabel = new Label("" , skin);
        playerLabel = new Label("", skin);
        aiLabel = new Label("", skin);
        difficultyLabel = new Label("", skin);
        statusLabel = new Label("", skin);
        comboLabel = new Label("", skin);
        p2ComboLabel = new Label("", skin); // P2 Combo <--- ADDED

        // 2. SCALE / ENLARGE
        timeLabel.setFontScale(2.95f);
        p2TimeLabel.setFontScale(2.95f);
        goalLabel.setFontScale(3.4f);
        difficultyLabel.setFontScale(3.7f);
        playerLabel.setFontScale(2.95f);
        aiLabel.setFontScale(2.95f);
        statusLabel.setFontScale(1.5f);
        comboLabel.setFontScale(2.9f);
        p2ComboLabel.setFontScale(2.9f); // <--- ADDED

        // 3. SET COLORS
        playerLabel.setColor(Color.YELLOW);
        aiLabel.setColor(Color.RED);
        comboLabel.setColor(Color.GOLD);
        difficultyLabel.setColor(Color.CYAN);
        

        // 4. POSITIONING
        timeLabel.setPosition(1075, 568);
        p2TimeLabel.setPosition(1075, 518);
        goalLabel.setPosition(877, 626);
        difficultyLabel.setPosition(320, 28);
        statusLabel.setPosition(VIRTUAL_WIDTH / 2f - 50, VIRTUAL_HEIGHT - 50);
        playerLabel.setPosition(832, 568); 
        aiLabel.setPosition(832, 518);     
        comboLabel.setPosition(1282, 568);
        p2ComboLabel.setPosition(1282, 518);

        // 5. ADD TO GROUP INSTEAD OF STAGE
        gameHudGroup = new com.badlogic.gdx.scenes.scene2d.Group();
        stage.addActor(gameHudGroup);
        
        gameHudGroup.addActor(timeLabel);
        gameHudGroup.addActor(p2TimeLabel);
        gameHudGroup.addActor(goalLabel);
        gameHudGroup.addActor(difficultyLabel);
        gameHudGroup.addActor(statusLabel);
        gameHudGroup.addActor(playerLabel);
        gameHudGroup.addActor(aiLabel);
        gameHudGroup.addActor(comboLabel);
        gameHudGroup.addActor(p2ComboLabel); // <--- ADDED
        gameHudGroup.setVisible(false); // <--- Hides the scores initially!

        // 6. MESSAGE OVERLAY
        messageLabel = new Label("", skin, "subtitle");
        messageLabel.setAlignment(Align.center);
        messageTable = new Table();
        messageTable.setFillParent(true);
        messageTable.add(messageLabel).padBottom(30).row();
        messageTable.setVisible(false);
        stage.addActor(messageTable);

        // 7. Pause
        pauseTriggerBtn = new Image(new Texture(Gdx.files.internal("pausebutton.png")));
        pauseTriggerBtn.setSize(70, 70); 
        pauseTriggerBtn.setPosition(VIRTUAL_WIDTH - 75, VIRTUAL_HEIGHT - 75); 
        pauseTriggerBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        // Allow clicking during PLAYING and GAME OVER states
                        if (gameState == GameState.PLAYING || 
                            gameState == GameState.PLAYER_WIN || 
                            gameState == GameState.AI_WIN || 
                            gameState == GameState.TIME_UP) {
                            
                            stateBeforePause = gameState; // <--- Save the state!
                            gameState = GameState.PAUSED;
                            gameHudGroup.setVisible(false); 
                            pauseTable.setVisible(true);    
                            pauseExitTable.setVisible(true);
                            
                            if (currentDifficulty == Difficulty.EASY) updateDifficultyGlowPause(easyBtnPause);
                            else if (currentDifficulty == Difficulty.MEDIUM) updateDifficultyGlowPause(medBtnPause);
                            else if (currentDifficulty == Difficulty.HARD) updateDifficultyGlowPause(hardBtnPause);
                        }
                    }
                });
        gameHudGroup.addActor(pauseTriggerBtn);
    }

    private void startGame() {
        grid = new int[ROWS][COLS];
        gameState = GameState.PLAYING;

        float startTime;
        if (scoreGoal == 1000)      startTime = 120f; // Easy: 2:00
        else if (scoreGoal == 3000) startTime = 100f; // Med: 1:40
        else                        startTime = 80f;  // Hard: 1:20

        p1TimeRemaining = startTime;
        p2TimeRemaining = startTime;

        currentPlayer = 1;
        aiNeedsToMove = false;
        aiTimer = 0f;
        aiScore = 0;
        score = 0;
        comboMultiplier = 0;
        
        introTable.setVisible(false);
        modeTable.setVisible(false); 
        gameTable.setVisible(true);
        gameHudGroup.setVisible(true); // <--- Show HUD now!
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

        // 2. COORDINATE MATH (Shifted to the left half)
        float scale = 2.5f;
        float frameWidth = frame.getWidth() * scale;
        float frameHeight = frame.getHeight() * scale;

        // Change frameX to align with the left side instead of center
        // We use a small padding (e.g., 20) or just 0 to stick it to the far left
        float frameX = 60f; 
        float frameY = 50f; 

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
            // 1. COUNTDOWN LOGIC
            if (currentPlayer == 1) {
                p1TimeRemaining -= deltaTime;
                if (p1TimeRemaining <= 0) {
                    p1TimeRemaining = 0;
                    gameState = GameState.AI_WIN; // P1 ran out of time, so P2/AI wins
                    loseSound.play();
                } else if (gameState == GameState.PAUSED) {
                // 🚫 DO NOTHING → this freezes the game
            }
            } else if (currentPlayer == 2 && isTwoPlayer) {
                // Only tick down if it's 2P mode or if you want the AI to have a limit
                p2TimeRemaining -= deltaTime; 
                if (p2TimeRemaining <= 0) {
                    p2TimeRemaining = 0;
                    gameState = GameState.PLAYER_WIN; // P2 ran out of time, P1 wins
                    winSound.play();
                }
                
            }

            // PLAYER CLICK DETECTION
        // Inside render() -> PLAYING state
        if (currentPlayer != 0) { // 0 means a piece is currently falling
            // Allow clicking if it's P1, OR if it's P2 and we are in Two Player mode
            if (currentPlayer == 1 || (isTwoPlayer && currentPlayer == 2)) {
                if (Gdx.input.justTouched()) {
                    com.badlogic.gdx.math.Vector2 touch = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
                    viewport.unproject(touch);
                    
                    if (touch.x >= innerX && touch.x <= innerX + innerW) {
                        int col = (int) ((touch.x - innerX) / cellWidth);
                        executeMove(col); 
                    }
                }
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
    if (shakeTimer > 0) {
        shakeTimer -= deltaTime;
        float currentShakeX = (random.nextFloat() - 0.5f) * 2 * shakeIntensity;
        float currentShakeY = (random.nextFloat() - 0.5f) * 2 * shakeIntensity;
        viewport.getCamera().translate(currentShakeX, currentShakeY, 0);
    } else {
        // Reset camera to center if not shaking
        viewport.getCamera().position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
    }
    viewport.getCamera().update();
    batch.setProjectionMatrix(viewport.getCamera().combined);

    // --- PRE-CALCULATE BOARD POSITION ---
    // We calculate these early so we can use them for the background layer
    float boardDrawW = innerW * (board.getWidth() / 112f);
    float boardDrawH = innerH * (board.getHeight() / 96f);
    float boardDrawX = innerX - (boardDrawW * (63f / 240f));
    float boardDrawY = innerY - (boardDrawH * (97f / 256f));

    // --- LAYER 1: BORDER (The Wallpaper/Screen Edges) ---
    // This will now be visible because the background won't cover the whole screen
    // Replace the old border draw with this:
    batch.begin();
    if (gameState == GameState.INTRO) {
        batch.draw(menuBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    } else if (gameState == GameState.MODE_SELECT) {
        batch.draw(modeBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    } else if (gameState == GameState.SETTINGS) { // <--- ADD THIS BLOCK
        batch.draw(settingsBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    } else {
        // Regular Game Border
        Texture activeBorder = isTwoPlayer ? border2p : border;
        batch.draw(activeBorder, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }
    // --- LAYER 2: BACKGROUND (Behind the pieces/holes only) ---
    if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
        float scalee = 0.98f;
        float scaledW = boardDrawW * scalee;
        float scaledH = boardDrawH * scalee;
        float scaledX = boardDrawX + (boardDrawW - scaledW) / 2f;
        float scaledY = (boardDrawY + (boardDrawH - scaledH) / 2f) + 1f;
        batch.draw(background, scaledX, scaledY, scaledW, scaledH);
    }
    // --- LAYER 3: PIECES (Stationary and Falling) ---
    if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
        for (int i = activeFallingPieces.size() - 1; i >= 0; i--) {
            FallingPiece p = activeFallingPieces.get(i);
            p.update(deltaTime);
            float size = Math.min(cellWidth, cellHeight) * 0.99f;
            Texture pieceTex = (p.player == 1 ? yellowPiece : redPiece);

            for (int j = 0; j < p.previousY.length; j++) {
                float alpha = (j < 2) ? 1.0f : 1.0f - (j * 0.2f);
                batch.setColor(1, 1, 1, Math.max(0, alpha));
                float trailWidth = size * 0.8f;
                float trailX = p.x + (size - trailWidth) / 2f;
                batch.draw(pieceTex, trailX, p.previousY[j], trailWidth, size + (j * 10f));
            }
            batch.setColor(Color.WHITE);
            batch.draw(pieceTex, p.x, p.y, size, size);

            if (p.y <= p.targetY) {
                popSound.play(masterVolume);
                grid[p.row][p.col] = p.player;
                activeFallingPieces.remove(i);
                finalizeTurn(p.col, p.player);
            }
        }

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

    // --- LAYER 4: THE BOARD (The Mask) ---
    if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
        batch.draw(board, boardDrawX, boardDrawY, boardDrawW, boardDrawH);
    }

    batch.end();

    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // Makes it look like a neon glow
    
    shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

    // 1. Draw Laser Sweeps (Rows/Cols)
    for (int i = activeSweeps.size() - 1; i >= 0; i--) {
        SweepEffect s = activeSweeps.get(i);
        s.update(deltaTime);
        shapeRenderer.setColor(1f, 1f, 1f, s.alpha); // Bright white-blue flash
        shapeRenderer.rect(s.x, s.y, s.width, s.height);
        if (s.alpha <= 0) activeSweeps.remove(i);
    }

    // 2. Draw Diagonal Blasts (Expanding Rings)
    for (int i = blastEffects.size() - 1; i >= 0; i--) {
        BlastEffect b = blastEffects.get(i);
        b.update(deltaTime);
        shapeRenderer.setColor(1f, 0.8f, 0f, b.alpha); // Golden glow for diagonals
        shapeRenderer.circle(b.x, b.y, b.size / 2);
        if (b.isFinished()) blastEffects.remove(i);
    }

    shapeRenderer.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
    batch.begin(); // Restart the batch for the frame and UI
    // ------------------------------------------------

    // --- LAYER 5: THE FRAME (Outer Plastic) ---
    if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
        batch.draw(frame, frameX, frameY, frameWidth, frameHeight);
    }

    // --- LAYER 6: PREVIEW PIECE ---
    if (gameState == GameState.PLAYING && (currentPlayer == 1 || (isTwoPlayer && currentPlayer == 2))) {
        com.badlogic.gdx.math.Vector2 mouse = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);

        if (mouse.x >= innerX && mouse.x <= innerX + innerW) {
            int hoverCol = (int) ((mouse.x - innerX) / cellWidth);
            float size = Math.min(cellWidth, cellHeight) * 0.99f;
            float previewX = innerX + hoverCol * cellWidth + (cellWidth - size) / 1f + 2.2f;
            float previewY = innerY + ROWS * cellHeight + 10;

            // Choose texture based on current player
            Texture previewTex = (currentPlayer == 1) ? yellowPiece : redPiece;

            // Visual feedback if column is full
            if (grid[0][hoverCol] == 0) {
                batch.setColor(1, 1, 1, 0.5f); // Transparent preview
            } else {
                batch.setColor(1, 0, 0, 0.7f); // Red tint if blocked
            }

            batch.draw(previewTex, previewX, previewY, size, size);
            batch.setColor(Color.WHITE);
        }
    }

    updateAndDrawUI();
    batch.end();

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

    // Replace your existing checkAndRemoveLines method with this:
    int checkAndRemoveLines() {
        int foundType = 0; // 0 = None, 1 = Horizontal, 2 = Vertical, 3 = Diagonal

        // 1. Horizontal Check (Clear entire ROW)
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (grid[r][c] != 0 && grid[r][c] == grid[r][c+1] && grid[r][c] == grid[r][c+2] && grid[r][c] == grid[r][c+3]) {
                    float sweepY = innerY + (ROWS - 1 - r) * cellHeight;
                    activeSweeps.add(new SweepEffect(innerX, sweepY, innerW, cellHeight));
                    for (int i = 0; i < COLS; i++) {
                        if (grid[r][i] != 0) createBlastEffect(r, i);
                        grid[r][i] = 0;
                    }
                    foundType = 1; // Mark as Horizontal
                    break; 
                }
            }
        }

        // 2. Vertical Check (Clear entire COLUMN)
        // We only check if we haven't already found a horizontal this "pass"
        if (foundType == 0) {
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r <= ROWS - 4; r++) {
                    if (grid[r][c] != 0 && grid[r][c] == grid[r+1][c] && grid[r][c] == grid[r+2][c] && grid[r][c] == grid[r+3][c]) {
                        float sweepX = innerX + c * cellWidth;
                        activeSweeps.add(new SweepEffect(sweepX, innerY, cellWidth, innerH));
                        for (int i = 0; i < ROWS; i++) {
                            if (grid[i][c] != 0) createBlastEffect(i, c);
                            grid[i][c] = 0;
                        }
                        foundType = 2; // Mark as Vertical
                        break;
                    }
                }
            }
        }

        // 3. Diagonal Checks
        if (foundType == 0) {
            // Diagonal Down-Right (\)
            for (int r = 0; r <= ROWS - 4; r++) {
                for (int c = 0; c <= COLS - 4; c++) {
                    if (grid[r][c] != 0 && grid[r][c] == grid[r+1][c+1] && grid[r][c] == grid[r+2][c+2] && grid[r][c] == grid[r+3][c+3]) {
                        int startR = r, startC = c;
                        while (startR > 0 && startC > 0) { startR--; startC--; }
                        while (startR < ROWS && startC < COLS) {
                            if (grid[startR][startC] != 0) createBlastEffect(startR, startC);
                            grid[startR][startC] = 0;
                            startR++; startC++;
                        }
                        foundType = 3; // Mark as Diagonal
                    }
                }
            }
            // Diagonal Up-Right (/)
            for (int r = 3; r < ROWS; r++) {
                for (int c = 0; c <= COLS - 4; c++) {
                    if (grid[r][c] != 0 && grid[r][c] == grid[r-1][c+1] && grid[r][c] == grid[r-2][c+2] && grid[r][c] == grid[r-3][c+3]) {
                        int startR = r, startC = c;
                        while (startR < ROWS - 1 && startC > 0) { startR++; startC--; }
                        while (startR >= 0 && startC < COLS) {
                            if (grid[startR][startC] != 0) createBlastEffect(startR, startC);
                            grid[startR][startC] = 0;
                            startR--; startC++;
                        }
                        foundType = 3;
                    }
                }
            }
        }
        
        return foundType; // Returns 0, 1, 2, or 3
    }

    void updateAndDrawUI() {
        if (gameState == GameState.INTRO) return;
        
        pauseTriggerBtn.setVisible(gameState == GameState.PLAYING || 
                                   gameState == GameState.PLAYER_WIN || 
                                   gameState == GameState.AI_WIN || 
                                   gameState == GameState.TIME_UP);

        // 1. --- TIMER LOGIC & PULSING EFFECT ---
        int m1 = (int) (p1TimeRemaining / 60);
        int s1 = (int) (p1TimeRemaining % 60);
        timeLabel.setText(String.format("%d:%02d", m1, s1));

        // Pulse P1 Timer Red if under 10 seconds
        if (p1TimeRemaining < 10 && p1TimeRemaining > 0) {
            float alpha = 0.5f + (float)Math.abs(Math.sin(Gdx.graphics.getFrameId() * 0.2f)) * 0.5f;
            timeLabel.setColor(1, 0, 0, alpha); 
        } else {
            timeLabel.setColor(Color.WHITE);
        }

        int p1Combo = 1;
        int p2Combo = 1;

        int m2 = (int) (p2TimeRemaining / 60);
        int s2 = (int) (p2TimeRemaining % 60);
        p2TimeLabel.setText(String.format("%d:%02d", m2, s2));
        p2TimeLabel.setVisible(isTwoPlayer); 

        // Pulse P2 Timer Red if under 10 seconds
        if (p2TimeRemaining < 10 && p2TimeRemaining > 0 && isTwoPlayer) {
            float alpha = 0.5f + (float)Math.abs(Math.sin(Gdx.graphics.getFrameId() * 0.2f)) * 0.5f;
            p2TimeLabel.setColor(1, 0, 0, alpha);
        } else {
            p2TimeLabel.setColor(Color.WHITE);
        }

        // --- 2. DYNAMIC P2/AI LABELS ---
    if (isTwoPlayer) {
        playerLabel.setText("" + score);
        aiLabel.setText("" + aiScore);
        aiLabel.setColor(Color.RED); // Ensure P2 stays Red
    } else {
        playerLabel.setText("" + score);
        aiLabel.setText("" + aiScore);
        aiLabel.setColor(Color.RED);
    }
    
    goalLabel.setText("" + scoreGoal);
    difficultyLabel.setText("" + currentDifficulty);

    // --- 3. THE COMBO COUNTER LOGIC (P1 + P2 SEPARATE) ---

    // ✅ Visibility based on mode
    comboLabel.setVisible(true);                  // P1 always visible
    p2ComboLabel.setVisible(isTwoPlayer);         // Only show in 2P mode

    // Update combo display
    comboLabel.setText("x" + p1Combo);

    if (isTwoPlayer) {
        p2ComboLabel.setText("x" + p2Combo);
    }

    // Scale + color for P1 combo
    float baseScale = 2.9f;
    comboLabel.setFontScale(baseScale + (p1Combo * 0.15f));

    if (p1Combo >= 5) comboLabel.setColor(Color.RED);
    else if (p1Combo >= 3) comboLabel.setColor(Color.ORANGE);
    else if (p1Combo > 1) comboLabel.setColor(Color.GOLD);
    else comboLabel.setColor(Color.WHITE);

    // Scale + color for P2 combo (ONLY if 2P)
    if (isTwoPlayer) {
        p2ComboLabel.setFontScale(baseScale + (p2Combo * 0.15f));

        if (p2Combo >= 5) p2ComboLabel.setColor(Color.RED);
        else if (p2Combo >= 3) p2ComboLabel.setColor(Color.ORANGE);
        else if (p2Combo > 1) p2ComboLabel.setColor(Color.GOLD);
        else p2ComboLabel.setColor(Color.WHITE);
    }

        // 4. --- MASTER TOGGLE & STATUS ---
        if (exitTable != null) {
            exitTable.setVisible(gameState == GameState.INTRO || 
                                gameState == GameState.MODE_SELECT || 
                                gameState == GameState.SETTINGS);
        }

        if (gameState == GameState.PLAYING) {
            messageTable.setVisible(false);
            if (isTwoPlayer) {
                statusLabel.setText(currentPlayer == 1 ? "P1 Turn" : "P2 Turn");
            } else {
                statusLabel.setText(currentPlayer == 1 ? "Your Turn" : "AI Thinking...");
            }
        } else if (gameState == GameState.PLAYER_WIN || gameState == GameState.AI_WIN || gameState == GameState.TIME_UP) {
            statusLabel.setText("GAME OVER");
            displayEndGameMessage();
        } else {
            messageTable.setVisible(false);
            statusLabel.setText(""); 
        }
    }

    @Override
        public void dispose() {
            // Systems & UI
            batch.dispose();
            shapeRenderer.dispose();
            font.dispose();
            stage.dispose();
            skin.dispose();
            
            // Game Board Textures
            board.dispose();
            frame.dispose();
            border.dispose();
            border2p.dispose();
            background.dispose();
            yellowPiece.dispose();
            redPiece.dispose();

            // Menu & Mode Textures
            menuBg.dispose();
            modeBg.dispose();
            startImg.dispose();
            tutorialImg.dispose();
            settingsImg.dispose();
            exitImg.dispose();
            easyImg.dispose();
            mediumImg.dispose();
            hardImg.dispose();
            p1Img.dispose();
            p2Img.dispose();
            settingsBg.dispose(); // <--- ADD THIS
            volumeImg.dispose();

            // Audio
            if (backgroundMusic != null) backgroundMusic.dispose();
            if (popSound != null) popSound.dispose();
            if (winSound != null) winSound.dispose();
            if (loseSound != null) loseSound.dispose();
            if (blast1 != null) blast1.dispose();
            if (blast2 != null) blast2.dispose();
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
        comboMultiplier = 0; 
        int clearType;
        
        // The while loop handles cascades (chains)
        while ((clearType = checkAndRemoveLines()) != 0) {
            comboMultiplier++; 
            
            // 1. AWARD TIME BONUS
            float bonus = (scoreGoal < 3000) ? 8f : 5f; // More generous bonus on Easy
            if (playerID == 1) p1TimeRemaining += bonus;
            else p2TimeRemaining += bonus;

            // 2. CALCULATE POINTS (Exponential growth for combos)
            int points = (100 * (int)Math.pow(2, comboMultiplier - 1));
            if (playerID == 1) score += points;
            else aiScore += points;

            // 3. AUDIO & VISUAL JUICE
            float pitch = 0.8f + (comboMultiplier * 0.2f); // Pitch goes up with combo
            if (clearType == 2) blast2.play(masterVolume, pitch, 0); // Vertical
            else blast1.play(masterVolume, pitch, 0);               // Horiz/Diag

            shakeTimer = 0.2f; 
            shakeIntensity = 3f + (comboMultiplier * 4f); // Bigger combos = bigger shake

            // 4. GRAVITY
            for(int c = 0; c < COLS; c++) bubbleSortColumn(c);
        }

        // Check Win Conditions
        if (score >= scoreGoal) {
            gameState = GameState.PLAYER_WIN;
            winSound.play(masterVolume);
        } else if (aiScore >= scoreGoal) {
            gameState = GameState.AI_WIN;
            if (isTwoPlayer) winSound.play(masterVolume); // P2 wins
            else loseSound.play(masterVolume);            // AI wins
        } else if (grid[0][col] != 0) {
            // Board overflow logic
            gameState = (playerID == 1) ? GameState.AI_WIN : GameState.PLAYER_WIN;
        } else {
            // SWITCH TURNS
            currentPlayer = (playerID == 1) ? 2 : 1;
            
            if (!isTwoPlayer && currentPlayer == 2) {
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
            float startY = 600;
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
        
        // This adds a new BlastEffect to the list
        blastEffects.add(new BlastEffect(pieceX + size / 2, pieceY + size / 2, size));
    }
}