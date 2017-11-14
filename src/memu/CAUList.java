package memu;


import java.util.ArrayList;
import java.util.List;


public class CAUList {
    // the item
	int item;
    // the sum of item utilities
	long sumUtility = 0;
    // the sum of revised maximal utilities
	long sumOfRmu = 0;
	// the sum of remaining maximal utilities
    long sumOfRemu = 0;

    // List container for each entry of item
	List<CAUEntry> CAUEntries = new ArrayList<CAUEntry>();
	 

	public CAUList(Integer item){
		this.item = item;
	}
	

	public void addElement(CAUEntry CAUEntry){
		sumUtility += CAUEntry.utility;
		sumOfRmu += CAUEntry.rmu;
		sumOfRemu += CAUEntry.remu;
		CAUEntries.add(CAUEntry);
	}
}
