package memu;



import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;




public class MEMU {

	// The number of high-average-utility itemsets generated */
	public int hauiCount =0; 
	
	// The number of candidate high-utility itemsets */
	public long candidateCount =0;
	
	// Map to remember the auub value of each item */
	Map<Integer, Long> item2auub = null;

	// Map item to MAU
    Map<Integer, Integer> item2mau = null;
	
	// The eaucs structure:  key: item   key: another item   value: auub */
	Map<Integer, Map<Integer, Long>> EAUCS=null;  

	
	// Buffer for current itemset
	int BUFFERS_SIZE = 200;
	private int[] itemsetBuffer = null;

    // Write HAUIs in a specified file
    BufferedWriter writer = null;

    public MemoryUpdateRunnable mur = new MemoryUpdateRunnable();
    int LeastMAU = 0;

	// This class represent an item and its utility in a transaction */
	class Pair{
		int item = 0;
		int utility = 0;
	}
	
	public MEMU() {

	}
	// read profit of each item in database
	private Map<Integer, Integer> readProfits(String profitPath) throws Exception{
		Map<Integer, Integer> item2profits = new HashMap<>();
		BufferedReader in = new BufferedReader(new FileReader(profitPath));
		String line = null;
		String[] pair = null;
		while ( (line = in.readLine())!=null){
			pair = line.split(", ");
			item2profits.put(Integer.parseInt(pair[0].trim()),
					Integer.parseInt(pair[1].trim()));
		}
		in.close();
		return item2profits;
	}
	


	/**
	 * Run the algorithm
	 * @param HAUIsPath Specify the file used to keep the discovered HAUIs
	 * @param quantityDBPath the input file path
	 * @param beta the constant used to randomly generate threshold for each items in DB
	 * @param GLMAU the global minimum utility threshold
	 * @throws IOException exception if error while writing the file
	 */
	public void runAlgorithm(String HAUIsPath, String profitPath ,String quantityDBPath, final int beta, final int GLMAU) throws Exception {
		// reset maximum
		Thread timeThread = new Thread(mur);
		mur.isTestMem = true;
		timeThread.start();
		
		// initialize the buffer for storing the current itemset
		itemsetBuffer = new int[BUFFERS_SIZE];
		EAUCS =  new HashMap<>();
		

		item2auub = new HashMap<Integer, Long>();

		item2mau = new HashMap<>();

		// Initialize writer
        if(HAUIsPath!=null && !HAUIsPath.equalsIgnoreCase("null"))
            writer = new BufferedWriter(new FileWriter(HAUIsPath));

		// Read items' profit
		Map<Integer, Integer> item2profits = readProfits(profitPath);

		// Generate MAU for each item
		LeastMAU = Integer.MAX_VALUE;
		for(Entry<Integer,Integer> entry : item2profits.entrySet()){
			int val= Math.max(entry.getValue() * beta, GLMAU);
			LeastMAU = LeastMAU>val ? val : LeastMAU;
            item2mau.put(entry.getKey(), val);
		}


		// Scan the database a first time to calculate the auub of each item.
		BufferedReader DBReader = null;
		String curTran;
		try {

			DBReader = new BufferedReader(new InputStreamReader( new FileInputStream(new File(quantityDBPath))));
			// for each transaction until the end of file
			int quantity, itemName;
			while ((curTran = DBReader.readLine()) != null) {
				String items[] = curTran.split(", ");
				int maxItemUtility = -1;
				for(int i=0; i <items.length; i+=2){
					quantity = Integer.parseInt(items[i].trim());
					itemName = Integer.parseInt(items[i+1].trim());
					int tmputility = quantity * item2profits.get(itemName);
					if(maxItemUtility < tmputility){
						maxItemUtility = tmputility;
					}
				}
				for(int i=0; i <items.length; i+=2){
					itemName = Integer.parseInt(items[i+1].trim());
					Long auub = item2auub.get(itemName);
					// add the utility of the item in the current transaction to its AUUB
					auub = (auub == null)? 
							maxItemUtility : auub + maxItemUtility;
					item2auub.put(itemName, auub);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(DBReader != null){
				DBReader.close();
			}
	    }

        // Container of CAUList of each item whose auub value >= leastMAU.
		List<CAUList> listOfCAULists = new ArrayList<CAUList>();

        // Key: item, Value:CAUList
		Map<Integer, CAUList> mapItemToUtilityList = new HashMap<Integer, CAUList>();
		

		for(Entry<Integer,Long> entry: item2auub.entrySet()){
			Integer item = entry.getKey();
			if(item2auub.get(item) >= LeastMAU){
				CAUList uList = new CAUList(item);
				mapItemToUtilityList.put(item, uList);
				listOfCAULists.add(uList);
			}
		}

		//  Sort CAUList according to its mau-ascending order
		Collections.sort(listOfCAULists, new Comparator<CAUList>(){
			public int compare(CAUList o1, CAUList o2) {
				return compareItems(o1.item, o2.item);
			}
		} );
		

		// Scan DB again to construct CAUList of each item whose auub value >= minUtility.
		try {

			DBReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(quantityDBPath))));
			int tid =0;
			int quantity,itemName;
			while ((curTran = DBReader.readLine()) != null) {
				String items[] = curTran.split(", ");
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				int maxUtility =0;
				for(int i=0; i <items.length; i+=2){
					quantity = Integer.parseInt(items[i].trim());
					itemName = Integer.parseInt(items[i+1].trim());
					int tmputility = quantity * item2profits.get(itemName);
					
					Pair pair = new Pair();
					pair.item = itemName;
					pair.utility = tmputility;
					

					// if the item's auub value >= LeastMAU
					if(item2auub.get(pair.item) >= LeastMAU){
						if(maxUtility < pair.utility){
							maxUtility = pair.utility;
						}
						revisedTransaction.add(pair);
					}
				}

				Collections.sort(revisedTransaction, new Comparator<Pair>(){
					public int compare(Pair o1, Pair o2) {
						return compareItems(o1.item, o2.item);
					}
				});

				int rmu=0, remu=0;
				for(int i = revisedTransaction.size()-1; i>=0; --i){
					Pair pair =  revisedTransaction.get(i);
					rmu = pair.utility > rmu ? pair.utility : rmu;
					// get the utility list of this item
					CAUList CAUListOfItem = mapItemToUtilityList.get(pair.item);
					// Add a new CAUEntry to the utility list of this item corresponding to this transaction
					CAUEntry CAUEntry =null;

                    CAUEntry = new CAUEntry(tid, pair.utility, rmu, remu);

					CAUListOfItem.addElement(CAUEntry);

                    remu = (remu<pair.utility) ? pair.utility : remu;
				}

				for(int i = 0; i<revisedTransaction.size(); ++i){
					Pair pair =  revisedTransaction.get(i);
						Map<Integer, Long> subEAUCS = EAUCS.get(pair.item);
						if(subEAUCS == null) {
							subEAUCS = new HashMap<Integer, Long>();
							EAUCS.put(pair.item, subEAUCS);
						}
						for(int j = i+1; j< revisedTransaction.size(); ++j){
							Pair pairAfter = revisedTransaction.get(j);
							Long twoAuub = subEAUCS.get(pairAfter.item);
							if(twoAuub == null) {
								subEAUCS.put(pairAfter.item, (long)(maxUtility));
							}else {
								subEAUCS.put(pairAfter.item, twoAuub + maxUtility);
							}
						}
				}
				tid++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(DBReader != null){
				DBReader.close();
			}
	    }

//	    System.out.println("process order");
//	    for(CAUList x:listOfCAULists){
//		    System.out.print(x.item +",");
//        }
//        System.out.println();

		// Mine the database recursively
		search(itemsetBuffer, 0, null, listOfCAULists, 0);

		if(writer!=null)
		    writer.close();

		mur.isTestMem =false;

	}
	

	private int compareItems(int item1, int item2) {
		int compare = (int)( item2mau.get(item1) - item2mau.get(item2));
		return (compare == 0)? item1 - item2 :  compare;
	}
	/**
	 * @param prefix  This is the current prefix. Initially, it is empty.
	 * @param P This is the Utility List of the prefix. Initially, it is empty.
	 * @param CAUListOfP The utility lists corresponding to each extension of the prefix.
	 * @param sumMAUOfPrefix The sum of mau of prefix.
	 * @param prefixLength The current prefix length
	 * @throws IOException
	 */
	private void search(int [] prefix, int prefixLength, CAUList P, List<CAUList> CAUListOfP, int sumMAUOfPrefix)
			throws IOException {
		
		// For each extension X of prefix P
		for(int i=0; i< CAUListOfP.size(); i++){
			CAUList X = CAUListOfP.get(i);

			int sumMAUOfPx= (item2mau.get(X.item) + sumMAUOfPrefix);


			// we save the itemset:  pX
			if(X.sumUtility >= sumMAUOfPx) {
				hauiCount++;
				if(writer!=null)
				    writeOut(prefix, prefixLength, X.item, X.sumUtility / (double)(prefixLength+1), sumMAUOfPx / (double)(prefixLength+1));
			}

			// Check looser Upper bound
            if((X.sumUtility + X.sumOfRemu*(prefixLength+1) ) < sumMAUOfPx ) {
			    // output
				//Create a string buffer
//                StringBuilder buffer = new StringBuilder();
//                // append the prefix
//                for (int k = 0; k < prefixLength; k++) {
//                    buffer.append(prefix[k]+",");
//                }
//                // append the last item
//                buffer.append(X.item);
//                // append the utility value
//                buffer.append(":");
//                buffer.append(String.format("%.2f", X.sumUtility / (double)(prefixLength+1)));
//                buffer.append(":"+ String.format("%.2f", sumMAUOfPx / (double)(prefixLength+1)));
//                buffer.append(":" + String.format("%.2f", (X.sumUtility / (double)(prefixLength+1) + X.sumOfRemu)));
//                System.out.print("length:"+ (prefixLength+1) +" ");
//                System.out.print("sumOfRemu:" + X.sumOfRemu +"  ");
//                System.out.println(buffer.toString());

                continue;
            }


			// Check revised tighter upper bound
			if(X.sumOfRmu * (prefixLength+1) >= sumMAUOfPx) {
				// This list will contain the CAU lists of pX 1-extensions.
				List<CAUList> exULs = new ArrayList<>();
				// For each extension of p appearing
				// after X according to the ascending order
				for(int j=i+1; j < CAUListOfP.size(); j++) {
					CAUList Y = CAUListOfP.get(j);

					// MAUCP strategy
                    Map<Integer, Long> auub1 = EAUCS.get(X.item);
                    if(auub1 != null) {
                        Long auub2 = auub1.get(Y.item);
                        long mauOfPrefix = 0;
                        if(prefixLength!=0) mauOfPrefix = sumMAUOfPrefix / prefixLength;
                        if(auub2 == null || auub2 < Math.max(LeastMAU, mauOfPrefix)) {
                            continue;
                        }
                    }

					candidateCount++;

					// Construct  new itemset pXY and its CAU-List
					// and add it to the list of extensions of pX
					CAUList pxy = construct(prefixLength+1, P, X, Y, sumMAUOfPx);
					if(pxy != null) {
						exULs.add(pxy);
					}
				}
                // Allocate new buffer when buffer size get small
                if(prefixLength==BUFFERS_SIZE) {
                    BUFFERS_SIZE = BUFFERS_SIZE + (int)(BUFFERS_SIZE/4);
                    int[] tmp = new int[BUFFERS_SIZE];
                    System.arraycopy(itemsetBuffer,0, tmp, 0, prefixLength);
                    itemsetBuffer = tmp;
                }
				// Create new prefix pX
				itemsetBuffer[prefixLength] = X.item;
				// Recursive call to discover all itemsets with the prefix pX
				search(itemsetBuffer, prefixLength+1, X, exULs, sumMAUOfPx);
			}
		}
	}

    private CAUList construct(int prefixLen, CAUList P, CAUList px, CAUList py, long sumMAUOfPx) {
        // create an empy utility list for Pxy
        CAUList pxyUL = new CAUList(py.item);

        long sumOfRmu = px.sumOfRmu;
        long sumOfRemu = (long)(px.sumUtility / (double)prefixLen + px.sumOfRemu);

        // For each element in the utility list of pX
        for(CAUEntry ex : px.CAUEntries) {
            // Do a binary search to find element ey in py with tid = ex.tid
            CAUEntry ey = findElementWithTID(py, ex.tid);
            if(ey == null) {
                sumOfRmu -= ex.rmu;
                sumOfRemu -= (ex.utility /(double)prefixLen + ex.remu);
                if(Math.min(sumOfRemu, sumOfRmu) * prefixLen < sumMAUOfPx) {
                    return null;
                }
                continue;
            }

            // If the prefix p is null
            if(P == null){
                // Create the new element
                CAUEntry eXY = new CAUEntry(ex.tid, ex.utility + ey.utility, ex.rmu, ey.remu);

                // add the new element to the utility list of pXY
                pxyUL.addElement(eXY);

            } else {
                // find the element in the utility list of p wih the same tid
                CAUEntry e = findElementWithTID(P, ex.tid);
                if(e != null){
                    // Create new element
                    CAUEntry eXY = new CAUEntry(ex.tid, ex.utility + ey.utility - e.utility,
                            ex.rmu, ey.remu);
                    // add the new element to the utility list of pXY
                    pxyUL.addElement(eXY);
                }
            }
        }
        // return the utility list of pXY.
        return pxyUL;
    }

    private CAUList construct_opt(int prefixLen,CAUList P, CAUList Px, CAUList Py, long sumMAUOfPx) {
        // create an empty utility list for pXY
        CAUList pxyUL = new CAUList(Py.item);
        long rtubOfPx = Px.sumOfRmu;
        long lubOfPx =(long)(Px.sumUtility / (double)prefixLen + Px.sumOfRemu) ;
        int tidPx=0, tidPy=0;

        while(tidPx < Px.CAUEntries.size() && tidPy < Py.CAUEntries.size()) {
            CAUEntry ex = Px.CAUEntries.get(tidPx);
            CAUEntry ey = Py.CAUEntries.get(tidPy);

            if(ex.tid==ey.tid) {
                if(P!=null) {
                    CAUEntry e = findElementWithTID(P, ex.tid);
                    if(e!=null) {
                        // Create the new element
                        CAUEntry eXY = new CAUEntry(ex.tid, ex.utility + ey.utility-e.utility, ex.rmu, ey.remu);
                        // add the new element to the utility list of pXY
                        pxyUL.addElement(eXY);
                    }
                } else {
                    // Create the new element
                    CAUEntry eXY = new CAUEntry(ex.tid, ex.utility + ey.utility, ex.rmu, ey.remu);
                    // add the new element to the utility list of pXY
                    pxyUL.addElement(eXY);
                }

                ++tidPx; ++tidPy;
            } else if (ex.tid > ey.tid) {
                tidPy++;
            }
            else {
                ++tidPx;
                rtubOfPx -= ex.rmu;
                lubOfPx -= (ex.utility / (double)prefixLen + ex.remu);
                if(Math.min(rtubOfPx,lubOfPx) * prefixLen < sumMAUOfPx) {
                    return null;
                }
            }
        }

        return pxyUL;
    }

	/**
	 * Do a binary search to find the element with a given tid in a utility list
	 * @param ulist the utility list
	 * @param tid  the tid
	 * @return  the element or null if none has the tid.
	 */
	private CAUEntry findElementWithTID(CAUList ulist, int tid){
		List<CAUEntry> list = ulist.CAUEntries;
		
		// perform a binary search to check if  the subset appears in  level k-1.
        int first = 0;
        int last = list.size() - 1;
       
        // the binary search
        while( first <= last )
        {
        	int middle = ( first + last ) >>> 1; // divide by 2

            if(list.get(middle).tid < tid){
            	first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            }
            else if(list.get(middle).tid > tid){
            	last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            }
            else{
            	return list.get(middle);
            }
        }
		return null;
	}

    //		//Create a string buffer
    private void writeOut(int[] prefix, int prefixLength, int item, double average_utility, double mau) throws IOException {



        //Create a string buffer
        StringBuilder buffer = new StringBuilder();
        // append the prefix
        for (int i = 0; i < prefixLength; i++) {
            buffer.append(prefix[i]+",");
        }
        // append the last item
        buffer.append(item);
        // append the utility value
        buffer.append(":");
        buffer.append(String.format("%.2f", average_utility));
        buffer.append(":"+ String.format("%.2f", mau));
        // write to file
        writer.write(buffer.toString());
        writer.newLine();

    }

	
    class MemoryUpdateRunnable implements Runnable {
    	boolean isTestMem;
    	double maxConsumationMemory;
		@Override
		public void run() {
			while (this.isTestMem) {
				double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime
						.getRuntime().freeMemory()) / 1024d / 1024d;
			 
				if(currentMemory > maxConsumationMemory) {
					maxConsumationMemory = currentMemory;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
				}
			}
		}
		
		public  double getMaxMemory(){
			return maxConsumationMemory;
		}
	}
}