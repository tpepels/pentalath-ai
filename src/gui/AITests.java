package gui;

import game.Board;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import ai.AlphaBeta;
import ai.MoveCallback;

public class AITests implements MoveCallback {
	private AlphaBeta aiPlayer1, aiPlayer2;
	private Board board;
	private int games = 20;
	//
	private int winner = -1, totalGames;
	private double ai1Wins = 0, ai1Depth = 0, ai1Nodes = 0, ai1Researches = 0;
	private int ai1Color = Board.WHITE, ai2Color = Board.BLACK;
	private double ai2Wins = 0, ai2Depth = 0, ai2Nodes = 0, ai2Researches = 0;
	//
	private String outFile;
	private PrintWriter out;
	DecimalFormat df2 = new DecimalFormat("#,###,###,###,##0");
	DecimalFormat df1 = new DecimalFormat("#,###,###,###,##0.##");

	public void runTests(int which) {
		if (which == 1) {
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = fullOptionsAi();
			outFile = "transpositions-full.txt";
//			aiPlayer1.transpositions = false;
			runGames("ai1: no transpositions, ai2: full options");
			//
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = fullOptionsAi();
			outFile = "aspiration-full.txt";
//			aiPlayer1.aspiration = false;
			runGames("ai1: no aspiration search, ai2: full options");
			//
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = fullOptionsAi();
			outFile = "nullmoves-full.txt";
//			aiPlayer1.nullmoves = false;
			runGames("ai1: no null moves, ai2: full options");
			//
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = fullOptionsAi();
			outFile = "history-full.txt";
//			aiPlayer1.historyHeuristic = false;
			runGames("ai1: no history heuristic, ai2: full options");
			// //
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = fullOptionsAi();
			outFile = "killermoves-full.txt";
//			aiPlayer1.killermoves = false;
			runGames("ai1: no killer moves, ai2: full options");
		} else if (which == 2) {
			// ------------------------------------------------------------------
			// Next: against plain a-b
			aiPlayer1 = nonEnhancedAi();
			aiPlayer2 = nonEnhancedAi();
			outFile = "transpositions-plain.txt";
//			aiPlayer1.transpositions = true;
			runGames("ai1: transpositions, ai2: plain a-b");
			//
			aiPlayer1 = nonEnhancedAi();
			aiPlayer2 = nonEnhancedAi();
			outFile = "aspiration-plain.txt";
//			aiPlayer1.aspiration = true;
			runGames("ai1: aspiration search, ai2: plain a-b");
			//
			aiPlayer1 = nonEnhancedAi();
			aiPlayer2 = nonEnhancedAi();
			outFile = "killermoves-plain.txt";
//			aiPlayer1.killermoves = true;
			runGames("ai1: killer moves, ai2: plain a-b");
			//
			aiPlayer1 = nonEnhancedAi();
			aiPlayer2 = nonEnhancedAi();
			outFile = "history-plain.txt";
//			aiPlayer1.historyHeuristic = true;
			runGames("ai1: history heuristic, ai2: plain a-b");
			//
			aiPlayer1 = nonEnhancedAi();
			aiPlayer2 = nonEnhancedAi();
			outFile = "nullmoves-plain.txt";
//			aiPlayer1.nullmoves = true;
			runGames("ai1: null moves heuristic, ai2: plain a-b");
		} else if (which == 3) {
			int[] fs1 = { 800, 40, 5, -5, -40, -800, 5, -5, 0, 0 };
			// int[] fs2 = { 800, 40, 5, -5, -40, -800, 0, -0, 5, -5 };

			// aiPlayer1 = new AlphaBeta();
			// aiPlayer2 = new AlphaBeta();
			// aiPlayer1.setFeatureWeights(fs1);
			// outFile = "feature set 1.txt";
			// runGames("ai1: feature set 1, ai2: default");
			//
			// aiPlayer1 = new AlphaBeta();
			// aiPlayer2 = new AlphaBeta();
			// aiPlayer1.setFeatureWeights(fs2);
			// outFile = "feature set 2s.txt";
			// runGames("ai1: feature set 2, ai2: default");
			// // runGames("ai1: null moves, ai2: plain a-b");
			// int[] fs3 = { 800, 40, 0, 0, -40, -800, 5, -5, 5, -5 };
			// aiPlayer1 = new AlphaBeta();
			// aiPlayer2 = new AlphaBeta();
			// aiPlayer1.setFeatureWeights(fs3);
			// outFile = "feature set 3.txt";
			// runGames("ai1: feature set 3, ai2: default");

			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = new AlphaBeta();
			aiPlayer1.setFeatureWeights(fs1);
			aiPlayer1.FREE_SQ_LIM = 0;
			outFile = "totalfreedomalways.txt";
			runGames("ai1: totalfreedom always, ai2: default");

		} else if (which == 4) {
			// ------------------------------------------------------------------
			// Next: against plain a-b
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = new AlphaBeta();
			outFile = "transpositions-depth.txt";
//			aiPlayer1.transpositions = false;
			aiPlayer1.MAX_DEPTH = 8;
			aiPlayer2.MAX_DEPTH = 8;
			runGames("ai1: no transpositions, ai2: plain a-b");
			//
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = new AlphaBeta();
			outFile = "aspiration-depth.txt";
//			aiPlayer1.aspiration = false;
			aiPlayer1.MAX_DEPTH = 8;
			aiPlayer2.MAX_DEPTH = 8;
			runGames("ai1: no aspiration search, ai2: plain a-b");
			// //
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = new AlphaBeta();
			outFile = "nullmoves-depth.txt";
//			aiPlayer1.nullmoves = false;
			aiPlayer1.MAX_DEPTH = 8;
			aiPlayer2.MAX_DEPTH = 8;
			runGames("ai1: no null moves, ai2: plain a-b");
			//
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = this.nonEnhancedAi();
			outFile = "history-depth(-plain-ab-depth).txt";
//			aiPlayer1.historyHeuristic = false;
			aiPlayer1.MAX_DEPTH = 8;
			aiPlayer2.MAX_DEPTH = 8;
			runGames("ai1: no history heuristic, ai2: plain a-b");
			//
			aiPlayer1 = new AlphaBeta();
			aiPlayer2 = new AlphaBeta();
			outFile = "killermoves-depth.txt";
//			aiPlayer1.killermoves = false;
			aiPlayer1.MAX_DEPTH = 8;
			aiPlayer2.MAX_DEPTH = 8;
			runGames("ai1: no killer moves, ai2: plain a-b");
		}
	}

	private void runGames(String testMessage) {
		try {
			out = new PrintWriter(outFile);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		totalGames = 0;
		ai1Color = Board.WHITE;
		ai2Color = Board.BLACK;
		ai1Wins = 0;
		ai1Depth = 0;
		ai1Nodes = 0;
		ai1Researches = 0;
		ai2Wins = 0;
		ai2Depth = 0;
		ai2Nodes = 0;
		ai2Researches = 0;
		while (totalGames < games) {
			runGame();
			// Switch the colors so 50% is played as black/white
			ai1Color = ai1Color == Board.WHITE ? Board.BLACK : Board.WHITE;
			ai2Color = ai2Color == Board.WHITE ? Board.BLACK : Board.WHITE;
		}
		//
		writeOutput(testMessage);
		writeOutput("AI1: Average depth " + df1.format(ai1Depth / totalGames));
		writeOutput("AI1: Average num nodes " + df2.format(ai1Nodes / totalGames));
		writeOutput("AI1: Wins " + ai1Wins);
		writeOutput("AI1: Aspiration re-searches " + ai1Researches);
		writeOutput(" ");
		writeOutput("AI2: Average depth " + df1.format(ai2Depth / totalGames));
		writeOutput("AI2: Average num nodes " + df2.format(ai2Nodes / totalGames));
		writeOutput("AI2: Wins " + ai2Wins);
		writeOutput("AI2: Average aspiration re-searches " + ai2Researches);
	}

	private void writeOutput(String output) {
		if (out != null) { // Write output to file
			out.println(output);
			out.flush();
		}
		// Also write it to default output in case writing to file fails.
		System.out.println(output);
	}

	private AlphaBeta fullOptionsAi() {
		return new AlphaBeta();
	}

	private AlphaBeta nonEnhancedAi() {
		AlphaBeta temp = new AlphaBeta();
//		temp.transpositions = false;
//		temp.aspiration = false;
//		temp.nullmoves = false;
//		temp.historyHeuristic = false;
//		temp.killermoves = false;
		return temp;
	}

	public void runGame() {
		board = new Board();
		winner = Board.NONE_WIN;
		while (winner == Board.NONE_WIN) {
			//
			int move = -1;
			if (ai1Color == board.currentPlayer) {
				aiPlayer1.getMove(board.copy(), this, ai1Color, false);
				move = aiPlayer1.getBestMove();
			} else {
				aiPlayer2.getMove(board.copy(), this, ai2Color, false);
				move = aiPlayer2.getBestMove();
			}
			if (board.doMove(move, board.currentPlayer)) {
				if (board.capturePieces(move)) { // Returns false if suicide without capture
					winner = board.checkWin(board.board[move]);
				} else {
					System.err.println("Error, suicide move!");
				}
			} else {
				System.err.println("Error, invalid move!");
			}
		}
		// Bookkeeping
		totalGames++;
		if (winner == ai1Color) {
			writeOutput("Ai 1 wins.");
			ai1Wins++;
		} else if (winner == ai2Color) {
			writeOutput("Ai 2 wins.");
			ai2Wins++;
		} else {
			writeOutput("Draw.");
		}
		writeOutput(board.moveList.size() + " moves.");
		writeOutput(String.format("%f\t%f\t%f\t%f\t", aiPlayer1.averageDepth(),
				aiPlayer1.averageNodes(), aiPlayer2.averageDepth(), aiPlayer2.averageNodes()));
		ai1Depth += aiPlayer1.averageDepth();
		ai1Nodes += aiPlayer1.averageNodes();
		ai1Researches += aiPlayer1.averageResearches();
		//
		ai2Depth += aiPlayer2.averageDepth();
		ai2Nodes += aiPlayer2.averageNodes();
		ai2Researches += aiPlayer2.averageResearches();
		//
		aiPlayer1.resetStats();
		aiPlayer2.resetStats();
	}

	@Override
	public void makeMove(int position) {

	}
}
