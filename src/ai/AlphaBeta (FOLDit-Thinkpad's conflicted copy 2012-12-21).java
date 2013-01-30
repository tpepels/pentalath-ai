package ai;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import game.Board;
import game.Field;

public class AlphaBeta implements AIPlayer, Runnable {

	private final int N_INF = -2000000, P_INF = 2000000;
	// The base win value, decreased with D_DECR per depth
	private final int WIN_VAL = 1000000, D_DECR = 5000;
	// If force win, row of 4 with 2 free squares
	private final int FW1_VAL = 20000, FW2_VAL = 10000, FW_D_DECR = 100;
	// Some contant values
	private final int TT_SIZE = 67108864, TIME_CHECK_INT = 1000, BASE_TIME = 15000;
	private final DecimalFormat decForm = new DecimalFormat("#,###,###,###,##0");
	//
	private Random r = new Random();
	private MoveCallback callback;
	private Board initBoard;
	// Statistics
	private double totalNodes;
	private double totalDepth, numMoves, researches;
	// Counters etc.
	private int maxDepth, nodes, collisions, timeCheck, myPlayer, opponent, bestMove, prevBestMove,
			tt_lookups;
	private long endTime;
	private boolean forceHalt = false, parallel = true;
	// Transposition table, history, killer moves, butterfly board
	private Transposition[] tt;
	private int[][] history, bfboard;
	private int[][] killermove;
	//
	public int R = 2, DELTA = 60, DEFAULT_DELTA = 60, MAX_DEPTH = 1000;
	public boolean nullmoves = true, transpositions = true, historyHeuristic = true,
			killermoves = true, aspiration = true;
	private Thread t;

	public AlphaBeta() {
		// Assuming we never go deeper than the size of the board.
		history = new int[2][Board.SIZE];
		bfboard = new int[2][Board.SIZE];
	}

	public void resetStats() {
		totalDepth = 0;
		numMoves = 0;
		totalNodes = 0;
		researches = 0;
		// Assuming we never go deeper than the size of the board.
		history = new int[2][Board.SIZE];
		bfboard = new int[2][Board.SIZE];
	}

	public double averageDepth() {
		return totalDepth / numMoves;
	}

	public double totalnodes() {
		return totalNodes;
	}

	public double averageNodes() {
		return totalNodes / numMoves;
	}

	public double averageResearches() {
		return researches / numMoves;
	}

	public int getBestMove() {
		return bestMove;
	}

	@Override
	public void getMove(Board board, MoveCallback callback, int myPlayer, boolean parallel) {
		this.initBoard = board;
		this.callback = callback;
		this.myPlayer = myPlayer;
		this.parallel = parallel;
		this.opponent = (myPlayer == Board.BLACK) ? Board.WHITE : Board.BLACK;
		//
		interupted = false;
		collisions = 0;
		nodes = 0;
		bestMove = 0;
		maxDepth = 0;
		tt_lookups = 0;
		timeCheck = TIME_CHECK_INT;
		forceHalt = false;
		//
		if (parallel) {
			// Start the search in a new Thread.
			t = new Thread(this);
			t.start();
		} else {
			run();
		}
	}

	boolean interupted = false;

	public void stop() {
		if (t != null) {
			interupted = true;
		}
	}

	@Override
	public void run() {
		killermove = new int[Board.SIZE][2];
		tt = new Transposition[TT_SIZE];
		// Decay the history values
		for(int i = 0; i < 2; i++) {
			for(int j = 0; j < Board.SIZE; j++) {
				history[i][j] /= 2;
				bfboard[i][j] /= 2;
			}
		}
		//
		//endTime = 15000; // for testing
		//
		long lastItStartTime = 0, lastItTime = 0;
		int val = 0, alpha = N_INF, beta = P_INF;
		boolean wonlost = false;
		//
		int approxMovesMade = (Board.REAL_SIZE - initBoard.freeSquares) / 2;
		endTime = Math.max((initBoard.timeLeft() / (20 + approxMovesMade)), BASE_TIME);
		System.out.println(":: End time-span: " + endTime);
		// No need to search deep the first few moves
		if (initBoard.freeSquares >= 60)
			MAX_DEPTH = 6;
		else
			MAX_DEPTH = P_INF;
		//
		endTime += System.currentTimeMillis();
		while ((System.currentTimeMillis() < endTime && maxDepth <= MAX_DEPTH) && !forceHalt
				&& !interupted) {
			maxDepth += 1;
			System.out.println(":: Max depth: " + maxDepth);
			prevBestMove = bestMove;
			lastItStartTime = System.currentTimeMillis();
			//
			val = alphaBeta(initBoard, maxDepth, alpha, beta, myPlayer, -1, false);
			if (aspiration) {
				if (val >= beta) {
					System.out.println(":: Re-search required, val(" + val + ") >= beta.");
					researches++;
					alpha = val;
					beta = P_INF;
					val = alphaBeta(initBoard, maxDepth, alpha, beta, myPlayer, -1, false);
				} else if (val <= alpha) {
					researches++;
					System.out.println(":: Re-search required, val(" + val + ") <= alpha.");
					alpha = N_INF;
					beta = val;
					val = alphaBeta(initBoard, maxDepth, alpha, beta, myPlayer, -1, false);
				}
				DELTA = Math.max(Math.abs(val / 2) - 1, DEFAULT_DELTA);
				//
				alpha = val - DELTA;
				beta = val + DELTA;
			}
			//
			System.out.println(" - Best value so far: " + val);
			System.out.println(" - Best move so far: " + bestMove);
			System.out.println(" - Nodes visited: " + decForm.format(nodes));
			// We win/lose
			if (Math.abs(val) > FW1_VAL) {
				wonlost = true;
				break;
			}
			lastItTime = System.currentTimeMillis() - lastItStartTime;
			// We don't have enough time for the next iteration....
			if (endTime - System.currentTimeMillis() < lastItTime * 2)
				break;
		}
		// We can still use the current val if the result is better in vase of forced halt
		if (forceHalt || interupted) {
			bestMove = prevBestMove;
			maxDepth--;
		}
		//
		if (!wonlost) {
			numMoves++;
			totalNodes += nodes;
			totalDepth += maxDepth;
		}
		//
		System.out.println(":: Forced halt: " + forceHalt);
		System.out.println(":: TT Lookups: " + decForm.format(tt_lookups));
		System.out.println(":: Collisions: " + decForm.format(collisions));
		System.out.println(":: Nodes visited: " + decForm.format(nodes));
		System.out.println("--------------------------------");
		// Free the transposition table for the gc.
		tt = null;
		if (!interupted && parallel)
			callback.makeMove(bestMove);
	}

	private int getOpponent(int player) {
		if (player == Board.WHITE)
			return Board.BLACK;
		else
			return Board.WHITE;
	}

	private long MASK = TT_SIZE - 1;

	private int getHashPos(long hash) {
		return (int) (hash & MASK);
	}

	int[] captures = new int[2];

	private int alphaBeta(Board board, int depth, int alpha, int beta, int player, int move,
			boolean nullMove) {
		if (forceHalt || interupted)
			return 0;
		if (timeCheck == 0) {
			// Check if still time left.
			if (System.currentTimeMillis() >= endTime) {
				forceHalt = true;
				return 0;
			}
			timeCheck = TIME_CHECK_INT;
		}
		timeCheck--;
		nodes++;
		// For win/loss depth
		int inv_depth = maxDepth - depth, value = N_INF, bestValue = N_INF;
		int capsw, capsb, olda = alpha, oldb = beta;
		int plyBestMove = -1, hashPos = 0, color = (player == myPlayer) ? 1 : -1;
		boolean valuefound = false, collision = false;
		int[] currentMoves;
		//
		Transposition tp = null;
		if (transpositions) {
			hashPos = getHashPos(board.zobristHash);
			tp = tt[hashPos];
			// Check if present in transposition table
			if (tp != null) {
				tt_lookups++;
				// Position was evaluated previously
				// Check for a collision
				if (tp.hash != board.zobristHash) {
					collisions++;
					collision = true;
				} else if (depth <= tp.depth) {
					if (tp.flag == Transposition.REAL)
						return tp.value;
					if (tp.flag == Transposition.L_BOUND && tp.value > alpha)
						alpha = tp.value;
					else if (tp.flag == Transposition.U_BOUND && tp.value < beta)
						beta = tp.value;
					if (alpha >= beta)
						return tp.value;
				}
			}
		}
		// Check if position is terminal.
		if (move != -1) {
			int winstate = board.checkWin(board.board[move]);
			if (winstate != Board.NONE_WIN) {
				if (winstate == player) {
					// Prefer shallow wins!
					bestValue = (WIN_VAL - (D_DECR * inv_depth));
					valuefound = true;
				} else if (winstate == Board.DRAW) {
					return 0;
				} else {
					// Deeper losses are "less worse" :) than shallow losses
					bestValue = -(WIN_VAL - (D_DECR * inv_depth));
					return bestValue;
				}
			}
		}
		// Leaf-node, evaluate the node
		if (depth == 0 && !valuefound) {
			bestValue = color * evaluate(board, inv_depth);
			valuefound = true;
		} else if (!valuefound) {
			// Don't do null moves at the first move, it messes up the swap rule
			if (nullmoves && !nullMove && depth < maxDepth && depth > R) {// && !board.firstMove) {
				board.pass();
				// Check for a null-move cut-off
				value = -alphaBeta(board, depth - 1 - R, -beta, -alpha, getOpponent(player), -1,
						true);
				board.undoPass();
				if (value >= beta) {
					return beta;
				}
			}
			//
			if (tp == null || collision) {
				currentMoves = board.getAvailableMoves(killermove[inv_depth]);
			} else {
				currentMoves = board.getAvailableMoves(tp.bestMove, killermove[inv_depth][0],
						killermove[inv_depth][1]);
			}
			int startindex = board.startindex, currentmove;
			double maxHistVal = 1.;
			for (int i = 0; i < currentMoves.length; i++) {
				// Try the killer and transposition moves first, then try the hh moves
				if (i >= startindex && maxHistVal > 0. && historyHeuristic) {
					board.getNextMove(history[player - 1], bfboard[player - 1], currentMoves, i);
					// If the previous max history value was 0, we can just follow the indexed list
					maxHistVal = board.maxHistVal;
				}
				currentmove = currentMoves[i];
				if (board.doMove(currentmove, player)) {
					// Returns false if suicide
					if (board.capturePieces(currentmove)) {
						//
						capsw = board.playerCaps[0];
						capsb = board.playerCaps[1];
						// Keep track of the captured pieces.
						captures[0] += capsw;
						captures[1] += capsb;
						//
						value = -alphaBeta(board, depth - 1, -beta, -alpha, getOpponent(player),
								currentmove, nullMove);
						//
						if (value > bestValue) {
							// for detemining the move to return
							if (depth == maxDepth && value > bestValue) {
								bestMove = currentmove;
							}
							//
							bestValue = value;
							plyBestMove = currentmove;
						}
						//
						alpha = Math.max(alpha, bestValue);
						// Substract the captures from this move
						captures[0] -= capsw;
						captures[1] -= capsb;
						board.undoMove();
						// Update the butterfly board for the relative history heuristic
						bfboard[player - 1][currentmove]++;
						if (alpha >= beta) {
							if (killermoves && currentmove != killermove[inv_depth][0]) {
								killermove[inv_depth][1] = killermove[inv_depth][0];
								killermove[inv_depth][0] = currentmove;
							}
							break;
						}
					}
				} else {
					System.err.println("error making move!");
				}
			}
		}
		// Update the history heuristics for move-ordering
		if (plyBestMove > -1)
			history[player - 1][plyBestMove]++;
		// Replace if deeper or doesn't exist
		if (transpositions && (tp == null || (collision && depth > tp.depth))) {
			tp = new Transposition();
			tt[hashPos] = tp;
			tp.bestMove = plyBestMove;
			tp.depth = depth;
			tp.hash = board.zobristHash;
			// 
			if (bestValue <= olda) {
				tp.flag = Transposition.U_BOUND;
			} else if (bestValue >= oldb) {
				tp.flag = Transposition.L_BOUND;
			} else {
				tp.flag = Transposition.REAL;
			}
			tp.value = bestValue;
		}
		return bestValue;
	}

	public void setFeatureWeights(int[] weights) {
		this.weights = weights;
	}

	// Weights for the features
	// [0] Captures
	// [1] my longest row,
	// [2] min. freedom of my pieces,
	// [3] min. freedom of opponent's pieces,
	// [4] longest opponent's row,
	// [5] pieces capped by opponent.
	// [6] my largest group
	// [7] opponent's largest group
	private int[] weights = { 800, 50, 5, -5, -50, -800, 10, -10, 5, -5 };
	private boolean[] seenFree, visited;
	public int FREE_SQ_LIM = 55; // The limit for choosing the largest group in stead of max freedom

	private int evaluate(Board board, int inv_depth) {
		int score = (board.currentPlayer != myPlayer) ? -20: 0;
		// The number of opponent pieces captured by my player
		score += weights[0] * captures[opponent - 1];
		// The number of my pieces captured by the opponent
		score += weights[5] * captures[myPlayer - 1];
		//
		seenFree = new boolean[Board.SIZE];
		int minFreeOpp = P_INF, minFreeMe = P_INF, currentFree, i, count = 0;
		int maxRowOpp = 0, maxRowMe = 0, currentMax, maxGroupMe = 0, maxGroupOpp = 0, maxTotalFreeMe = 0, maxTotalFreeOpp = 0;
		boolean isOpp, myTurn;
		// Check minimal freedom, longest rows etc.
		for (int j = 0; j < board.spiralOrder.length; j++) {
			i = board.spiralOrder[j];
			// Check if position is part of the board
			if (board.board[i].occupant == Board.FREE)
				continue;
			isOpp = board.board[i].occupant != myPlayer;
			myTurn = board.currentPlayer == board.board[i].occupant;
			// Check if longest row.
			currentMax = checkRowLength(board.board[i], board,
					board.currentPlayer == board.board[i].occupant);
			// Check if row of 4 with 2 freedom
			if (winByForce1 > 0) {
				// System.out.println("Force move win for: " + board.board[i].occupant);
				// good or bad :)
				score = (isOpp) ? (-(FW1_VAL - (FW_D_DECR * inv_depth)))
						: (FW1_VAL - (FW_D_DECR * inv_depth));
				return score;
			} else if (winByForce2 > 0) {
				// System.out.println("Force move win for: " + board.board[i].occupant);
				// good or bad :)
				score = (isOpp) ? (-(FW2_VAL - (FW_D_DECR * inv_depth)))
						: (FW2_VAL - (FW_D_DECR * inv_depth));
				return score;
			}
			// Check if row length is higher than current highest
			if (isOpp && currentMax > maxRowOpp) {
				maxRowOpp = currentMax;
			} else if (!isOpp && currentMax > maxRowMe) {
				maxRowMe = currentMax;
			}
			// Check the maximum total freedom in every direction
			if (isOpp && totalfreedom > maxTotalFreeOpp) {
				maxTotalFreeOpp = totalfreedom;
			} else if (!isOpp && totalfreedom > maxTotalFreeMe) {
				maxTotalFreeMe = totalfreedom;
			}
			// Check for minimal freedom.
			checkedFree.clear();
			visited = new boolean[Board.SIZE];
			if (myTurn) // Be pessimistic about group-size if not my turn
				groupSize = 1;
			else
				groupSize = 0;
			//
			currentFree = checkFreedom(board.board[i], 0);
			for (Field f : checkedFree) {
				f.freedom = currentFree;
			}
			// Check the largest group
			if (isOpp && groupSize > maxGroupOpp) {
				maxGroupOpp = groupSize;
			} else if (!isOpp && groupSize > maxGroupMe) {
				maxGroupMe = groupSize;
			}
			// There should be at least two pieces on the board or no use to compare freedom
			if (Board.REAL_SIZE - board.freeSquares > 2) {
				// Check if freedom is lower than current lowest.
				if (isOpp && currentFree < minFreeOpp) {
					minFreeOpp = currentFree;
				} else if (!isOpp && currentFree < minFreeMe) {
					minFreeMe = currentFree;
				}
			}
			count++;
			if (count == Board.REAL_SIZE - board.freeSquares)
				break;
		}
		// Final scoring
		score += weights[1] * maxRowMe;
		score += weights[2] * minFreeMe;
		score += weights[3] * minFreeOpp;
		score += weights[4] * maxRowOpp;
		//
		if (initBoard.freeSquares < FREE_SQ_LIM) {
			score += weights[6] * maxGroupMe;
			score += weights[7] * maxGroupOpp;
		} else {
			score += weights[8] * maxTotalFreeMe;
			score += weights[9] * maxTotalFreeOpp;
		}
		if (!parallel) { // If running tests..
			if (r.nextInt(10) < 3)
				score += r.nextInt(5);
		}
		return score;
	}

	private ArrayList<Field> checkedFree = new ArrayList<Field>(Board.SIZE);
	private int groupSize = 0;

	/**
	 * Set/get the freedom of a field
	 * 
	 * @param f
	 *            The current field
	 * @return The freedom of the field
	 */
	private int checkFreedom(Field f, int current) {
		// This field was checked before, return its freedom.
		if (seenFree[f.position])
			return f.freedom;

		visited[f.position] = true;
		//
		Field[] nb = f.neighbours;
		for (Field n : nb) {
			if (n == null)
				continue;
			// For each free neighbor increase the current freedom.
			if (n.occupant == Board.FREE && !visited[n.position]) {
				current++;
				// Count each free position only once!
				visited[n.position] = true;
			} else if (n.occupant == f.occupant && !visited[n.position]) {
				// Check similarly occupied neighbors
				groupSize++;
				current = checkFreedom(n, current);
				checkedFree.add(n);
			}
		}
		seenFree[f.position] = true;
		return current;
	}

	// This value is set if there exists a row of length 4 and freedom 2
	private int winByForce1 = 0, winByForce2 = 0, totalfreedom = 0;
	int opp, longestRow;
	boolean[] closedrow = new boolean[3], extrafreedom = new boolean[3];
	int[] rowLength = new int[3], freedom = new int[6], totFreedom = new int[6];

	/**
	 * Check the longest row that this field is part of
	 * 
	 * @param f
	 *            The current field
	 * @return The length of the longest row
	 */
	public int checkRowLength(Field f, Board board, boolean myTurn) {
		winByForce1 = 0;
		winByForce2 = 0;
		totalfreedom = 0;
		//
		opp = (f.occupant == Board.BLACK) ? Board.WHITE : Board.BLACK;
		// Each row is of at least length 1 :)
		for (int i = 0; i < rowLength.length; i++) {
			// The current stone
			rowLength[i] = 1;
			closedrow[i] = true;
			extrafreedom[i] = false;
			//
			freedom[i] = 0;
			freedom[i + 3] = 0;
			totFreedom[i] = 0;
			totFreedom[i + 3] = 0;
		}
		longestRow = 0;
		boolean prevFree = false, rowfinished = false;
		Field currentField;
		// Check once in each direction.
		for (int j = 0; j < Board.NUM_NEIGHBOURS; j++) {
			prevFree = false;
			rowfinished = false;
			currentField = f.neighbours[j];
			// If we've already seen this position, the current row will not be longer than when
			// we saw it before.
			if (currentField == null)
				continue;

			// Check for a row of 5 in each direction.
			while (currentField != null && currentField.occupant != opp) {
				if (!rowfinished) {
					// Is part of the row, increase
					if (currentField.occupant == f.occupant) {
						if (prevFree) {
							closedrow[j % 3] = false; // a gap
						} else {
							prevFree = false;
							rowLength[j % 3]++;
						}
					} else if (!prevFree) {
						totalfreedom++;
						// The row has some freedom in this direction
						prevFree = true;
						freedom[j]++;
						totFreedom[j]++;
					} else if (prevFree) {
						extrafreedom[j % 3] = true;
						// Two free squares == no longer part of a row
						rowfinished = true;
						totalfreedom++;
						totFreedom[j]++;
						// Total freedom is not considered later in the game
						if (initBoard.freeSquares < FREE_SQ_LIM
								|| totFreedom[j % 3] + rowLength[j % 3] >= Board.ROW_SIZE)
							break;
					}
				} else {
					// Keep counting the free squares in this direction
					totalfreedom++;
					totFreedom[j]++;
				}
				//
				currentField = currentField.neighbours[j];
			}
		}
		//
		int longestrowi = -1;
		for (int i = 0; i < rowLength.length; i++) {
			// Check for the longest row, only if it can be extended to a row of 5
			if (rowLength[i] > longestRow
					&& rowLength[i] + totFreedom[i] + totFreedom[i + 3] >= Board.ROW_SIZE) {
				longestRow = rowLength[i];
				longestrowi = i;
			}
		}
		// If not player's turn, be pessimistic about freedom
		if (!myTurn) {
			if (longestrowi >= 0) {
				// Assume the opponent will block the longest row
				freedom[longestrowi]--;
				// Assume the opponent will cut of row with most freedom
				if (totFreedom[longestrowi] > totFreedom[longestrowi + 3]) {
					totalfreedom -= totFreedom[longestrowi];
					totFreedom[longestrowi] = 0;
				} else {
					totalfreedom -= totFreedom[longestrowi + 3];
					totFreedom[longestrowi + 3] = 0;
				}
				// Re-check if still the longest row.
				for (int i = 0; i < rowLength.length; i++) {
					// Check for the longest row
					if (rowLength[i] > longestRow
							&& rowLength[i] + totFreedom[i] + totFreedom[i + 3] >= Board.ROW_SIZE) {
						longestRow = rowLength[i];
					}
				}
			}
		}
		for (int i = 0; i < rowLength.length; i++) {
			// This condition always leads to a win, closed row of 4, freedom on both sides
			// Or, if myTurn, closed row of three with freedom on both sides, and one extra freedom.
			if (rowLength[i] == 4 && freedom[i] == 2 && closedrow[i]) {
				winByForce1 = f.occupant;
				return longestRow;
			} else if (myTurn && rowLength[i] == 3 && freedom[i] == 2 && closedrow[i]
					&& extrafreedom[i]) {
				winByForce2 = f.occupant;
			} else if (myTurn && rowLength[i] == 4 && freedom[i] >= 1 && closedrow[i]) {
				winByForce2 = f.occupant;
			}
		}
		return longestRow;// * numGoodRows;
	}
}
