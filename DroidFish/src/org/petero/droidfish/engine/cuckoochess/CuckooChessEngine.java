/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com

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

import android.os.Environment;
import chess.ChessParseError;
import chess.ComputerPlayer;
import chess.Move;
import chess.Position;
import chess.TextIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import org.petero.droidfish.EngineOptions;
import org.petero.droidfish.engine.LocalPipe;
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

    private LocalPipe guiToEngine;
    private LocalPipe engineToGui;
    private Thread engineThread;

    public CuckooChessEngine(Report report) {
        pos = null;
        moves = new ArrayList<Move>();
        quit = false;
        guiToEngine = new LocalPipe();
        engineToGui = new LocalPipe();
    }

    /** @inheritDoc */
    @Override
    protected final void startProcess() {
        engineThread = new Thread(new Runnable() {
            public void run() {
                mainLoop(guiToEngine, engineToGui);
            }
        });
        int pMin = Thread.MIN_PRIORITY;
        int pNorm = Thread.NORM_PRIORITY;
        int prio = pMin + (pNorm - pMin) / 2;
        engineThread.setPriority(prio);
        engineThread.start();
    }

    @Override
    protected File getOptionsFile() {
        File extDir = Environment.getExternalStorageDirectory();
        return new File(extDir, "/DroidFish/uci/cuckoochess.ini");
    }

    @Override
    protected boolean configurableOption(String name) {
        name = name.toLowerCase(Locale.US);
        if (!super.configurableOption(name))
            return false;
        if (name.equals("strength"))
            return false;
        return true;
    }

    /** @inheritDoc */
    @Override
    public boolean optionsOk(EngineOptions engineOptions) {
        return true;
    }

    /** @inheritDoc */
    @Override
    public final void setStrength(int strength) {
        setOption("strength", strength);
    }

    private final void mainLoop(LocalPipe is, LocalPipe os) {
        String line;
        while ((line = is.readLine()) != null) {
            handleCommand(line, os);
            if (quit) {
                break;
            }
        }
    }

    @Override
    public void shutDown() {
        super.shutDown();
        guiToEngine.close();
    }

    /** @inheritDoc */
    @Override
    public final String readLineFromEngine(int timeoutMillis) {
        if ((engineThread != null) && !engineThread.isAlive())
            return null;
        String ret = engineToGui.readLine(timeoutMillis);
        if (ret == null)
            return null;
        if (ret.length() > 0) {
//            System.out.printf("Engine -> GUI: %s\n", ret);
        }
        return ret;
    }

    /** @inheritDoc */
    @Override
    public final synchronized void writeLineToEngine(String data) {
//        System.out.printf("GUI -> Engine: %s\n", data);
        guiToEngine.addLine(data);
    }

    private final void handleCommand(String cmdLine, LocalPipe os) {
        String[] tokens = tokenize(cmdLine);
        try {
            String cmd = tokens[0];
            if (cmd.equals("uci")) {
                os.printLine("id name %s", ComputerPlayer.engineName);
                os.printLine("id author Peter Osterlund");
                DroidEngineControl.printOptions(os);
                os.printLine("uciok");
            } else if (cmd.equals("isready")) {
                initEngine(os);
                os.printLine("readyok");
            } else if (cmd.equals("setoption")) {
                initEngine(os);
                StringBuilder optionName = new StringBuilder();
                StringBuilder optionValue = new StringBuilder();
                if (tokens[1].endsWith("name")) {
                    int idx = 2;
                    while ((idx < tokens.length) && !tokens[idx].equals("value")) {
                        optionName.append(tokens[idx++].toLowerCase(Locale.US));
                        optionName.append(' ');
                    }
                    if ((idx < tokens.length) && tokens[idx++].equals("value")) {
                        while ((idx < tokens.length)) {
                            optionValue.append(tokens[idx++].toLowerCase(Locale.US));
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
                if (pos == null) {
                    try {
                        pos = TextIO.readFEN(TextIO.startPosFEN);
                    } catch (ChessParseError ex) {
                        throw new RuntimeException();
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

    private final void initEngine(LocalPipe os) {
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
