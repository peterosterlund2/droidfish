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

package tui;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import uci.UCIProtocol;
import chess.ChessParseError;
import chess.ComputerPlayer;
import chess.Evaluate;
import chess.Game;
import chess.Move;
import chess.Player;
import chess.Position;
import chess.TextIO;
import chess.TwoReturnValues;

public class TUIGame extends Game {

    public TUIGame(Player whitePlayer, Player blackPlayer) {
        super(whitePlayer, blackPlayer);
    }

    protected boolean handleCommand(String moveStr) {
        if (super.handleCommand(moveStr))
            return true;
        if (moveStr.startsWith("testsuite ")) {
            String testSuiteCmd = moveStr.substring(moveStr.indexOf(" ") + 1);
            return handleTestSuite(testSuiteCmd);
        } else if (moveStr.equals("uci")) {
            whitePlayer = null;
            blackPlayer = null;
            UCIProtocol.main(true);
            System.exit(0);
            return false;
        } else if (moveStr.equals("help")) {
            showHelp();
            return true;
        }

        return false;
    }

    private void showHelp() {
        System.out.println("Enter a move, or one of the following special commands:");
        System.out.println("  new             - Start a new game");
        System.out.println("  undo            - Undo last half-move");
        System.out.println("  redo            - Redo next half-move");
        System.out.println("  swap            - Swap sides");
        System.out.println("  go              - Same as swap");
        System.out.println("  list            - List all moves in current game");
        System.out.println("  setpos FEN      - Set a position using a FEN string");
        System.out.println("  getpos          - Print current position in FEN notation");
        System.out.println("  draw rep [move] - Claim draw by repetition");
        System.out.println("  draw 50 [move]  - Claim draw by 50-move rule");
        System.out.println("  draw offer move - Play move and offer draw");
        System.out.println("  draw accept     - Accept a draw offer");
        System.out.println("  resign          - Resign the current game");
        System.out.println("  testsuite filename maxtime");
        System.out.println("  book on|off     - Turn opening book on/off");
        System.out.println("  time t          - Set computer thinking time, ms");
        System.out.println("  perft d         - Run perft test to depth d");
        System.out.println("  uci             - Switch to uci protocol.");
        System.out.println("  help            - Show this help");
        System.out.println("  quit            - Terminate program");
    }

    private boolean handleTestSuite(String cmd) {
        LineNumberReader fr = null;
        try {
            int idx = cmd.indexOf(" ");
            String filename = cmd.substring(0, idx);
            String timeStr = cmd.substring(idx + 1, cmd.length());
            int timeLimit = Integer.parseInt(timeStr);
            //            System.out.printf("file:%s time:%s (%d)\n", filename, timeStr, timeLimit);
            fr = new LineNumberReader(new FileReader(filename));
            String line;
            Player pl = whitePlayer.isHumanPlayer() ? blackPlayer : whitePlayer;
            if (pl.isHumanPlayer()) {
                System.out.printf("No computer player available");
                return false;
            }
            ComputerPlayer cp = (ComputerPlayer)pl;
            int numRight = 0;
            int numTotal = 0;
            while ((line = fr.readLine()) != null) {
                if (line.startsWith("#") || (line.length() == 0)) {
                    continue;
                }
                int idx1 = line.indexOf(" bm ");
                String fen = line.substring(0, idx1);
                int idx2 = line.indexOf(";", idx1);
                String bm = line.substring(idx1 + 4, idx2);
                //                System.out.printf("Line %3d: fen:%s bm:%s\n", fr.getLineNumber(), fen, bm);
                Position testPos = TextIO.readFEN(fen);
                cp.clearTT();
                TwoReturnValues<Move, String> ret = cp.searchPosition(testPos, timeLimit);
                Move sm = ret.first;
                String PV = ret.second;
                Move m = new Move(sm);
                String[] answers = bm.split(" ");
                boolean correct = false;
                for (String a : answers) {
                    Move am = TextIO.stringToMove(testPos, a);
                    if (am == null) {
                        throw new ChessParseError("Invalid move " + a);
                    }
                    if (am.equals(m)) {
                        correct = true;
                        break;
                    }
                }
                if (correct) {
                    numRight++;
                }
                numTotal++;
                System.out.printf("%3d : %6s %6d %d %03d/%03d %s : %s\n", fr.getLineNumber(),
                        TextIO.moveToString(testPos, sm, false), sm.score, correct ? 1 : 0,
                                numRight, numTotal, bm, PV);
            }
            fr.close();
        } catch (NumberFormatException nfe) {
            System.out.printf("Number format exception: %s\n", nfe.getMessage());
            return false;
        } catch (FileNotFoundException fnfe) {
            System.out.printf("File not found: %s\n", fnfe.getMessage());
            return false;
        } catch (IOException ex) {
            System.out.printf("IO error: %s\n", ex.getMessage());
        } catch (ChessParseError cpe) {
            int lineNo = (fr == null) ? -1 : fr.getLineNumber();
            System.out.printf("Parse error, line %d: %s\n", lineNo, cpe.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            int lineNo = (fr == null) ? -1 : fr.getLineNumber();
            System.out.printf("Parse error, line %d: %s\n", lineNo, e.getMessage());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ex) {
                    // Stupid FileReader class forces me to catch this meaningless exception
                }
            }
        }
        return true;
    }

    /**
     * Administrate a game between two players, human or computer.
     */
    public void play() throws IOException {
        handleCommand("new");
        while (true) {
            // Print last move
            if (currentMove > 0) {
                Position prevPos = new Position(pos);
                prevPos.unMakeMove(moveList.get(currentMove - 1), uiInfoList.get(currentMove - 1));
                String moveStr= TextIO.moveToString(prevPos, moveList.get(currentMove - 1), false);
                if (haveDrawOffer()) {
                    moveStr += " (offer draw)";
                }
                String msg = String.format("Last move: %d%s %s",
                        prevPos.fullMoveCounter, prevPos.whiteMove ? "." : "...",
                                moveStr);
                System.out.println(msg);
            }
//            System.out.printf("Hash: %016x\n", pos.zobristHash());
            {
                Evaluate eval = new Evaluate();
                int evScore = eval.evalPos(pos) * (pos.whiteMove ? 1 : -1);
                System.out.printf("Eval: %.2f%n", evScore / 100.0);
            }

            // Check game state
            System.out.print(TextIO.asciiBoard(pos));
            String stateStr = getGameStateString();
            if (stateStr.length() > 0) {
                System.out.printf("%s%n", stateStr);
            }
            if (getGameState() != GameState.ALIVE) {
                activateHumanPlayer();
            }

            // Get command from current player and act on it
            Player pl = pos.whiteMove ? whitePlayer : blackPlayer;
            String moveStr = pl.getCommand(new Position(pos), haveDrawOffer(), getHistory());
            if (moveStr.equals("quit")) {
                return;
            } else {
                boolean ok = processString(moveStr);
                if (!ok) {
                    System.out.printf("Invalid move: %s\n", moveStr);
                }
            }
        }
    }
}
