package osm;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.MapDataDao;
import de.westnordost.osmapi.overpass.OverpassMapDataDao;
import de.westnordost.osmapi.overpass.OverpassStatus;

public class ImportShops {
	public static void main (String[] args) {
		//FIXME doesn't work (do we need that api of send straight HTTP requests?)
	    OverpassStatus result = new OverpassStatus();
	OsmConnection connection = new OsmConnection("https://overpass-api.de/api/", "ICI");
	OverpassMapDataDao overpass = new OverpassMapDataDao(connection);
	MapDataDao map = new MapDataDao(connection);
	System.out.println(overpass.queryCount("[out:csv(name)];\n" + 
			"node[amenity](bbox:2.3367,88.8375,2.3674,88.8553);\n" + 
			"for (t[\"amenity\"])\n" + 
			"{\n" + 
			"  make ex name=_.val;\n" + 
			"  out;\n" + 
			"}"));
	
	}
}
