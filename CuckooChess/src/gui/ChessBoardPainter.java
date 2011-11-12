/*
    CuckooChess - A java chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package gui;

import chess.Move;
import chess.Piece;
import chess.Position;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JLabel;

/**
 * Draws a graphical chess board. Also handles user interaction.
 * @author petero
 */
public class ChessBoardPainter extends JLabel {
    private static final long serialVersionUID = -1319250011487017825L;
    private Position pos;
    private int selectedSquare;
    private int x0, y0, sqSize;
    private boolean flipped;
    private Font chessFont;

    // For piece animation during dragging
    private int activeSquare;
    private boolean dragging;
    private int dragX;
    private int dragY;
    private boolean cancelSelection;

    ChessBoardPainter() {
        pos = new Position();
        selectedSquare = -1;
        x0 = y0 = sqSize = 0;
        flipped = false;
        activeSquare = -1;
    }

    /**
     * Set the board to a given state.
     * @param pos
     */
    final public void setPosition(Position pos) {
        this.pos = pos;
        repaint();
    }

    /**
     * Set/clear the board flipped status.
     * @param flipped
     */
    final public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    /**
     * Set/clear the selected square.
     * @param square The square to select, or -1 to clear selection.
     */
    final public void setSelection(int square) {
        if (square != this.selectedSquare) {
            this.selectedSquare = square;
            repaint();
        }
    }

    @Override
    public void paint(Graphics g0) {
        Graphics2D g = (Graphics2D)g0;
        Dimension size = getSize();
        sqSize = (Math.min(size.width, size.height) - 4) / 8;
        x0 = (size.width - sqSize * 8) / 2;
        y0 = (size.height - sqSize * 8) / 2;

        boolean doDrag = (activeSquare >= 0) && dragging;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                g.setColor(Position.darkSquare(x, y) ? Color.GRAY : new Color(190, 190, 90));
                g.fillRect(xCrd, yCrd, sqSize, sqSize);

                int sq = Position.getSquare(x, y);
                int p = pos.getPiece(sq);
                if (doDrag && (sq == activeSquare)) {
                    // Skip this piece. It will be drawn later at (dragX,dragY)
                } else {
                    drawPiece(g, xCrd + sqSize / 2, yCrd + sqSize / 2, p);
                }
            }
        }
        if (selectedSquare >= 0) {
            int selX = Position.getX(selectedSquare);
            int selY = Position.getY(selectedSquare);
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3));
            g.drawRect(getXCrd(selX), getYCrd(selY), sqSize, sqSize);
        }
        if (doDrag) {
            int p = pos.getPiece(activeSquare);
            drawPiece(g, dragX, dragY, p);
        }
    }

    private final void drawPiece(Graphics2D g, int xCrd, int yCrd, int p) {
        g.setColor(Piece.isWhite(p) ? Color.WHITE : Color.BLACK);
        String ps;
        switch (p) {
            case Piece.EMPTY:
                ps = "";
                break;
            case Piece.WKING:
                ps = "k";
                break;
            case Piece.WQUEEN:
                ps = "q";
                break;
            case Piece.WROOK:
                ps = "r";
                break;
            case Piece.WBISHOP:
                ps = "b";
                break;
            case Piece.WKNIGHT:
                ps = "n";
                break;
            case Piece.WPAWN:
                ps = "p";
                break;
            case Piece.BKING:
                ps = "l";
                break;
            case Piece.BQUEEN:
                ps = "w";
                break;
            case Piece.BROOK:
                ps = "t";
                break;
            case Piece.BBISHOP:
                ps = "v";
                break;
            case Piece.BKNIGHT:
                ps = "m";
                break;
            case Piece.BPAWN:
                ps = "o";
                break;
            default:
                ps = "?";
                break;
        }
        if (ps.length() > 0) {
            FontRenderContext frc = g.getFontRenderContext();
            if ((chessFont == null) || (chessFont.getSize() != sqSize)) {
                InputStream inStream = getClass().getResourceAsStream("/gui/casefont.ttf");
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, inStream);
                    chessFont = font.deriveFont((float)sqSize);
                } catch (FontFormatException ex) {
                    throw new RuntimeException();
                } catch (IOException ex) {
                    throw new RuntimeException();
                }
            }
            g.setFont(chessFont);
            Rectangle2D textRect = g.getFont().getStringBounds(ps, frc);
            int xCent = (int)textRect.getCenterX();
            int yCent = (int)textRect.getCenterY();
            g.drawString(ps, xCrd - xCent, yCrd - yCent);
        }
    }

    private final int getXCrd(int x) {
        return x0 + sqSize * (flipped ? 7 - x : x);
    }
    private final int getYCrd(int y) {
        return y0 + sqSize * (flipped ? y : (7 - y));
    }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    final int eventToSquare(MouseEvent evt) {
        int xCrd = evt.getX();
        int yCrd = evt.getY();

        int sq = -1;
        if ((xCrd >= x0) && (yCrd >= y0) && (sqSize > 0)) {
            int x = (xCrd - x0) / sqSize;
            int y = 7 - (yCrd - y0) / sqSize;
            if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                if (flipped) {
                    x = 7 - x;
                    y = 7 - y;
                }
                sq = Position.getSquare(x, y);
            }
        }
        return sq;
    }

    final Move mousePressed(int sq) {
        Move m = null;
        cancelSelection = false;
        int p = pos.getPiece(sq);
        if ((selectedSquare >= 0) && (sq == selectedSquare)) {
            int fromPiece = pos.getPiece(selectedSquare);
            if ((fromPiece == Piece.EMPTY) || (Piece.isWhite(fromPiece) != pos.whiteMove)) {
                return m; // Can't move the piece the oppenent just moved.
            }
        }
        if ((selectedSquare < 0) &&
                ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.whiteMove))) {
            return m;  // You must click on one of your own pieces.
        }
        activeSquare = sq;
        dragging = false;
        dragX = dragY = -1;

        if (selectedSquare >= 0) {
            if (sq == selectedSquare) {
                cancelSelection = true;
            } else {
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.whiteMove)) {
                    m = new Move(selectedSquare, sq, Piece.EMPTY);
                    activeSquare = -1;
                    setSelection(sq);
                }
            }
        }
        if (m == null) {
            setSelection(-1);
        }
        return m;
    }

    final void mouseDragged(MouseEvent evt) {
        final int xCrd = evt.getX();
        final int yCrd = evt.getY();
        if (!dragging || (dragX != xCrd) || (dragY != yCrd)) {
            dragging = true;
            dragX = xCrd;
            dragY = yCrd;
            repaint();
        }
    }

    final Move mouseReleased(int sq) {
        Move m = null;
        if (activeSquare >= 0) {
            if (sq != activeSquare) {
                m = new Move(activeSquare, sq, Piece.EMPTY);
                setSelection(sq);
            } else if (!cancelSelection) {
                setSelection(sq);
            }
            activeSquare = -1;
            repaint();
        }
        return m;
    }
}
