/*
    DroidFish - An Android chess program.
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

package org.petero.droidfish.engine.cuckoochess;

import chess.ChessParseError;
import chess.ComputerPlayer;
import chess.Move;
import chess.Position;
import chess.TextIO;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;

import org.petero.droidfish.engine.UCIEngineBase;

/**
 * UCI interface to cuckoochess engine.
 * @author petero
 */
public class CuckooChessEngine extends UCIEngineBase {

    // Data set by the "position" command.
    private Position pos;
    private ArrayList<Move> moves;

    // Engine data
    private DroidEngineControl engine;

    // Set to true to break out of main loop
    private boolean quit;

    private Pipe guiToEngine;
    private Pipe engineToGui;
    private NioInputStream inFromEngine;

    public CuckooChessEngine() {
        try {
            pos = TextIO.readFEN(TextIO.startPosFEN);
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        }
        moves = new ArrayList<Move>();
        quit = false;
        try {
            guiToEngine = Pipe.open();
            engineToGui = Pipe.open();
            inFromEngine = new NioInputStream(engineToGui);
        } catch (IOException e) {
        }
    }

    @Override
    public void setStrength(int strength) {
        this.strength = strength;
        setOption("strength", strength);
    }

    protected final void startProcess() {
        new Thread(new Runnable() {
            public void run() {
                NioInputStream in = new NioInputStream(guiToEngine);
                NioPrintStream out = new NioPrintStream(engineToGui);
                mainLoop(in, out);
            }
        }).start();
    }

    private final void mainLoop(NioInputStream is, NioPrintStream os) {
        String line;
        while ((line = is.readLine()) != null) {
            handleCommand(line, os);
            if (quit) {
                break;
            }
        }
    }

    @Override
    public final String readLineFromEngine(int timeoutMillis) {
        String ret = inFromEngine.readLine(timeoutMillis);
        if (ret == null)
            return null;
        if (ret.length() > 0) {
//            System.out.printf("Engine -> GUI: %s\n", ret);
        }
        return ret;
    }

    @Override
    public final synchronized void writeLineToEngine(String data) {
//        System.out.printf("GUI -> Engine: %s\n", data);
        try {
            String s = data + "\n";
            guiToEngine.sink().write(ByteBuffer.wrap(s.getBytes()));
        } catch (IOException e) {
        }
    }

    private final void handleCommand(String cmdLine, NioPrintStream os) {
        String[] tokens = tokenize(cmdLine);
        try {
            String cmd = tokens[0];
            if (cmd.equals("uci")) {
                os.printf("id name %s%n", ComputerPlayer.engineName);
                os.printf("id author Peter Osterlund%n");
                DroidEngineControl.printOptions(os);
                os.printf("uciok%n");
            } else if (cmd.equals("isready")) {
                initEngine(os);
                os.printf("readyok%n");
            } else if (cmd.equals("setoption")) {
                initEngine(os);
                StringBuilder optionName = new StringBuilder();
                StringBuilder optionValue = new StringBuilder();
                if (tokens[1].endsWith("name")) {
                    int idx = 2;
                    while ((idx < tokens.length) && !tokens[idx].equals("value")) {
                        optionName.append(tokens[idx++].toLowerCase());
                        optionName.append(' ');
                    }
                    if ((idx < tokens.length) && tokens[idx++].equals("value")) {
                        while ((idx < tokens.length)) {
                            optionValue.append(tokens[idx++].toLowerCase());
                            optionValue.append(' ');
                        }
                    }
                    engine.setOption(optionName.toString().trim(), optionValue.toString().trim());
                }
            } else if (cmd.equals("ucinewgame")) {
                if (engine != null) {
                    engine.newGame();
                }
            } else if (cmd.equals("position")) {
                String fen = null;
                int idx = 1;
                if (tokens[idx].equals("startpos")) {
                    idx++;
                    fen = TextIO.startPosFEN;
                } else if (tokens[idx].equals("fen")) {
                    idx++;
                    StringBuilder sb = new StringBuilder();
                    while ((idx < tokens.length) && !tokens[idx].equals("moves")) {
                        sb.append(tokens[idx++]);
                        sb.append(' ');
                    }
                    fen = sb.toString().trim();
                }
                if (fen != null) {
                    pos = TextIO.readFEN(fen);
                    moves.clear();
                    if ((idx < tokens.length) && tokens[idx++].equals("moves")) {
                        for (int i = idx; i < tokens.length; i++) {
                            Move m = TextIO.uciStringToMove(tokens[i]);
                            if (m != null) {
                                moves.add(m);
                            } else {
                                break;
                            }
                        }
                    }
                }
            } else if (cmd.equals("go")) {
                initEngine(os);
                int idx = 1;
                SearchParams sPar = new SearchParams();
                boolean ponder = false;
                while (idx < tokens.length) {
                    String subCmd = tokens[idx++];
                    if (subCmd.equals("searchmoves")) {
                        while (idx < tokens.length) {
                            Move m = TextIO.uciStringToMove(tokens[idx]);
                            if (m != null) {
                                sPar.searchMoves.add(m);
                                idx++;
                            } else {
                                break;
                            }
                        }
                    } else if (subCmd.equals("ponder")) {
                        ponder = true;
                    } else if (subCmd.equals("wtime")) {
                        sPar.wTime = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("btime")) {
                        sPar.bTime = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("winc")) {
                        sPar.wInc = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("binc")) {
                        sPar.bInc = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("movestogo")) {
                        sPar.movesToGo = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("depth")) {
                        sPar.depth = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("nodes")) {
                        sPar.nodes = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("mate")) {
                        sPar.mate = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("movetime")) {
                        sPar.moveTime = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("infinite")) {
                        sPar.infinite = true;
                    }
                }
                if (ponder) {
                    engine.startPonder(pos, moves, sPar);
                } else {
                    engine.startSearch(pos, moves, sPar);
                }
            } else if (cmd.equals("stop")) {
                engine.stopSearch();
            } else if (cmd.equals("ponderhit")) {
                engine.ponderHit();
            } else if (cmd.equals("quit")) {
                if (engine != null) {
                    engine.stopSearch();
                }
                quit = true;
            }
        } catch (ChessParseError ex) {
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (NumberFormatException nfe) {
        }
    }

    private final void initEngine(NioPrintStream os) {
        if (engine == null) {
            engine = new DroidEngineControl(os);
        }
    }

    /** Convert a string to tokens by splitting at whitespace characters. */
    private final String[] tokenize(String cmdLine) {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }
}
