package memu;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

public class Main {

	public static void main(String[] args) throws Exception{
		
//		String location = args[0]; // specify the folder containing quantityDB and its profit table.
//		String fileName=args[1]; // quantityDB name
//		String quantityDBPath = location+fileName+".txt"; // unique quantityDB address
//		String profitPath=location+fileName+"_UtilityTable.txt";// unique profit table address
//		int beta=Integer.parseInt(args[2]); // used to generate MAU for each of item
//		int GLMAU = Integer.parseInt(args[3]); // global least minimum average utility
//        String HAUIsPath = args[4]; // specify the file storing the discovered HAUIs. If not specify, enter `null`
//        String statusPath = args[5]; // specify the file storing the running status of EHAUPM. If not specify, enter `null`
//
//
//		long startTimestamp = System.currentTimeMillis();
//
//		MEMU memu = new MEMU();
//		memu.runAlgorithm(HAUIsPath, profitPath, quantityDBPath, beta, GLMAU);
//
//
//		long endTimestamp = System.currentTimeMillis();
//
//        BufferedWriter statusWriter = null;
//        if(statusPath==null || statusPath.equalsIgnoreCase("null")){
//            statusWriter = new BufferedWriter(new OutputStreamWriter(System.out));
//        }else {
//            statusWriter = new BufferedWriter(new FileWriter(statusPath));
//        }
//
//        statusWriter.write(String.format("Total_time(s): %.2f\n", (endTimestamp - startTimestamp) / (double)1000));
//        statusWriter.write(String.format("Memory_usage:%.2f\n", memu.mur.maxConsumationMemory));
//        statusWriter.write("Haui_count:"+ memu.hauiCount +"\n");
//        statusWriter.write("Join_count:"+ memu.candidateCount +"\n");

		exe();
	}
	
	
	public static void exe() throws Exception{
		String location = "E:\\Data_mining\\dataset\\have_value_table\\";
		String fileName= "retail";
		String quantityDBPath = location+fileName+".txt";
		String profitPath=location+fileName+"_UtilityTable.txt";
		String HAUIsPath = "E:\\java_workspace\\MEMU\\output\\with_lub.txt";
		int GLMAU = 50000; // global least minimum average utility
		int beta= 800;

		long startTimestamp = System.currentTimeMillis();

		// Applying the HUIMiner algorithm
		MEMU memu = new MEMU();
		memu.runAlgorithm(HAUIsPath, profitPath, quantityDBPath, beta, GLMAU);
		
		// print time, memory, candidate, hauicount
		long endTimestamp = System.currentTimeMillis();
		
		System.out.printf("Runtime(s):%.2f\n" ,(endTimestamp - startTimestamp)/(double)1000);
		System.out.printf("Max_memory(MB):%.2f\n", memu.mur.getMaxMemory());
		System.out.println("HAUIs_count:" + memu.hauiCount);
		System.out.println("Candidates_count:" + memu.candidateCount);
		
	}
	
}
