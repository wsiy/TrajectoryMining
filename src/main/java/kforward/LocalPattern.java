package kforward;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import model.Cluster;
import model.Pattern;

import org.apache.spark.api.java.function.Function;

import util.SetCompResult;
import util.SetOps;


/**
 * mining local patterns based on the given set of snapshot clusters
 * @author a0048267
 * 
 */
public class LocalPattern implements Function<Iterable<SnapshotCluster>, Iterable<Set<Integer>>>{
   
    private static final long serialVersionUID = 7626346274874868740L;
    private int K, L,G,M;
    public LocalPattern(int L, int K, int M, int G) {
	this.K = K;
	this.L = L;
	this.M = M;
	this.G = G;
    }
    
    @Override
    public Iterable<Set<Integer>> call(Iterable<SnapshotCluster> v1)
	    throws Exception { 
	ArrayList<SnapshotCluster> tmp = new ArrayList<>();
	Iterator<SnapshotCluster> itr = v1.iterator();
	ArrayList<Set<Integer>> patterns = new ArrayList<>();
	
	while(itr.hasNext()) {
	    tmp.add(itr.next());
	}
	  
	Collections.sort(tmp, new SnapshotClusterComp());
	//at this point, the tmp list is sorted, use CMC to 
	//find the clusters which contains patterns
	//scan the snaphots from first to the end;
	HashSet<Pattern> candidate_sets = new HashSet<>();
	
	int start = 0;
	int end = tmp.size();
	for (int t = start; t < end; t++) {
	    ArrayList<Cluster> clusters = tmp.get(t).getClusters();
	    // see whether any cluster can extend the candidate
	    for (Cluster cluster : clusters) {
		Set<Integer> objects = cluster.getObjects();
		if (t == start) {
		    if (objects.size() >= M) {
			Pattern p = new Pattern();
			p.insertObjects(objects);
			p.insertTime(start);
			candidate_sets.add(p);
		    }
		} else {
		    // intersect with existing patterns
		    Set<Pattern> tobeadded = new HashSet<Pattern>();
		    boolean singleton = true; // singleton checks if the current
					      // cluster does not extend any
					      // pattern
		    for (Pattern p : candidate_sets) {
			if (p.getLatestTS() < t) {
			    // here we discuss three cases,
			    // p \subseteq c,
			    // c \subseteq p
			    // p \cap c \neq \emptyset
			    Set<Integer> pattern_objects = p.getObjectSet();
			    SetCompResult scr = SetOps.setCompare(
				    pattern_objects, objects);
			    if (scr.getStatus() == 0) {
				// no containments occur
				if (scr.getCommonsSize() >= M) {
				    // make a new pattern;
				    Pattern newp = new Pattern();
				    newp.insertPattern(scr.getCommons(),
					    p.getTimeSet());
				    newp.insertTime(t);
				    tobeadded.add(newp);
				}
			    } else if (scr.getStatus() == 1) {
				// pattern contain objects
				// this coincide with status 0
				singleton = false;
			    } else if (scr.getStatus() == 2) {
				// object contains pattern
				// object itself needs to be a pattern
				Pattern newp2 = new Pattern();
				// newp2.insertPattern(objects,
				// new ArrayList<Integer>(), t);
				newp2.insertPattern(objects,
					new ArrayList<Integer>());
				// extend newp2;
				newp2.insertTime(t);
				tobeadded.add(newp2);
				p.insertTime(t);
				singleton = false;
			    } else if (scr.getStatus() == 3) {
				// object equals pattern
				// extends p by one more timestamp
				p.insertTime(t);
				singleton = false;
			    }
			}
		    }
		    if (singleton) {
			// create a pattern for objects at time t
			Pattern newp2 = new Pattern();
			newp2.insertPattern(objects, new ArrayList<Integer>());
			newp2.insertTime(t);
			tobeadded.add(newp2);
		    }
		    candidate_sets.addAll(tobeadded);
		}
	    }
	    // before moving to next sequence, filter all necessary patterns
	    HashSet<Pattern> toberemoved = new HashSet<>();
	    for (Pattern p : candidate_sets) {
		if (t != p.getLatestTS()) {
		    // check l-consecutiveness
		    List<Integer> sequences = p.getTimeSet();
		    int cur_consecutive = 1;
		    for (int ps = 1, len = sequences.size(); ps < len; ps++) {
			if (sequences.get(ps) - sequences.get(ps - 1) == 1) {
			    cur_consecutive++;
			} else {
			    if (cur_consecutive < L) {
				toberemoved.add(p);
				break;
			    } else {
				cur_consecutive = 0;
			    }
			}
		    }
		}
		if (t - p.getLatestTS() > G) {
		    toberemoved.add(p);
		}
		// this should not happen
		if (p.getObjectSet().size() < M) {
		    toberemoved.add(p);
		}
	    }
	    candidate_sets.removeAll(toberemoved);
	}
	for (Pattern p : candidate_sets) {
	    if (p.getTimeSet().size() >= K) {
		patterns.add(p.getObjectSet());
	    }
	}
	return patterns;
    }
    
    /**
     * a unit-test
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
	SnapshotCluster sc1 = new SnapshotCluster(0);
	SnapshotCluster sc2 = new SnapshotCluster(1);
	SnapshotCluster sc3 = new SnapshotCluster(2);
	SnapshotCluster sc4 = new SnapshotCluster(3);
	SnapshotCluster sc5 = new SnapshotCluster(4);
	SnapshotCluster sc6 = new SnapshotCluster(5);
	SnapshotCluster sc7 = new SnapshotCluster(6);
	SnapshotCluster sc8 = new SnapshotCluster(7);
	SnapshotCluster sc9 = new SnapshotCluster(8);
	SnapshotCluster sc10 = new SnapshotCluster(9);
	ArrayList<SnapshotCluster> clusters = new ArrayList<>();
	clusters.add(sc1);
	clusters.add(sc2);
	clusters.add(sc3);
	clusters.add(sc4);
	clusters.add(sc5);
	clusters.add(sc6);
	clusters.add(sc7);
	clusters.add(sc8);
	clusters.add(sc9);
	clusters.add(sc10);
	
	sc1.addCluster(new Cluster(1, "11", new int[]{1,2,3}));
	sc1.addCluster(new Cluster(1, "12", new int[]{3,4,5}));
	sc1.addCluster(new Cluster(1, "13", new int[]{6,7,8}));
	
	sc2.addCluster(new Cluster(2, "21", new int[]{1,2,3}));
	sc2.addCluster(new Cluster(2, "22", new int[]{3,4,5}));
	sc2.addCluster(new Cluster(2, "23", new int[]{6,7,8}));
	
	sc3.addCluster(new Cluster(3, "31", new int[]{1,2,3}));
	sc3.addCluster(new Cluster(3, "32", new int[]{3,4,5}));
	sc3.addCluster(new Cluster(3, "33", new int[]{6,7,8}));
	
	sc4.addCluster(new Cluster(4, "41", new int[]{1,2,3}));
	sc4.addCluster(new Cluster(4, "42", new int[]{3,4,5}));
	sc4.addCluster(new Cluster(4, "43", new int[]{6,7,8}));
	
	
	
	
	LocalPattern lp = new LocalPattern(5,10,4,3);
	Iterable<Set<Integer>> result = lp.call(clusters);
	System.out.println(result);
    }
}

class SnapshotClusterComp implements Comparator<SnapshotCluster>, Serializable {
    private static final long serialVersionUID = -7921743971973093960L;
    @Override
    public int compare(SnapshotCluster o1, SnapshotCluster o2) {
	return o1.getTS() - o2.getTS();
    }
    
}
