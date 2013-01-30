package game;


public class Field {
	public int occupant = Board.FREE;
	public int position, freedom;
	public Field[] neighbours = new Field[6];
	
	public Field(int position) {
		this.position = position;
	}
}
