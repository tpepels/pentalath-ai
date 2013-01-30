package ai;
import game.Board;

public interface AIPlayer {
	public void getMove(Board board, MoveCallback callback, int myPlayer, boolean parallel);
}
