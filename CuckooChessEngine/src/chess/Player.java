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

import java.util.List;

/**
 * Interface for human/computer players.
 * @author petero
 */
public interface Player {
    /**
     * Get a command from a player.
     * The command can be a valid move string, in which case the move is played
     * and the turn goes over to the other player. The command can also be a special
     * command, such as "quit", "new", "resign", etc.
     * @param history List of earlier positions (not including the current position).
     *                This makes it possible for the player to correctly handle
     *                the draw by repetition rule.
     */
    public String getCommand(Position pos, boolean drawOffer, List<Position> history);
    
    /** Return true if this player is a human player. */
    public boolean isHumanPlayer();

    /**
     * Inform player whether or not to use an opening book.
     * Of course, a human player is likely to ignore this.
     */
    public void useBook(boolean bookOn);

    /**
     * Inform player about min recommended/max allowed thinking time per move.
     * Of course, a human player is likely to ignore this.
     */
    public void timeLimit(int minTimeLimit, int maxTimeLimit, boolean randomMode);

    /** 
     * Inform player that the transposition table should be cleared.
     * Of course, a human player has a hard time implementing this.
     */
    public void clearTT();
}
