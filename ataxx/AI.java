/* Skeleton code copyright (C) 2008, 2022 Paul N. Hilfinger and the
 * Regents of the University of California.  Do not distribute this or any
 * derivative work without permission. */

package ataxx;

import java.util.ArrayList;
import java.util.Random;

import static ataxx.PieceColor.*;
import static java.lang.Math.*;

/** A Player that computes its own moves.
 *  @author Peter Chen
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 3;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. SEED is used to initialize
     *  a random-number generator for use in move computations.  Identical
     *  seeds produce identical behaviour. */
    AI(Game game, PieceColor myColor, long seed) {
        super(game, myColor);
        _random = new Random(seed);
    }


    @Override
    boolean isAuto() {
        return true;
    }

    @Override
    String getMove() {
        if (!getBoard().canMove(myColor())) {
            game().reportMove(Move.pass(), myColor());
            return "-";
        }
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();
        game().reportMove(move, myColor());
        return move.toString();
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(getBoard());
        _lastFoundMove = null;
        if (myColor() == RED) {
            minMax(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            minMax(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to the findMove method
     *  above. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove, int sense,
                       int alpha, int beta) {
        /* We use WINNING_VALUE + depth as the winning value so as to favor
         * wins that happen sooner rather than later (depth is larger the
         * fewer moves have been made. */
        if (depth == 0 || board.getWinner() != null) {
            return staticScore(board, WINNING_VALUE + depth);
        }
        Move best;
        int a = alpha;
        int b = beta;
        int bestScore;
        ArrayList<Move> all;
        if (sense == 1) {
            all = board.possible(board, RED);
        } else {
            all = board.possible(board, BLUE);
        }
        if (all.size() == 0 && sense == 1) {
            return a;
        } else if (all.size() == 0 && sense == -1) {
            return b;
        }
        best = all.get(0);
        board.makeMove(best);
        bestScore = minMax(board, depth - 1, false, -sense, a, b);
        board.undo();
        for (Move m : all) {
            board.makeMove(m);
            int response = minMax(board, depth - 1, false, -sense, a, b);
            board.undo();
            if (response > bestScore && sense == 1) {
                bestScore = response;
                if (a >= b) {
                    return bestScore;
                }
                a = max(bestScore, a);
                best = m;
            } else if (response < bestScore && sense == -1) {
                bestScore = response;
                if (a >= b) {
                    return bestScore;
                }
                b = min(bestScore, b);
                best = m;
            }
        }
        if (saveMove) {
            _lastFoundMove = best;
        }
        return bestScore;
    }

    private int empty(Board b) {
        int empty = 0;
        String col = "abcdefg";
        String row = "1234567";
        for (int i = 0; i < col.length(); i++) {
            for (int j = 0; j < row.length(); j++) {
                if (b.get(col.charAt(i), row.charAt(j)) == RED) {
                    if (i == 0 || i == 6 || j == 0 || j == 6) {
                        empty += 5;
                    }
                    for (int x: b.surrounding(col.charAt(i), row.charAt(j))) {
                        if (b.get(x) == EMPTY || b.get(x) == RED) {
                            empty -= 2;
                        } else if (b.get(x) == BLUE) {
                            empty += 1;
                        }
                    }
                } else if (b.get(col.charAt(i),
                        row.charAt(j)) == BLUE) {
                    if (i == 0 || i == 6 || j == 0 || j == 6) {
                        empty -= 5;
                    }
                    for (int x: b.surrounding(col.charAt(i), row.charAt(j))) {
                        if (b.get(x) == EMPTY || b.get(x) == BLUE) {
                            empty += 2;
                        } else if (b.get(x) == RED) {
                            empty -= 1;
                        }
                    }
                }
            }
        }
        return empty;
    }


    /** Return a heuristic value for BOARD.  This value is +- WINNINGVALUE in
     *  won positions, and 0 for ties. */
    private int staticScore(Board board, int winningValue) {
        PieceColor winner = board.getWinner();
        if (winner != null) {
            return switch (winner) {
            case RED -> winningValue;
            case BLUE -> -winningValue;
            default -> 0;
            };
        }
        int empty = empty(board);
        int gee = 0;
        int diff = board.redPieces() - board.bluePieces();
        ArrayList<Move> all = board.possible(board, RED);
        int max = 0;
        int[] index;
        int count;
        for (Move m: all) {
            count = 0;
            index = board.surrounding(m.col1(), m.row1());
            for (int i: index) {
                if (board.get(i) == BLUE) {
                    count += 1;
                }
            }
            if (count > max) {
                max = count;
            }
            if (board.bluePieces() <= 2) {
                for (int y: board.surrounding(m.col1(), m.row1())) {
                    if (board.get(y) == BLUE) {
                        gee += 1;
                    }
                }
            }
        }
        all = board.possible(board, BLUE);
        int min = 0;
        for (Move m: all) {
            count = 0;
            index = board.surrounding(m.col1(), m.row1());
            for (int j: index) {
                if (board.get(j) == RED) {
                    count += 1;
                }
            }
            if (count > min) {
                min = count;
            }
            if (board.redPieces() <= 2) {
                for (int y: board.surrounding(m.col1(), m.row1())) {
                    if (board.get(y) == RED) {
                        gee -= 1;
                    }
                }
            }
        }
        return diff * 8 + max * 5 - min * 5 + empty + gee * 10;
    }

    /** Pseudo-random number generator for move computation. */
    private Random _random = new Random();
}
