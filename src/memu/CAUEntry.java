package memu;

public class CAUEntry {

	final int tid;

	final int utility;

	int rmu;

	int remu;
	
	/**
	 * Constructor.
	 * @param tid  the transaction id
	 * @param utility  the itemset utility
	 * @param remu  the maximal utility of the transaction
	 */
	public CAUEntry(int tid, int utility, int rmu, int remu){
		this.tid=tid;
		this.utility = utility;
		this.rmu = rmu;
		this.remu = remu;
	}
}
