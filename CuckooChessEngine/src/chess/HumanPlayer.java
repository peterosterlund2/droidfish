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

package chess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

/**
 * A player that reads input from the keyboard.
 * @author petero
 */
public class HumanPlayer implements Player {
    private String lastCmd = "";
    private BufferedReader in;

    public HumanPlayer() {
        in = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public String getCommand(Position pos, boolean drawOffer, List<Position> history) {
        try {
            String color = pos.whiteMove ? "white" : "black";
            System.out.print(String.format(Locale.US, "Enter move (%s):", color));
            String moveStr = in.readLine();
            if (moveStr == null)
                return "quit";
            if (moveStr.length() == 0) {
                return lastCmd;
            } else {
                lastCmd = moveStr;
            }
            return moveStr;
        } catch (IOException ex) {
            return "quit";
        }
    }
    
    @Override
    public boolean isHumanPlayer() {
        return true;
    }
    
    @Override
    public void useBook(boolean bookOn) {
    }

    @Override
    public void timeLimit(int minTimeLimit, int maxTimeLimit, boolean randomMode) {
    }

    @Override
    public void clearTT() {
    }
}
