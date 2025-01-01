package org.flightgear.terramaster;

import java.util.Collection;
import java.util.Map;

public interface TileService {

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

  Map<TileName, TileData> newScnMap();
}
