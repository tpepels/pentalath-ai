package gui;

import game.Board;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.rush.HexGridCell;
import ai.*;

public class PentalathPanel extends JPanel implements MouseListener, MoveCallback, ActionListener {
	public static final short[] numbers = { 0, 0, 1, 2, 3, 4, 5, 0, 0, 0, 1, 2, 3, 4, 5, 6, 0, 0,
			0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7, 8, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 2, 3,
			4, 5, 6, 7, 8, 9, 0, 0, 3, 4, 5, 6, 7, 8, 9, 0, 0, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 5, 6,
			7, 8, 9, 0, 0, };
	//
	private static final long serialVersionUID = -7255477935485381647L;
	private static final int CELL_R = 40;
	//
	private int[] cornersY = new int[6], cornersX = new int[6];
	private HexGridCell hexagons = new HexGridCell(CELL_R);
	private Board board, tempBoard;
	//
	private boolean p1Human = true, p2Human = true;
	public boolean movenotation = true;
	public AlphaBeta aiPlayer1, aiPlayer2;
	private boolean aiThinking = false;
	private int movenum = 0;
	public String aiMessage = "";
	private Timer t = new Timer(1000, this);

	public PentalathPanel(Board board, boolean p1Human, boolean p2Human) {
		this.board = board;
		System.out.println("Initial hash: " + board.zobristHash);
		this.p1Human = p1Human;
		this.p2Human = p2Human;
		//
		if (!p1Human)
			aiPlayer1 = new AlphaBeta();
		if (!p2Human)
			aiPlayer2 = new AlphaBeta();
		addMouseListener(this);
		t = new Timer(1000, this);
		t.start();
	}

	public void undoMove() {
		board.undoMove();
		if (aiPlayer1 != null) {
			aiPlayer1.stop();
			aiMessage = "interupted";
			aiThinking = false;
		}
		if (aiPlayer2 != null) {
			aiPlayer2.stop();
			aiMessage = "interupted";
			aiThinking = false;
		}

		movenum--;
		repaint();
	}

	public void makeAIMove() {
		if (board.currentPlayer == 1 && !p1Human) {
			aiThinking = true;
			aiPlayer1.getMove(board.copy(), this, Board.WHITE, true);
			aiMessage = "Player 1, thinking ...";
		} else if (board.currentPlayer == 2 && !p2Human) {
			aiThinking = true;
			aiPlayer2.getMove(board.copy(), this, Board.BLACK, true);
			aiMessage = "Player 2, thinking ...";
		}
	}

	public void setBoard(Board board) {
		this.board = board;
		movenum = 0;
		//
		if (!p1Human)
			aiPlayer1 = new AlphaBeta();
		if (!p2Human)
			aiPlayer2 = new AlphaBeta();
		//
		repaint();
	}

	public void drawTempBoard(Board b) {
		this.tempBoard = board;
		this.board = b;
		repaint();
		this.board = this.tempBoard;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setFont(new Font("Arial", Font.PLAIN, 12));
		if (board.currentPlayer == Board.BLACK)
			g2d.setColor(Color.black);
		else
			g2d.setColor(Color.decode("#FFFFDD"));
		g2d.fillRect(0, 0, getWidth(), getHeight());
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//
		for (int i = 0; i < Board.WIDTH; i++) {
			for (int j = 0; j < Board.WIDTH; j++) {
				hexagons.setCellIndex(i, j);
				//
				if (Board.occupancy[i * Board.WIDTH + j] == 1) {
					hexagons.computeCorners(cornersY, cornersX);
					g2d.setColor(Color.decode("#FFE47A"));
					g2d.fillPolygon(cornersX, cornersY, 6);
					g2d.setColor(Color.DARK_GRAY);
					g2d.drawPolygon(cornersX, cornersY, 6);
					//
					g2d.setColor(Color.DARK_GRAY);
					if (board.board[i * Board.WIDTH + j].occupant == Board.BLACK) {
						g2d.setColor(Color.black);
						g2d.fillOval(cornersX[0] + 10, cornersY[0] - 5, 50, 50);
						g2d.setColor(Color.decode("#FFFFDD"));
					}
					//
					if (board.board[i * Board.WIDTH + j].occupant == Board.WHITE) {
						g2d.setColor(Color.decode("#FFFFDD"));
						g2d.fillOval(cornersX[0] + 10, cornersY[0] - 5, 50, 50);
						g2d.setColor(Color.black);
					}
					// g2d.drawString(Integer.toString(board.board[i * Board.WIDTH + j].position),
					// cornersX[0] + 17, cornersY[0] + 17);
					g2d.drawString(positionString(board.board[i * Board.WIDTH + j].position),
							cornersX[0] + 17, cornersY[0] + 17);
				}
			}
		}
		//
		if (board.currentPlayer == Board.WHITE)
			g2d.setColor(Color.black);
		else
			g2d.setColor(Color.decode("#FFFFDD"));
		//
		long endtime = board.totalTime;
		g2d.setFont(new Font("Arial", Font.BOLD, 14));
		g2d.drawString(String.format(
				"%d : %d",
				TimeUnit.MILLISECONDS.toMinutes(endtime),
				TimeUnit.MILLISECONDS.toSeconds(endtime)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endtime))),
				10, 15);
		g2d.drawString(aiMessage, 10, this.getHeight() - 15);
	}

	//
	private String positionString(int position) {
		if (movenotation) {
			char letter = 'a';
			int row = position / 9;
			letter += row;
			return String.format(letter + "" + numbers[position]);
		} else {
			return Integer.toString(position);
		}
	}

	public void setPlayer(int player, boolean human) {
		System.out.println("Player " + player + " human: " + human);
		if (player == 1) {
			this.p1Human = human;
			//
			if (!human) {
				this.aiPlayer1 = new AlphaBeta();
			}
		} else {
			this.p2Human = human;
			//
			if (!human) {
				this.aiPlayer2 = new AlphaBeta();
				aiPlayer2.transpositions = false;
				aiPlayer2.aspiration = false;
				aiPlayer2.nullmoves = false;
				aiPlayer2.historyHeuristic = false;
				aiPlayer2.killermoves = false;
			}
		}
	}

	public void pass() {
		board.pass();
		repaint();
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	private boolean isInsideBoard(int i, int j) {
		return i >= 0 && i < Board.WIDTH && j >= 0 && j < Board.WIDTH
				&& Board.occupancy[i * Board.WIDTH + j] == 1;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// Check if human player.
		if (board.currentPlayer == 1 && !p1Human) {
			return;
		} else if (board.currentPlayer == 2 && !p2Human) {
			return;
		}
		//
		hexagons.setCellByPoint(e.getY(), e.getX());
		int clickI = hexagons.getIndexI();
		int clickJ = hexagons.getIndexJ();
		//
		if (isInsideBoard(clickI, clickJ)) {
			makeMove(clickI * Board.WIDTH + clickJ);
		}

	}

	DecimalFormat df3 = new DecimalFormat("#,###,###,###,#00");

	@Override
	public void makeMove(int position) {
		aiMessage = "";
		aiThinking = false;
		if (board.doMove(position, board.currentPlayer)) {
			if (board.capturePieces(position)) { // Returns false if suicide without capture
				movenum++;
				PentalathGui.logMessage(df3.format(movenum) + ": player: "
						+ board.getOpponent(board.currentPlayer) + " move: "
						+ positionString(position));
				int winner = board.checkWin(board.board[position]);
				if (winner != Board.NONE_WIN) {
					String message = "";
					//
					if (winner == Board.BLACK_WIN) {
						message = "Black wins!";
					} else {
						message = "White wins!";
					}
					if (winner == Board.DRAW) {
						message = "It's a draw!";
					}
					//
					PentalathGui.logMessage(message);
					printStats();
				} else {
					// Check if the AI should make a move
					if (board.currentPlayer == 1 && !p1Human) {
						aiThinking = true;
						aiPlayer1.getMove(board.copy(), this, Board.WHITE, true);
						aiMessage = "Player 1, thinking ...";
					} else if (board.currentPlayer == 2 && !p2Human) {
						aiThinking = true;
						aiPlayer2.getMove(board.copy(), this, Board.BLACK, true);
						aiMessage = "Player 2, thinking ...";
					}
				}
			}
		}
		repaint();
	}

	public void printStats() {
		DecimalFormat df2 = new DecimalFormat("#,###,###,###,##0");
		DecimalFormat df1 = new DecimalFormat("#,###,###,###,##0.##");
		String message;
		if (!p1Human) {
			message = "WHITE: Average depth " + df1.format(aiPlayer1.averageDepth());
			message += "\nWHITE: Average num nodes " + df2.format(aiPlayer1.averageNodes());
			message += "\nWHITE: Total num nodes " + df2.format(aiPlayer1.totalnodes());
			message += "\nWHITE: Aspiration re-searches "
					+ df1.format(aiPlayer1.averageResearches());
			PentalathGui.logMessage(message);
		}
		if (!p2Human) {
			message = "BLACK: Average depth " + df1.format(aiPlayer2.averageDepth());
			message += "\nBLACK: Average num nodes " + df2.format(aiPlayer2.averageNodes());
			message += "\nBLACK: Total num nodes " + df2.format(aiPlayer2.totalnodes());
			message += "\nBLACK: Aspiration re-searches "
					+ df1.format(aiPlayer2.averageResearches());
			PentalathGui.logMessage(message);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (aiThinking) {
			board.totalTime -= 1000;
			repaint();
		}
	}
}
