package org.flightgear.terramaster;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface TileService {

  /**Set the TerraSync scenery path*/
	void setScnPath(File file);

	/**Start the Thread*/
	void start();
	

	/**Add the tiles to the queue*/
	void sync(Collection<Syncable> set, boolean ageCheck);

	/***/
	Collection<Syncable> getSyncList();

	void quit();

	void cancel();

	void delete(Collection<TileName> selection);

	void restoreSettings();

  void wakeUp();

  Map<TileName, TileData> newScnMap(String path);
}
