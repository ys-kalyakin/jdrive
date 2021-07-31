package game.struct;


public class DrawTownTileStruct {


	//SpriteID sprite_1;
	//SpriteID sprite_2;
	public int sprite_1;
	public int sprite_2;

	public byte subtile_x;
	public byte subtile_y;
	public byte width;
	public byte height;
	public byte dz;
	public byte proc;

	/** Writes the data into the Town Tile Drawing Struct
	 * @param s1 The first sprite of the building, mostly the ground sprite
	 * @param s2 The second sprite of the building.
	 * @param sx The x-position of the sprite within the tile
	 * @param xy the y-position of the sprite within the tile
	 * @param w the width of the sprite
	 * @param h the height of the sprite
	 * @param dz the virtual height of the sprite
	 * @param p set to 1 if a lift is present
	 * @see DrawTownTileStruct
	 */

	public DrawTownTileStruct(int s1, int s2, int sx, int sy, int w, int h, int dz, int p) {
		// {s1, s2, sx, sy, w - 1, h - 1, dz, p}
		this.sprite_1 = s1;
		this.sprite_2 = s2;

		this.subtile_x = (byte) sx;
		this.subtile_y = (byte) sy;
		this.width = (byte) (w - 1);
		this.height = (byte) (h - 1);
		this.dz = (byte) dz;
		this.proc = (byte) p;
	}	

}
