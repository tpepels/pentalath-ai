package ai;

public class Move {
	public final int position;
	public int history;
	//
	public Move(int position) {
		this.position = position;
		history = 0;
	}
}
