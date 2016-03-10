package twophasejoin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import model.Cluster;
import util.SetCompResult;

public class CMCFilterTest {
    
    private static final int M = 5;
    private static final int G = 10;
    
    
    public ArrayList<Cluster> call(Iterable<ArrayList<Cluster>> v1)
	    throws Exception {
	ArrayList<ArrayList<Cluster>> tmp = new ArrayList<>();
	ArrayList<Cluster> result = new ArrayList<>();
	Iterator<ArrayList<Cluster>> itr = v1.iterator();
	while (itr.hasNext()) {
	    tmp.add(itr.next());
	}
	// sort the tmp using cluster timestamps;
	Collections.sort(tmp, new Comparator<ArrayList<Cluster>>() {
	    @Override
	    public int compare(ArrayList<Cluster> o1, ArrayList<Cluster> o2) {
		return o1.get(0).getTS() - o2.get(0).getTS();
	    }
	});
	// scan tmp list to find the local pattern
	for (int i = 0, len = tmp.size(); i < len; i++) {
	    HashSet<Cluster> tscurrent = new HashSet<>(tmp.get(i));
	    Iterator<Cluster> cluster_itr = tscurrent.iterator();
	    while (cluster_itr.hasNext()) {
		Cluster c = cluster_itr.next();
		boolean passed = false;
		for (int j = 1; j <= G && i + j < len; j++) {
		    passed = false;
		    HashSet<Cluster> tsnext = new HashSet<>(tmp.get(i + j));
		    for (Cluster n : tsnext) {
			SetCompResult sr = util.SetOps
				.setCompare(c.getObjects  (), n.getObjects());
			if (sr.getCommonsSize() < M) {
			    // continue;
			} else {
			    passed = true;
			    break; 
			}
		    }
		    if (!passed) {
			break;
		    }
		}
		if (!passed) {
		    // not proximate cluster within G-steps
		    cluster_itr.remove();
		} else {
		    result.add(c);
		}
	    }
	}
	return result;
    }
}
