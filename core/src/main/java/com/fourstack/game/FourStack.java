package com.fourstack.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class FourStack extends ApplicationAdapter {

    int ROWS = 6;
    int COLS = 7;

    int[][] grid = new int[ROWS][COLS];
    // 0 = empty, 1 = yellow, 2 = red

    int currentPlayer = 1; // 1 = yellow goes first, 2 = red

    SpriteBatch batch;
    Texture board;
    Texture frame;
    Texture yellowPiece;
    Texture redPiece;

    @Override
    public void create() {
        batch = new SpriteBatch();
        board = new Texture("board.png");
        frame = new Texture("frame.png");
        yellowPiece = new Texture("piece_yellow.png");
        redPiece = new Texture("piece_red.png");
    }

    @Override
    public void render() {
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

        // 2. Handle input
        if (Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (mouseX >= boardX && mouseX <= boardX + boardWidth &&
                    mouseY >= boardY && mouseY <= boardY + boardHeight) {

                float cellWidth = boardWidth / COLS;
                int col = (int) ((mouseX - boardX) / cellWidth);
                dropPiece(col, currentPlayer);
                currentPlayer = (currentPlayer == 1) ? 2 : 1; // switch turn
            }
        }

        // 3. Draw pieces (always use boardX/boardY/boardWidth/boardHeight for
        // alignment)
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

        batch.end();
    }

    void dropPiece(int col, int player) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (grid[row][col] == 0) {
                grid[row][col] = player;
                break;
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        board.dispose();
        frame.dispose();
        yellowPiece.dispose();
        redPiece.dispose();
    }
}