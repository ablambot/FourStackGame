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
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class FallingPiece {
    float x, y, targetY, speed;
    int player;
    int col, row;
    
    float[] previousY = new float[5]; 

    FallingPiece(float x, float y, float targetY, int player, int col, int row) {
        this.x = x;
        this.y = y;
        this.targetY = targetY;
        this.player = player;
        this.speed = 1500f; 
        this.col = col;
        this.row = row;
        
        for(int i = 0; i < previousY.length; i++) previousY[i] = y;
    }

    void update(float deltaTime) {
        
        for (int i = previousY.length - 1; i > 0; i--) {
            previousY[i] = previousY[i - 1];
        }
        previousY[0] = y; 
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
        
    int tutorialStage = 0; 

    int[] ghostMovesH = {0, 0, 1, 1, 2, 2, 3}; 
    int[] ghostMovesV = {3, 4, 3, 4, 3, 4, 3}; 
    int[] ghostMovesD = {0, 1, 1, 2, 3, 2, 2, 3, 3, 4, 3}; 
    
    int ghostMoveIndex = 0;
    float ghostTimer = 0f;
    float ghostMoveDelay = 1.0f; 

    public static final float VIRTUAL_WIDTH = 1440;
    public static final float VIRTUAL_HEIGHT = 762;
    private com.badlogic.gdx.utils.viewport.Viewport viewport;

    Sound blast1, blast2;
    ShapeRenderer shapeRenderer;
    List<SweepEffect> activeSweeps = new ArrayList<>();
    List<FallingPiece> activeFallingPieces = new ArrayList<>();
    List<BlastEffect> blastEffects = new ArrayList<>();
    float masterVolume = 1f; 
    Table settingsTable;

    float innerX, innerY, innerW, innerH;
    float cellWidth, cellHeight;
    enum Difficulty { EASY, MEDIUM, HARD }
    Difficulty currentDifficulty = Difficulty.MEDIUM; 
    int scoreGoal = 1000; 
    int score = 0;      
    int aiScore = 0;    
    int ROWS = 6;
    int COLS = 7;
    int comboMultiplier = 0;
    float shakeTimer = 0;
    float shakeIntensity = 0;
    
    int[][] grid = new int[ROWS][COLS];

    int currentPlayer = 1; 

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
    Texture border2p;
    Texture menuBg, modeBg;
    Texture startImg, tutorialImg, settingsImg, exitImg, backImg;
    Texture easyImg, mediumImg, hardImg, p1Img, p2Img;
    Texture playAgainBg, yesImg, noImg;
    Table playAgainTable;
    boolean isTwoPlayer = false;
    Texture pauseBtnImg;
    Texture pausedBg;
    Texture p1TurnTex, p2TurnTex, aiTurnTex;
    Texture winTex, lossTex, p1WinTex, p2WinTex, p1LostTex, p2LostTex;
    Texture timeTex;
    Image statusImage;
    float statusScale = 1.0f;
    Texture p1TurnClTex, p2TurnClTex, aiTurnClTex;
    Texture winClTex, lossClTex, timesUpClTex;
    Texture resumeImg;
    Texture restartImg;
    Texture settingsPauseImg;
    Texture tutorialBg;
    Image clImage;
    float clScale = 1.0f;
    Texture[] expTextures;
    Image expImage;
    float expTimer = 0f;
    boolean showExp = false;

    Music backgroundMusic;
    Sound popSound;
    Sound winSound;
    Sound loseSound;

    Stage stage;
    Skin skin;
    Table introTable;
    Table gameTable;
    Table exitTable;
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
    Label instructionsLabel;
    Label p2TimeLabel;
    Label p2ComboLabel;
    com.badlogic.gdx.scenes.scene2d.Group gameHudGroup;
    Image easyBtn;
    Image medBtn;
    Image hardBtn;

    private Table tutorialTable;
    private int tutorialStep = 0;
    private Texture[] tutCharacters;
    private Texture[] tutTextBoxes;
    private Image currentCharacterImg;
    private Image currentTextBoxImg;

    enum PowerUp { NONE, BULLET, STAR, EIGHT, COIN, BOMB }
    Texture bulletTex, starTex, eightTex, coinTex, bombTex;
    Texture[] powerUpList;
    
    int p1LinesCleared = 0;
    int p2LinesCleared = 0;
    PowerUp p1PowerUp = PowerUp.NONE;
    PowerUp p2PowerUp = PowerUp.NONE;

    boolean p1TimerStopped = false;
    boolean p2TimerStopped = false;
    PowerUp activeTargeting = PowerUp.NONE;
    boolean bulletIsRow = false;

    boolean isShuffling1 = false;
    boolean isShuffling2 = false;
    float shuffleTimer1 = 0f;
    float shuffleTimer2 = 0f;
    int shuffleIndex1 = 0;
    int shuffleIndex2 = 0;

    float powerUpScale = 0.13f;
    float p1PowerUpX = 31f;
    float p1PowerUpY = 85f;
    float p2PowerUpX = 602f;   
    float p2PowerUpY = 85f;

    enum GameState { INTRO, MODE_SELECT, SETTINGS, TUTORIAL, PLAYING, PLAYER_WIN, AI_WIN, TIME_UP, PAUSED }
    GameState gameState = GameState.INTRO;

    float p1TimeRemaining;
    float p2TimeRemaining;
    int p1Combo = 1;
    int p2Combo = 1;

    Random random = new Random();
    float aiMoveDelay = 0.65f;
    float aiTimer = 0f;
    boolean aiNeedsToMove = false;
    private Image pauseTriggerBtn;
    private boolean comingFromPause = false; 
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
    redPiece = new Texture("piece_yellow.png");
    yellowPiece = new Texture("piece_red.png");

    menuBg = new Texture("menubg.png");
    modeBg = new Texture("modebg.png");
    settingsBg = new Texture("settingsbg.png");
    tutorialBg = new Texture("tutorialbg.png");
    
    startImg = new Texture("start.png");
    tutorialImg = new Texture("tutorial.png");
    settingsImg = new Texture("settings.png");
    exitImg = new Texture("exitgame.png");
    backImg = new Texture("back.png");
    volumeImg = new Texture("volume.png");
    easyImg = new Texture("easy.png");
    mediumImg = new Texture("medium.png");
    hardImg = new Texture("hard.png");
    
    p1Img = new Texture("1p.png");
    p2Img = new Texture("2p.png");
    shapeRenderer = new ShapeRenderer();

    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("myfont.TTF"));
    FreeTypeFontParameter parameter = new FreeTypeFontParameter();

    parameter.size = 32; 
    parameter.color = Color.WHITE;
    parameter.borderWidth = 2;
    parameter.borderColor = Color.valueOf("3b2d59");

    font = generator.generateFont(parameter); 
    generator.dispose();

    pauseBtnImg = new Texture("pausebutton.png");
    pausedBg = new Texture("paused_clear.png");
    playAgainBg = new Texture("again.png");
    yesImg = new Texture("yes.png");
    noImg = new Texture("no.png");
    resumeImg = new Texture("resume.png");
    restartImg = new Texture("restart.png");
    settingsPauseImg = new Texture("settings.png");

    backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background_audio.mp3"));
    backgroundMusic.setLooping(true);
    backgroundMusic.setVolume(0.5f);
    backgroundMusic.play();

    popSound = Gdx.audio.newSound(Gdx.files.internal("pop_sound.mp3"));
    winSound = Gdx.audio.newSound(Gdx.files.internal("win_sound.wav"));
    loseSound = Gdx.audio.newSound(Gdx.files.internal("lose_sound.wav"));
    blast1 = Gdx.audio.newSound(Gdx.files.internal("blast1.mp3"));
    blast2 = Gdx.audio.newSound(Gdx.files.internal("blast2.mp3"));

    p1TurnTex = new Texture("p1turn.png");
    p2TurnTex = new Texture("p2turn.png");
    aiTurnTex = new Texture("aiturn.png");
    winTex = new Texture("win.png");
    lossTex = new Texture("loss.png");
    p1WinTex = new Texture("p1win.png");
    p2WinTex = new Texture("p2win.png");
    p1LostTex = new Texture("p1lost.png");
    p2LostTex = new Texture("p2lost.png");
    timeTex = new Texture("time.png");
    expTextures = new Texture[5];
    for (int i = 0; i < 5; i++) {
        expTextures[i] = new Texture("exp" + (i + 1) + ".png");
    }

    p1TurnClTex = new Texture("p1turn_cl.png");
    p2TurnClTex = new Texture("p2turn_cl.png");
    aiTurnClTex = new Texture("aiturn_cl.png");
    winClTex = new Texture("win_cl.png");
    lossClTex = new Texture("loss_cl.png");
    timesUpClTex = new Texture("timesup_cl.png");

    bulletTex = new Texture("bullet.png");
    starTex = new Texture("star.png");
    eightTex = new Texture("eight.png");
    coinTex = new Texture("coin.png");
    bombTex = new Texture("bomb.png");
    powerUpList = new Texture[]{bulletTex, starTex, eightTex, coinTex, bombTex};

    viewport = new com.badlogic.gdx.utils.viewport.FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    stage = new Stage(viewport); 
    Gdx.input.setInputProcessor(stage);
    skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

    
    createSettingsUI(); 
    createPauseUI();
    createTutorialUI();
    createGameUI();     
    createModeUI();
    createIntroUI();
    createPlayAgainUI();

    introTable.setVisible(true);
    gameTable.setVisible(false);
}

private void createIntroUI() {
    introTable = new Table();
    introTable.setFillParent(true);
    introTable.center().padTop(75); 
    stage.addActor(introTable);

    exitTable = new Table();
    exitTable.setFillParent(true);
    exitTable.bottom().right().pad(20).padBottom(10);; 
    stage.addActor(exitTable);

    Image startBtn = new Image(startImg);
    Image tutorialBtn = new Image(tutorialImg);
    Image settingsBtn = new Image(settingsImg);
    Image exitBtn = new Image(exitImg);

    startBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = GameState.MODE_SELECT;
            introTable.setVisible(false);
            exitTable.setVisible(true); 
            modeTable.setVisible(true);
        }
    });

    settingsBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = GameState.SETTINGS;
            introTable.setVisible(false);
            exitTable.setVisible(false); 
            settingsTable.setVisible(true);
        }
    });

    exitBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            Gdx.app.exit();
        }
    });

    tutorialBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            startTutorialGameplay();
        }
    });

    introTable.add(startBtn).width(226).height(57).padBottom(30).row();
    introTable.add(tutorialBtn).width(226).height(57).padBottom(30).row();
    introTable.add(settingsBtn).width(226).height(57);
    exitTable.add(exitBtn).width(226).height(57);
}

private void createModeUI() {

    modeTable = new Table();
    modeTable.setFillParent(true);
    modeTable.center().padTop(194);
    stage.addActor(modeTable);
    modeTable.setVisible(false); 

    easyBtn = new Image(easyImg);
    medBtn = new Image(mediumImg);
    hardBtn = new Image(hardImg);
    Image p1Btn = new Image(p1Img);
    Image p2Btn = new Image(p2Img);
    Image backBtn = new Image(backImg);
    
    backBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = GameState.INTRO;
            modeTable.setVisible(false);
            introTable.setVisible(true);
            exitTable.setVisible(true);
        }
    });

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
    settingsTable.center().padTop(-13); 
    stage.addActor(settingsTable);
    settingsTable.setVisible(false); 

    Image volumeLabel = new Image(volumeImg);
    Image backBtnSettings = new Image(backImg);

    com.badlogic.gdx.scenes.scene2d.ui.Slider volumeSlider = 
        new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.05f, false, skin);
    
    volumeSlider.setColor(Color.valueOf("ffffffff"));
    volumeSlider.setValue(masterVolume); 

    volumeSlider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
        @Override
        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            masterVolume = volumeSlider.getValue();
            backgroundMusic.setVolume(masterVolume); 
        }
    });

    backBtnSettings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                
                if (comingFromPause) {
                    gameState = GameState.PAUSED;
                    settingsTable.setVisible(false);
                    pauseTable.setVisible(true);
                    gameHudGroup.setVisible(true);
                    comingFromPause = false;
                } else {
                    gameState = GameState.INTRO;
                    settingsTable.setVisible(false);
                    introTable.setVisible(true);
                    if (exitTable != null) {
                        exitTable.setVisible(true);
                    }
                }
                
            }
        });

    settingsTable.add(volumeLabel).width(226).height(57).padBottom(30).padRight(20);
    settingsTable.add(volumeSlider).width(300).height(50).padBottom(30).row();
    settingsTable.add(backBtnSettings).colspan(2).width(226).height(57);
}

private Table pauseExitTable;

private void createPauseUI() {
    pauseTable = new Table();
    pauseTable.setFillParent(true);
    pauseTable.center().padTop(194);
    stage.addActor(pauseTable);
    pauseTable.setVisible(false);
    pauseExitTable = new Table();
    pauseExitTable.setFillParent(true);
    pauseExitTable.bottom().right().pad(20).padBottom(10); 
    stage.addActor(pauseExitTable);
    pauseExitTable.setVisible(false);

    Image resumeBtn = new Image(resumeImg);
    Image restartBtn = new Image(restartImg);
    Image settingsBtn = new Image(settingsPauseImg);
    easyBtnPause = new Image(easyImg);
    medBtnPause = new Image(mediumImg);
    hardBtnPause = new Image(hardImg);
    Image exitBtnPause = new Image(exitImg);

    resumeBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = stateBeforePause; 
            pauseTable.setVisible(false);
            pauseExitTable.setVisible(false); 
            gameHudGroup.setVisible(true);
        }
    });

    restartBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            pauseTable.setVisible(false);
            pauseExitTable.setVisible(false); 
            startGame();
        }
    });

    exitBtnPause.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = GameState.INTRO;
            pauseTable.setVisible(false);
            pauseExitTable.setVisible(false); 
            gameHudGroup.setVisible(false);
            introTable.setVisible(true);
            exitTable.setVisible(true);      
        }
    });

    settingsBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            comingFromPause = true;
            gameState = GameState.SETTINGS;
            pauseTable.setVisible(false);
            settingsTable.setVisible(true);
            settingsTable.toFront();
            gameHudGroup.setVisible(false);
        }
    });
    
    
    easyBtnPause.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            scoreGoal = 1000;
            currentDifficulty = Difficulty.EASY;
            p1TimeRemaining = 120f;
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

    pauseTable.setBackground(new Image(pausedBg).getDrawable());
    pauseTable.add(resumeBtn).width(226).height(57).colspan(3).padBottom(30).row();
    pauseTable.add(easyBtnPause).width(226).height(57).padBottom(30);
    pauseTable.add(medBtnPause).width(226).height(57).padBottom(30);
    pauseTable.add(hardBtnPause).width(226).height(57).padBottom(30).row();
    pauseTable.add(restartBtn).width(226).height(57).colspan(3).padBottom(30).row();
    pauseTable.add(settingsBtn).width(226).height(57).colspan(3).padBottom(30);
    pauseExitTable.add(exitBtnPause).width(226).height(57);
}

private void updateDifficultyGlowPause(Image selected) {
    easyBtnPause.setColor(Color.WHITE);
    medBtnPause.setColor(Color.WHITE);
    hardBtnPause.setColor(Color.WHITE);
    selected.setColor(Color.YELLOW); 
}

private void createTutorialUI() {
    tutorialTable = new Table();
    tutorialTable.setFillParent(true);

    Image clickArea = new Image();
    clickArea.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    clickArea.setPosition(0, 0);
    tutorialTable.addActor(clickArea);

    tutCharacters = new Texture[3];
    for (int i = 0; i < 3; i++) {
        tutCharacters[i] = new Texture(Gdx.files.internal("tut" + (i + 1) + ".png"));
    }
    
    tutTextBoxes = new Texture[12];
    for (int i = 0; i < 12; i++) {
        tutTextBoxes[i] = new Texture(Gdx.files.internal((i + 1) + ".png"));
    }

    currentTextBoxImg = new Image(tutTextBoxes[0]);
    currentCharacterImg = new Image(tutCharacters[0]);

    float textBoxScale = 0.5f;   
    float textBoxX = 25f;    
    float textBoxY = 380f;      

    float textBoxWidth = tutTextBoxes[0].getWidth() * textBoxScale;
    float textBoxHeight = tutTextBoxes[0].getHeight() * textBoxScale;
    
    float charX = 80f;
    float charY = 0f;
    float charWidth = 287f;    
    float charHeight = 400f;    

    currentTextBoxImg.setPosition(textBoxX, textBoxY);
    currentTextBoxImg.setSize(textBoxWidth, textBoxHeight);
    
    currentCharacterImg.setPosition(charX, charY);
    currentCharacterImg.setSize(charWidth, charHeight);

    tutorialTable.addActor(currentTextBoxImg);
    tutorialTable.addActor(currentCharacterImg);

    Image tBackBtn = new Image(new Texture(Gdx.files.internal("back.png")));
    tBackBtn.setSize(226, 57);
    tBackBtn.setPosition(1230, 10);
    tutorialTable.addActor(tBackBtn);

    clickArea.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            tutorialStep++;
            
            if (tutorialStep >= 12) {
                tutorialStep = 0; 
            }
            
            currentTextBoxImg.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(tutTextBoxes[tutorialStep]));
            
            currentTextBoxImg.setSize(tutTextBoxes[tutorialStep].getWidth() * textBoxScale, tutTextBoxes[tutorialStep].getHeight() * textBoxScale);

            currentCharacterImg.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(tutCharacters[tutorialStep % 3]));
        }
    });

    tBackBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = GameState.INTRO;
            tutorialTable.setVisible(false);
            introTable.setVisible(true);
            if (exitTable != null) {
                exitTable.setVisible(true); 
            }
            
            tutorialStep = 0;
            currentTextBoxImg.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(tutTextBoxes[0]));
            currentCharacterImg.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(tutCharacters[0]));
        }
    });

    stage.addActor(tutorialTable);
    tutorialTable.setVisible(false); 
}

private void startTutorialGameplay() {
    gameState = GameState.TUTORIAL;
    
    
    grid = new int[ROWS][COLS];
    activeFallingPieces.clear();
    activeSweeps.clear();
    blastEffects.clear();
    
    
    ghostMoveIndex = 0;
    tutorialStage = 0;
    ghostTimer = 0f;
    currentPlayer = 1; 
    
    
    introTable.setVisible(false);
    if (exitTable != null) exitTable.setVisible(false);
    gameHudGroup.setVisible(false);
    tutorialTable.setVisible(true); 
}

private void createPlayAgainUI() {
    playAgainTable = new Table();
    playAgainTable.setFillParent(true);
    playAgainTable.center();
    playAgainTable.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(playAgainBg));
    
    Image yesBtn = new Image(yesImg);
    Image noBtn = new Image(noImg);
    
    yesBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            playAgainTable.setVisible(false);
            startGame(); 
        }
    });
    
    noBtn.addListener(new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            gameState = GameState.INTRO;
            playAgainTable.setVisible(false);
            gameTable.setVisible(false);
            gameHudGroup.setVisible(false);
            introTable.setVisible(true);
            if (exitTable != null) exitTable.setVisible(true);
        }
    });
    playAgainTable.add(yesBtn).width(180).height(76).padRight(30);
    playAgainTable.add(noBtn).width(180).height(76);
    
    stage.addActor(playAgainTable);
    playAgainTable.setVisible(false); 
}

private void createGameUI() {
    gameTable = new Table();
    gameTable.setFillParent(true);
    stage.addActor(gameTable);
    
    Label.LabelStyle customLabelStyle = new Label.LabelStyle();
    customLabelStyle.font = font;

    timeLabel = new Label("", customLabelStyle); 
    p2TimeLabel = new Label("", customLabelStyle); 
    goalLabel = new Label("", customLabelStyle);
    playerLabel = new Label("", customLabelStyle);
    aiLabel = new Label("", customLabelStyle);
    difficultyLabel = new Label("", customLabelStyle);
    statusLabel = new Label("", customLabelStyle);
    comboLabel = new Label("", customLabelStyle);
    p2ComboLabel = new Label("", customLabelStyle);

    timeLabel.setFontScale(1.25f, 1.225f);
    p2TimeLabel.setFontScale(1.25f, 1.225f);
    goalLabel.setFontScale(1.45f, 1.45f);
    difficultyLabel.setFontScale(1.57f, 1.68f);
    playerLabel.setFontScale(1.25f, 1.225f);
    aiLabel.setFontScale(1.25f, 1.225f);
    comboLabel.setFontScale(1.23f, 1.2f);
    p2ComboLabel.setFontScale(1.23f, 1.2f);

    playerLabel.setColor(Color.RED);
    aiLabel.setColor(Color.YELLOW);
    difficultyLabel.setColor(Color.WHITE);

    timeLabel.setPosition(1075, 570);
    p2TimeLabel.setPosition(1075, 520);
    goalLabel.setPosition(872, 626);
    difficultyLabel.setPosition(315, 30);
    playerLabel.setPosition(832, 570); 
    aiLabel.setPosition(832, 520);     
    comboLabel.setPosition(1282, 570);
    p2ComboLabel.setPosition(1282, 520);
    statusLabel.setWidth(728f);
    statusLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    statusLabel.setPosition(0,110);

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
    gameHudGroup.addActor(p2ComboLabel); 
    gameHudGroup.setVisible(false);

    statusImage = new Image(p1TurnTex);

    statusScale = 0.5f;          
    float statusX = 1050f;       
    float statusY = 380f;         

    statusImage.setPosition(statusX, statusY);
    statusImage.setSize(p1TurnTex.getWidth() * statusScale, p1TurnTex.getHeight() * statusScale);

    gameHudGroup.addActor(statusImage);
    statusLabel.setVisible(false);

    clImage = new Image(p1TurnClTex); 

    clScale = 0.35f;       
    float clX = 930f;         
    float clY = 71f;         
    
    clImage.setPosition(clX, clY);
    clImage.setSize(p1TurnClTex.getWidth() * clScale, p1TurnClTex.getHeight() * clScale);

    gameHudGroup.addActor(clImage);

    expImage = new Image(expTextures[0]);

    expImage.setPosition(statusImage.getX(), statusImage.getY());
    expImage.setSize(
        expTextures[0].getWidth() * statusScale,
        expTextures[0].getHeight() * statusScale
    );

    expImage.setVisible(false);
    gameHudGroup.addActor(expImage);

    pauseTriggerBtn = new Image(new Texture(Gdx.files.internal("pausebutton.png")));
    pauseTriggerBtn.setSize(70, 70); 
    pauseTriggerBtn.setPosition(VIRTUAL_WIDTH - 75, VIRTUAL_HEIGHT - 75); 
    pauseTriggerBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (gameState == GameState.PLAYING || 
                        gameState == GameState.PLAYER_WIN || 
                        gameState == GameState.AI_WIN || 
                        gameState == GameState.TIME_UP) {
                        
                        stateBeforePause = gameState; 
                        gameState = GameState.PAUSED;
                        pauseTable.setVisible(true); 
                        pauseTable.toFront();   
                        pauseExitTable.setVisible(true);
                        pauseExitTable.toFront();
                        
                        if (currentDifficulty == Difficulty.EASY) updateDifficultyGlowPause(easyBtnPause);
                        else if (currentDifficulty == Difficulty.MEDIUM) updateDifficultyGlowPause(medBtnPause);
                        else if (currentDifficulty == Difficulty.HARD) updateDifficultyGlowPause(hardBtnPause);
                    }
                }
            });
    gameHudGroup.addActor(pauseTriggerBtn);
}

private void updateStatusImage() {
    if (statusImage == null || clImage == null || !gameHudGroup.isVisible()) return;
    
    Texture targetTex = null;
    Texture targetClTex = null; 

    
    if (gameState == GameState.PLAYING) {
        if (currentPlayer == 1) {
            targetTex = p1TurnTex;
            targetClTex = p1TurnClTex;
        } else if (currentPlayer == 2) {
            targetTex = isTwoPlayer ? p2TurnTex : aiTurnTex;
            targetClTex = isTwoPlayer ? p2TurnClTex : aiTurnClTex;
        }
    } 
    
    else if (gameState == GameState.PLAYER_WIN) {
        targetTex = isTwoPlayer ? p1WinTex : winTex; 
        targetClTex = winClTex;
    } 
    
    else if (gameState == GameState.AI_WIN) {
        targetTex = isTwoPlayer ? p2WinTex : lossTex;
        targetClTex = isTwoPlayer ? winClTex : lossClTex; 
    } 
    else if (gameState == GameState.TIME_UP) {
        targetTex = timeTex;
        targetClTex = timesUpClTex; 
    }

    if (targetTex != null) {
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable currentDrawable = 
            (com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable) statusImage.getDrawable();
            
        if (currentDrawable == null || currentDrawable.getRegion().getTexture() != targetTex) {
            statusImage.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(targetTex));
            statusImage.setSize(targetTex.getWidth() * statusScale, targetTex.getHeight() * statusScale);
        }
    }

    if (targetClTex != null) {
        com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable currentClDrawable = 
            (com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable) clImage.getDrawable();
            
        if (currentClDrawable == null || currentClDrawable.getRegion().getTexture() != targetClTex) {
            clImage.setDrawable(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(targetClTex));
            clImage.setSize(targetClTex.getWidth() * clScale, targetClTex.getHeight() * clScale);
        }
    }
}

private void startGame() {
    grid = new int[ROWS][COLS];
    gameState = GameState.PLAYING;

    float startTime;
    if (scoreGoal == 1000)      startTime = 120f; 
    else if (scoreGoal == 3000) startTime = 100f; 
    else                        startTime = 80f;  

    p1TimeRemaining = startTime;
    p2TimeRemaining = startTime;

    currentPlayer = 1;
    aiNeedsToMove = false;
    aiTimer = 0f;
    aiScore = 0;
    score = 0;
    comboMultiplier = 0;

    p1PowerUp = PowerUp.NONE;
    p2PowerUp = PowerUp.NONE;
    p1LinesCleared = 0;
    p2LinesCleared = 0;
    p1TimerStopped = false;
    p2TimerStopped = false;
    activeTargeting = PowerUp.NONE;
    isShuffling1 = false;
    isShuffling2 = false;
    shuffleTimer1 = 0f;
    shuffleTimer2 = 0f;
    statusLabel.setVisible(false);

    
    introTable.setVisible(false);
    modeTable.setVisible(false); 
    gameTable.setVisible(true);
    gameHudGroup.setVisible(true);
}

@Override
public void resize(int width, int height) {
    viewport.update(width, height, true); 
}

@Override
public void render() {
    float deltaTime = Gdx.graphics.getDeltaTime();
    
    if (gameState == GameState.PLAYING && showExp) {
        expTimer -= deltaTime;
        if (expTimer <= 0f) {
            expImage.setVisible(false);
            showExp = false;
        }
    }

    updateStatusImage();
    
float scale = 2.5f;
    float offsetX = 0f;
    float offsetY = 0f;

    if (gameState == GameState.TUTORIAL) {
        scale = 1.75f;  
        offsetX = 450f; 
        offsetY = 5f; 
    }

    float frameWidth = frame.getWidth() * scale;
    float frameHeight = frame.getHeight() * scale;
    float frameX = 60f + offsetX; 
    float frameY = 50f + offsetY;
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

    if (gameState == GameState.INTRO) {
    } else if (gameState == GameState.PLAYING) {
        if (currentPlayer == 1) {
            if (!p1TimerStopped) { 
                p1TimeRemaining -= deltaTime;
                if (p1TimeRemaining <= 0) {
                    p1TimeRemaining = 0;
                    gameState = GameState.TIME_UP; 
                    loseSound.play(masterVolume); 
                }
            }
        } else if (currentPlayer == 2 && isTwoPlayer) {
            if (!p2TimerStopped) {
            p2TimeRemaining -= deltaTime; 
            if (p2TimeRemaining <= 0) {
                p2TimeRemaining = 0;
                gameState = GameState.TIME_UP;
                winSound.play(masterVolume);   
            }
        }
    }

    if (currentPlayer != 0) {
        if (currentPlayer == 1 || (isTwoPlayer && currentPlayer == 2)) {
            if (Gdx.input.justTouched()) {
                    com.badlogic.gdx.math.Vector2 touch = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
                    viewport.unproject(touch);
                    
                    
                    float pW = 700 * powerUpScale;
                    if (currentPlayer == 1 && p1PowerUp != PowerUp.NONE && touch.x >= p1PowerUpX && touch.x <= p1PowerUpX + pW && touch.y >= p1PowerUpY && touch.y <= p1PowerUpY + pW) {
                        activatePowerUp(p1PowerUp, 1);
                        p1PowerUp = PowerUp.NONE;
                    } 
                    else if (currentPlayer == 2 && isTwoPlayer && p2PowerUp != PowerUp.NONE && touch.x >= p2PowerUpX && touch.x <= p2PowerUpX + pW && touch.y >= p2PowerUpY && touch.y <= p2PowerUpY + pW) {
                        activatePowerUp(p2PowerUp, 2);
                        p2PowerUp = PowerUp.NONE;
                    }
                    
                    
                    else if (activeTargeting != PowerUp.NONE) {
                        if (touch.x >= innerX && touch.x <= innerX + innerW) {
                            int col = (int) ((touch.x - innerX) / cellWidth);
                            int row = (int) ((innerY + innerH - touch.y) / cellHeight); 
                            
                            if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
                                executeTargeting(col, row);
                            }
                        }
                    }
                    
                    
                    else if (touch.x >= innerX && touch.x <= innerX + innerW && activeTargeting == PowerUp.NONE) {
                        int col = (int) ((touch.x - innerX) / cellWidth);
                        executeMove(col); 
                    }
                }
        }
    }

    if (aiNeedsToMove) {
        aiTimer += deltaTime;
        if (aiTimer >= aiMoveDelay) {
            makeAIMove();
            aiTimer = 0f;
            aiNeedsToMove = false;
        }
    }
}

Gdx.gl.glClearColor(0, 0, 0, 1);
Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

viewport.apply();
if (shakeTimer > 0) {
    shakeTimer -= deltaTime;
    float currentShakeX = (random.nextFloat() - 0.5f) * 2 * shakeIntensity;
    float currentShakeY = (random.nextFloat() - 0.5f) * 2 * shakeIntensity;
    viewport.getCamera().translate(currentShakeX, currentShakeY, 0);
} else {
    viewport.getCamera().position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
}
viewport.getCamera().update();
batch.setProjectionMatrix(viewport.getCamera().combined);

float boardDrawW = innerW * (board.getWidth() / 112f);
float boardDrawH = innerH * (board.getHeight() / 96f);
float boardDrawX = innerX - (boardDrawW * (63f / 240f));
float boardDrawY = innerY - (boardDrawH * (97f / 256f));

batch.begin();
if (gameState == GameState.INTRO) {
    batch.draw(menuBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
} else if (gameState == GameState.MODE_SELECT) {
    batch.draw(modeBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
} else if (gameState == GameState.SETTINGS) { 
    batch.draw(settingsBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
} else if (gameState == GameState.TUTORIAL) {
    
    
    
    if (tutorialBg != null) batch.draw(tutorialBg, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
} else {
    
    Texture activeBorder = isTwoPlayer ? border2p : border;
    batch.draw(activeBorder, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
}

if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
    float scalee = 0.98f;
    float scaledW = boardDrawW * scalee;
    float scaledH = boardDrawH * scalee;
    float scaledX = boardDrawX + (boardDrawW - scaledW) / 2f;
    float scaledY = (boardDrawY + (boardDrawH - scaledH) / 2f) + 1f;
    batch.draw(background, scaledX, scaledY, scaledW, scaledH);
}

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
            popSound.play(masterVolume * 0.5f);
            grid[p.row][p.col] = p.player;
            activeFallingPieces.remove(i);
            finalizeTurn(p.col, p.player);
        }
    }

    if (gameState == GameState.TUTORIAL) {
        ghostTimer += deltaTime;
        
        
        if (ghostTimer >= ghostMoveDelay && activeFallingPieces.isEmpty()) {
            
            
            int[] currentMoves;
            if (tutorialStage == 0) currentMoves = ghostMovesH;
            else if (tutorialStage == 1) currentMoves = ghostMovesV;
            else currentMoves = ghostMovesD;
            
            if (ghostMoveIndex < currentMoves.length) {
                
                executeMove(currentMoves[ghostMoveIndex]);
                ghostMoveIndex++;
                ghostTimer = 0f;
            } else if (ghostTimer >= ghostMoveDelay + 2.5f) {
                
                tutorialStage++;
                if (tutorialStage > 2) tutorialStage = 0; 
                
                
                grid = new int[ROWS][COLS];
                activeFallingPieces.clear();
                activeSweeps.clear();
                blastEffects.clear();
                ghostMoveIndex = 0;
                ghostTimer = 0f;
                currentPlayer = 1;
            }
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

if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
    batch.draw(board, boardDrawX, boardDrawY, boardDrawW, boardDrawH);
}

batch.end();

Gdx.gl.glEnable(GL20.GL_BLEND);
Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); 

shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

for (int i = activeSweeps.size() - 1; i >= 0; i--) {
    SweepEffect s = activeSweeps.get(i);
    s.update(deltaTime);
    shapeRenderer.setColor(1f, 1f, 1f, s.alpha); 
    shapeRenderer.rect(s.x, s.y, s.width, s.height);
    if (s.alpha <= 0) activeSweeps.remove(i);
}

for (int i = blastEffects.size() - 1; i >= 0; i--) {
    BlastEffect b = blastEffects.get(i);
    b.update(deltaTime);
    shapeRenderer.setColor(1f, 0.8f, 0f, b.alpha); 
    shapeRenderer.circle(b.x, b.y, b.size / 2);
    if (b.isFinished()) blastEffects.remove(i);
}

shapeRenderer.end();
Gdx.gl.glDisable(GL20.GL_BLEND);
batch.begin();

if (gameState != GameState.INTRO && gameState != GameState.MODE_SELECT && gameState != GameState.SETTINGS) {
    batch.draw(frame, frameX, frameY, frameWidth, frameHeight);
}

if (gameState == GameState.PLAYING && (currentPlayer == 1 || (isTwoPlayer && currentPlayer == 2))) {
    com.badlogic.gdx.math.Vector2 mouse = new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY());
    viewport.unproject(mouse);

    if (mouse.x >= innerX && mouse.x <= innerX + innerW) {
        int hoverCol = (int) ((mouse.x - innerX) / cellWidth);
        float size = Math.min(cellWidth, cellHeight) * 0.99f;
        float previewX = innerX + hoverCol * cellWidth + (cellWidth - size) / 1f + 2.2f;
        float previewY = innerY + ROWS * cellHeight + 10;

        
        Texture previewTex = (currentPlayer == 1) ? yellowPiece : redPiece;

        if (grid[0][hoverCol] == 0) {
            batch.setColor(1, 1, 1, 0.5f); 
        } else {
            batch.setColor(1, 0, 0, 0.7f); 
        }

        batch.draw(previewTex, previewX, previewY, size, size);
        batch.setColor(Color.WHITE);
    }
}

if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
    float pW = 700 * powerUpScale;
    float pH = 700 * powerUpScale;

    
    if (isShuffling1) {
        
        if (gameState == GameState.PLAYING) {
            shuffleTimer1 -= deltaTime;
            if ((int)(shuffleTimer1 * 10) % 2 == 0) shuffleIndex1 = (shuffleIndex1 + 1) % 5;
        }
        
        if (shuffleTimer1 > 0) {
            batch.draw(powerUpList[shuffleIndex1], p1PowerUpX, p1PowerUpY, pW, pH);
        } else {
            
            isShuffling1 = false;
            p1PowerUp = PowerUp.values()[random.nextInt(5) + 1]; 
        }
    } else if (p1PowerUp != PowerUp.NONE) {
        batch.draw(getTextureForPowerUp(p1PowerUp), p1PowerUpX, p1PowerUpY, pW, pH);
    }

    
    if (isShuffling2) {
        
        if (gameState == GameState.PLAYING) {
            shuffleTimer2 -= deltaTime;
            if ((int)(shuffleTimer2 * 10) % 2 == 0) shuffleIndex2 = (shuffleIndex2 + 1) % 5;
        }
        
        if (shuffleTimer2 > 0) {
            batch.draw(powerUpList[shuffleIndex2], p2PowerUpX, p2PowerUpY, pW, pH);
        } else {
            isShuffling2 = false;
            p2PowerUp = PowerUp.values()[random.nextInt(5) + 1];
        }
    } else if (p2PowerUp != PowerUp.NONE) {
        batch.draw(getTextureForPowerUp(p2PowerUp), p2PowerUpX, p2PowerUpY, pW, pH);
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
    return false;
}


void displayEndGameMessage() {
    if (gameState == GameState.PLAYER_WIN) {
    } else if (gameState == GameState.AI_WIN) {
    } else if (gameState == GameState.TIME_UP) {
    }
}

void makeAIMove() {
    if (p2PowerUp != PowerUp.NONE && !isShuffling2) {
        PowerUp usedPowerUp = p2PowerUp;
        activatePowerUp(p2PowerUp, 2);
        p2PowerUp = PowerUp.NONE;
        
        if (usedPowerUp == PowerUp.BOMB) {
            
            java.util.List<int[]> validTargets = new java.util.ArrayList<>();
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (grid[r][c] != 0) validTargets.add(new int[]{r, c});
                }
            }
            if (!validTargets.isEmpty()) {
                int[] target = validTargets.get(random.nextInt(validTargets.size()));
                executeTargeting(target[1], target[0]);
            } else {
                activeTargeting = PowerUp.NONE;
            }
            return;
            
    } else if (usedPowerUp == PowerUp.BULLET) {

            if (bulletIsRow) {
                executeTargeting(0, random.nextInt(ROWS));
            } else {
                executeTargeting(random.nextInt(COLS), 0);
            }
            return; 
        }
    }
    
    int chosenCol = -1;

    if (currentDifficulty == Difficulty.HARD) {
        chosenCol = findWinningMove(2);
        if (chosenCol == -1) chosenCol = findWinningMove(1);

        if (chosenCol == -1) {
            List<Integer> safeCols = new ArrayList<>();
            
            for (int c = 0; c < COLS; c++) {
                if (grid[0][c] == 0) {
                    int targetRow = -1;
                    for (int r = ROWS - 1; r >= 0; r--) {
                        if (grid[r][c] == 0) { targetRow = r; break; }
                    }

                    grid[targetRow][c] = 2; 
                    boolean givesWinToPlayer = false;

                    if (targetRow > 0) {
                        grid[targetRow - 1][c] = 1;
                        givesWinToPlayer = checkWin(1);
                        grid[targetRow - 1][c] = 0;
                    }
                    
                    grid[targetRow][c] = 0;
                    
                    if (!givesWinToPlayer) safeCols.add(c);
                }
            }
            
            if (!safeCols.isEmpty()) {
                int bestCol = safeCols.get(0);
                int closestDist = 999;
                
                for (int c : safeCols) {
                    int dist = Math.abs(c - 3);
                    if (dist < closestDist || (dist == closestDist && random.nextBoolean())) {
                        closestDist = dist;
                        bestCol = c;
                    }
                }
                chosenCol = bestCol;
            }
        }
    } 

    if (currentDifficulty == Difficulty.MEDIUM && chosenCol == -1) {
        if (random.nextFloat() > 0.5f) chosenCol = findWinningMove(1); 
    }

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

int findWinningMove(int player) {
    for (int c = 0; c < COLS; c++) {
        
        int targetRow = -1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (grid[r][c] == 0) {
                targetRow = r;
                break;
            }
        }
        
        if (targetRow != -1) {
            grid[targetRow][c] = player;
            boolean wins = checkWin(player);
            grid[targetRow][c] = 0;
            if (wins) return c;
        }
    }
    return -1;
}

int checkAndRemoveLines() {
    java.util.Set<Integer> rowsToClear = new java.util.HashSet<>();
    java.util.Set<Integer> colsToClear = new java.util.HashSet<>();
    java.util.Set<Integer> diag1ToClear = new java.util.HashSet<>(); 
    java.util.Set<Integer> diag2ToClear = new java.util.HashSet<>(); 

    
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c <= COLS - 4; c++) {
            if (grid[r][c] != 0 && grid[r][c] == grid[r][c+1] && grid[r][c] == grid[r][c+2] && grid[r][c] == grid[r][c+3]) rowsToClear.add(r);
        }
    }
    for (int c = 0; c < COLS; c++) {
        for (int r = 0; r <= ROWS - 4; r++) {
            if (grid[r][c] != 0 && grid[r][c] == grid[r+1][c] && grid[r][c] == grid[r+2][c] && grid[r][c] == grid[r+3][c]) colsToClear.add(c);
        }
    }
    for (int r = 0; r <= ROWS - 4; r++) {
        for (int c = 0; c <= COLS - 4; c++) {
            if (grid[r][c] != 0 && grid[r][c] == grid[r+1][c+1] && grid[r][c] == grid[r+2][c+2] && grid[r][c] == grid[r+3][c+3]) diag1ToClear.add(r - c);
        }
    }
    for (int r = 3; r < ROWS; r++) {
        for (int c = 0; c <= COLS - 4; c++) {
            if (grid[r][c] != 0 && grid[r][c] == grid[r-1][c+1] && grid[r][c] == grid[r-2][c+2] && grid[r][c] == grid[r-3][c+3]) diag2ToClear.add(r + c);
        }
    }

    
    int linesFound = rowsToClear.size() + colsToClear.size() + diag1ToClear.size() + diag2ToClear.size();

    
    if (linesFound > 0) {
        boolean[][] toDestroy = new boolean[ROWS][COLS];

        for (int r : rowsToClear) {
            float sweepY = innerY + (ROWS - 1 - r) * cellHeight;
            activeSweeps.add(new SweepEffect(innerX, sweepY, innerW, cellHeight));
            for (int i = 0; i < COLS; i++) toDestroy[r][i] = true;
        }
        for (int c : colsToClear) {
            float sweepX = innerX + c * cellWidth;
            activeSweeps.add(new SweepEffect(sweepX, innerY, cellWidth, innerH));
            for (int i = 0; i < ROWS; i++) toDestroy[i][c] = true;
        }
        for (int d1 : diag1ToClear) {
            for (int r = 0; r < ROWS; r++) {
                int c = r - d1;
                if (c >= 0 && c < COLS) toDestroy[r][c] = true;
            }
        }
        for (int d2 : diag2ToClear) {
            for (int r = 0; r < ROWS; r++) {
                int c = d2 - r;
                if (c >= 0 && c < COLS) toDestroy[r][c] = true;
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (toDestroy[r][c] && grid[r][c] != 0) {
                    createBlastEffect(r, c);
                    grid[r][c] = 0;
                }
            }
        }
    }
    return linesFound;
}

void updateAndDrawUI() {
    if (gameState == GameState.INTRO) return;
    
    pauseTriggerBtn.setVisible(gameState == GameState.PLAYING || 
                                gameState == GameState.PLAYER_WIN || 
                                gameState == GameState.AI_WIN || 
                                gameState == GameState.TIME_UP);

    int m1 = (int) (p1TimeRemaining / 60);
    int s1 = (int) (p1TimeRemaining % 60);
    timeLabel.setText(String.format("%d:%02d", m1, s1));

    if (p1TimeRemaining < 10 && p1TimeRemaining > 0) {
        float alpha = 0.5f + (float)Math.abs(Math.sin(Gdx.graphics.getFrameId() * 0.2f)) * 0.5f;
        timeLabel.setColor(1, 0, 0, alpha); 
    } else {
        timeLabel.setColor(Color.WHITE);
    }

    int m2 = (int) (p2TimeRemaining / 60);
    int s2 = (int) (p2TimeRemaining % 60);
    p2TimeLabel.setText(String.format("%d:%02d", m2, s2));
    p2TimeLabel.setVisible(isTwoPlayer); 

    if (p2TimeRemaining < 10 && p2TimeRemaining > 0 && isTwoPlayer) {
        float alpha = 0.5f + (float)Math.abs(Math.sin(Gdx.graphics.getFrameId() * 0.2f)) * 0.5f;
        p2TimeLabel.setColor(1, 0, 0, alpha);
    } else {
        p2TimeLabel.setColor(Color.WHITE);
    }

if (isTwoPlayer) {
    playerLabel.setText("" + score);
    aiLabel.setText("" + aiScore);
    aiLabel.setColor(Color.YELLOW); 
} else {
    playerLabel.setText("" + score);
    aiLabel.setText("" + aiScore);
    aiLabel.setColor(Color.YELLOW);
}

goalLabel.setText("" + scoreGoal);
difficultyLabel.setText("" + currentDifficulty);

comboLabel.setVisible(true);           
p2ComboLabel.setVisible(isTwoPlayer);      
comboLabel.setText("x" + p1Combo);

if (isTwoPlayer) {
    p2ComboLabel.setText("x" + p2Combo);
}

float baseScaleX = 1.23f;
float baseScaleY = 1.2f; 
float p1Growth = p1Combo * 0.125f;

comboLabel.setFontScale(baseScaleX + p1Growth, baseScaleY + p1Growth);

if (p1Combo >= 5) comboLabel.setColor(Color.RED);
else if (p1Combo >= 3) comboLabel.setColor(Color.ORANGE);
else if (p1Combo > 1) comboLabel.setColor(Color.GOLD);
else comboLabel.setColor(Color.WHITE);

if (isTwoPlayer) {
    float p2Growth = p2Combo * 0.15f;
    p2ComboLabel.setFontScale(baseScaleX + p2Growth, baseScaleY + p2Growth);

    if (p2Combo >= 5) p2ComboLabel.setColor(Color.RED);
    else if (p2Combo >= 3) p2ComboLabel.setColor(Color.ORANGE);
    else if (p2Combo > 1) p2ComboLabel.setColor(Color.GOLD);
    else p2ComboLabel.setColor(Color.WHITE);
}
    if (exitTable != null) {
        exitTable.setVisible(gameState == GameState.INTRO || 
                            gameState == GameState.MODE_SELECT || 
                            gameState == GameState.SETTINGS);
    }

    if (gameState == GameState.PLAYING) {
        if (isTwoPlayer) {
        } else {
        }
    } else if (gameState == GameState.PLAYER_WIN || gameState == GameState.AI_WIN || gameState == GameState.TIME_UP) {
        displayEndGameMessage();
        playAgainTable.setVisible(true);
    } else {
        statusLabel.setText(""); 
        playAgainTable.setVisible(false); 
    }
}

@Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        stage.dispose();
        skin.dispose();
        board.dispose();
        frame.dispose();
        border.dispose();
        border2p.dispose();
        background.dispose();
        yellowPiece.dispose();
        redPiece.dispose();
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
        settingsBg.dispose();
        volumeImg.dispose();
        bulletTex.dispose();
        starTex.dispose();
        eightTex.dispose();
        coinTex.dispose();
        bombTex.dispose();
        if (pauseBtnImg != null) pauseBtnImg.dispose();
        if (pausedBg != null) pausedBg.dispose();
        if (playAgainBg != null) playAgainBg.dispose();
        if (yesImg != null) yesImg.dispose();
        if (noImg != null) noImg.dispose();
        if (resumeImg != null) resumeImg.dispose();
        if (restartImg != null) restartImg.dispose();
        if (settingsPauseImg != null) settingsPauseImg.dispose();
        if (p1TurnTex != null) p1TurnTex.dispose();
        if (p2TurnTex != null) p2TurnTex.dispose();
        if (aiTurnTex != null) aiTurnTex.dispose();
        if (winTex != null) winTex.dispose();
        if (lossTex != null) lossTex.dispose();
        if (p1WinTex != null) p1WinTex.dispose();
        if (p2WinTex != null) p2WinTex.dispose();
        if (p1LostTex != null) p1LostTex.dispose();
        if (p2LostTex != null) p2LostTex.dispose();
        if (timeTex != null) timeTex.dispose();
        if (p1TurnClTex != null) p1TurnClTex.dispose();
        if (p2TurnClTex != null) p2TurnClTex.dispose();
        if (aiTurnClTex != null) aiTurnClTex.dispose();
        if (winClTex != null) winClTex.dispose();
        if (lossClTex != null) lossClTex.dispose();
        if (timesUpClTex != null) timesUpClTex.dispose();
        if (backImg != null) backImg.dispose();
        if (expTextures != null) {
            for (Texture t : expTextures) if (t != null) t.dispose();
        }
        if (backgroundMusic != null) backgroundMusic.dispose();
        if (popSound != null) popSound.dispose();
        if (winSound != null) winSound.dispose();
        if (loseSound != null) loseSound.dispose();
        if (blast1 != null) blast1.dispose();
        if (blast2 != null) blast2.dispose();
        if (tutCharacters != null) {
            for (Texture t : tutCharacters) t.dispose();
        }
        if (tutTextBoxes != null) {
            for (Texture t : tutTextBoxes) t.dispose();
        }
        if (tutorialBg != null) tutorialBg.dispose();
    }

void bubbleSortColumn(int col) {
    for (int pass = 0; pass < ROWS; pass++) {
        for (int r = 0; r < ROWS - 1; r++) {
            
            if (grid[r][col] != 0 && grid[r + 1][col] == 0) {
                
                int temp = grid[r][col];
                grid[r][col] = grid[r + 1][col];
                grid[r + 1][col] = temp;
            }
        }
    }
}

void finalizeTurn(int col, int playerID) {
    p1TimerStopped = false; 
    p2TimerStopped = false; 
    comboMultiplier = 0; 
    statusLabel.setVisible(false);
    int linesCleared;
    
    
    while ((linesCleared = checkAndRemoveLines()) > 0) {
        triggerExplosion();
        comboMultiplier += linesCleared; 

        if (playerID == 1) {
            int previousClears = p1LinesCleared;
            p1LinesCleared += linesCleared;
            if ((p1LinesCleared / 2) > (previousClears / 2) && p1PowerUp == PowerUp.NONE && !isShuffling1) {
                isShuffling1 = true;
                shuffleTimer1 = 2.0f; 
            }
        } else {
            int previousClears = p2LinesCleared;
            p2LinesCleared += linesCleared;
            if ((p2LinesCleared / 2) > (previousClears / 2) && p2PowerUp == PowerUp.NONE && !isShuffling2) {
                isShuffling2 = true;
                shuffleTimer2 = 2.0f;
            }
        }

        float bonus = (scoreGoal < 3000) ? 8f : 5f; 
        bonus *= linesCleared; 
        if (playerID == 1) p1TimeRemaining += bonus;
        else p2TimeRemaining += bonus;

        int points = (100 * (int)Math.pow(2, comboMultiplier - 1));
        if (playerID == 1) score += points;
        else aiScore += points;
        
        float pitch = 0.8f + (comboMultiplier * 0.2f);
        if (linesCleared >= 2) blast2.play(masterVolume * 0.5f, pitch, 0);
        else blast1.play(masterVolume * 0.5f, pitch, 0);

        shakeTimer = 0.2f; 
        shakeIntensity = 3f + (comboMultiplier * 4f); 

        for(int c = 0; c < COLS; c++) bubbleSortColumn(c);
    }

    if (playerID == 1) p1Combo = Math.max(1, comboMultiplier);
    else p2Combo = Math.max(1, comboMultiplier);

    if (gameState == GameState.TUTORIAL) {
        currentPlayer = (playerID == 1) ? 2 : 1;
        return;
    }

    if (score >= scoreGoal) {
        gameState = GameState.PLAYER_WIN;
        winSound.play(masterVolume);
    } else if (aiScore >= scoreGoal) {
        gameState = GameState.AI_WIN;
        if (isTwoPlayer) winSound.play(masterVolume); 
        else loseSound.play(masterVolume);         
    } else if (grid[0][col] != 0) {
        gameState = (playerID == 1) ? GameState.AI_WIN : GameState.PLAYER_WIN;
        if (gameState == GameState.PLAYER_WIN) winSound.play(masterVolume);
        else {
            if (isTwoPlayer) winSound.play(masterVolume); 
            else loseSound.play(masterVolume);          
        }
    } else {
        currentPlayer = (playerID == 1) ? 2 : 1;
        if (!isTwoPlayer && currentPlayer == 2) {
            aiNeedsToMove = true;
            aiTimer = 0f;
        }
    }
}

void triggerExplosion() {
    int rand = new Random().nextInt(5);

    expImage.setDrawable(
        new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(expTextures[rand])
    );

    float expX = 900f; 
    float expY = 380f;

    expImage.setPosition(expX, expY);
    expImage.setSize(
        expTextures[rand].getWidth() * statusScale,
        expTextures[rand].getHeight() * statusScale
    );

    expImage.clearActions();
    expImage.getColor().a = 1f;
    expImage.setScale(0f);
    expImage.setVisible(true);
    
    expTimer = 1f; 
    showExp = true;
    
    expImage.addAction(
        com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            Actions.scaleTo(1.4f, 1.4f, 0.08f, Interpolation.swingOut),
            Actions.scaleTo(1f, 1f, 0.08f, Interpolation.sine),
            Actions.delay(0.6f),
            Actions.fadeOut(0.24f)
        )
    );
}

void executeMove(int col) {
    if ((gameState != GameState.PLAYING && gameState != GameState.TUTORIAL) || currentPlayer == 0) return;

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
    int[][] directions = {
        {0, 1},   
        {1, 0},   
        {1, 1},   
        {-1, 1}   
    };

    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {

            if (grid[r][c] != player) continue;

            
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

void activatePowerUp(PowerUp type, int playerID) {
    if (type == PowerUp.COIN) {
        int coinValue = 1000;
        if (currentDifficulty == Difficulty.EASY) coinValue = 250;
        else if (currentDifficulty == Difficulty.MEDIUM) coinValue = 500;
        
        if (playerID == 1) score += coinValue; else aiScore += coinValue;
        popSound.play(masterVolume); 
        
        
        statusLabel.setText((playerID == 1 || isTwoPlayer) ? "+" + coinValue + " Points!" : "AI gained points!");
        statusLabel.setVisible(true);
    }
    else if (type == PowerUp.EIGHT) {
        if (playerID == 1) p1TimeRemaining += 8f; else p2TimeRemaining += 8f;
        popSound.play(masterVolume); 
        
        
        statusLabel.setText("+8 Seconds!");
        statusLabel.setVisible(true);
    } 
    else if (type == PowerUp.STAR) {
        if (playerID == 1) p1TimerStopped = true; else p2TimerStopped = true;
        popSound.play(masterVolume); 
        
        
        statusLabel.setText("Time Frozen!");
        statusLabel.setVisible(true);
    }
    else if (type == PowerUp.BOMB || type == PowerUp.BULLET) {
        
        boolean isEmpty = true;
        for(int r = 0; r < ROWS; r++) {
            for(int c = 0; c < COLS; c++) {
                if (grid[r][c] != 0) isEmpty = false;
            }
        }
    if (isEmpty) {
            
            if (playerID == 1) p1PowerUp = type; else p2PowerUp = type; 
            return; 
        }

        activeTargeting = type;
        if (type == PowerUp.BOMB) {
            statusLabel.setText("Select piece to destroy!");
        } else {
            bulletIsRow = random.nextBoolean(); 
            statusLabel.setText(bulletIsRow ? "Select row to clear!" : "Select column to clear!");
        }
        statusLabel.setVisible(true);
    }
}

void executeTargeting(int col, int row) {
    if (activeTargeting == PowerUp.BOMB) {
        if (grid[row][col] != 0) {
            createBlastEffect(row, col);
            blast2.play(masterVolume);
            grid[row][col] = 0; 
            for(int c = 0; c < COLS; c++) bubbleSortColumn(c); 
            finalizeTurn(col, currentPlayer); 
            activeTargeting = PowerUp.NONE;
            statusLabel.setVisible(false);
        }
    } 
    else if (activeTargeting == PowerUp.BULLET) {
        if (bulletIsRow) {
            
            activeSweeps.add(new SweepEffect(innerX, innerY + (ROWS - 1 - row) * cellHeight, innerW, cellHeight));
            blast1.play(masterVolume);
            for(int c = 0; c < COLS; c++) {
                if (grid[row][c] != 0) createBlastEffect(row, c);
                grid[row][c] = 0;
            }
        } else {
            
            activeSweeps.add(new SweepEffect(innerX + col * cellWidth, innerY, cellWidth, innerH));
            blast1.play(masterVolume);
            for(int r = 0; r < ROWS; r++) {
                if (grid[r][col] != 0) createBlastEffect(r, col);
                grid[r][col] = 0;
            }
        }
        for(int c = 0; c < COLS; c++) bubbleSortColumn(c);
        finalizeTurn(col, currentPlayer);
        activeTargeting = PowerUp.NONE;
        statusLabel.setVisible(false);
    }
}

Texture getTextureForPowerUp(PowerUp p) {
    if (p == PowerUp.BULLET) return bulletTex;
    if (p == PowerUp.STAR) return starTex;
    if (p == PowerUp.EIGHT) return eightTex;
    if (p == PowerUp.COIN) return coinTex;
    return bombTex;
}

void createBlastEffect(int r, int c) {
    float size = Math.min(cellWidth, cellHeight);
    float pieceX = innerX + c * cellWidth + (cellWidth - size) / 2f;
    float pieceY = innerY + (ROWS - 1 - r) * cellHeight + (cellHeight - size) / 2f;
    blastEffects.add(new BlastEffect(pieceX + size / 2, pieceY + size / 2, size));
    }
}