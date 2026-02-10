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
    // 0 = empty, 1 = yellow

    SpriteBatch batch;
    Texture board;
    Texture frame;
    Texture yellowPiece;

    @Override
    public void create() {
        batch = new SpriteBatch();
        board = new Texture("board.png");
        frame = new Texture("frame.png");
        yellowPiece = new Texture("piece_yellow.png"); // ✅ fixed
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float scale = 2.5f;

        float boardWidth  = board.getWidth()  * scale;
        float boardHeight = board.getHeight() * scale;

        float boardX = (Gdx.graphics.getWidth() - boardWidth) / 2f;
        float boardY = (Gdx.graphics.getHeight() - boardHeight) / 2f;

        batch.begin();

        // draw board
        batch.draw(board, boardX, boardY, boardWidth, boardHeight);

        // input
        if (Gdx.input.justTouched()) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            if (mouseX >= boardX && mouseX <= boardX + boardWidth &&
                mouseY >= boardY && mouseY <= boardY + boardHeight) {

                float cellWidth = boardWidth / COLS;
                int col = (int) ((mouseX - boardX) / cellWidth);
                dropPiece(col);
            }
        }

        // draw pieces
        float cellWidth  = boardWidth / COLS;
        float cellHeight = boardHeight / ROWS;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == 1) {

                    float size = Math.min(cellWidth, cellHeight) * 0.8f;

                    float pieceX = boardX + c * cellWidth + (cellWidth - size) / 2f;
                    float pieceY = boardY + (ROWS - 1 - r) * cellHeight + (cellHeight - size) / 2f;

                    batch.draw(yellowPiece, pieceX, pieceY, size, size);
                }
            }
        }

        // draw frame LAST
        batch.draw(frame, boardX, boardY, boardWidth, boardHeight);

        batch.end();
    }

    void dropPiece(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (grid[row][col] == 0) {
                grid[row][col] = 1;
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
    }
}
