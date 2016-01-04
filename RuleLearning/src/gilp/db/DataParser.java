package gilp.db;

import java.util.ArrayList;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

import gilp.rdf.Triple;

import java.util.Iterator;

/*
 * DataParser is to parse the triples stored in a text file. 
 * CJC 2015.10.28
 * */

public class DataParser {
	
	public static ArrayList<Triple> _dataset = null; 	
	
	static String data_file = "dataset.txt";
	
	public static ArrayList<Triple> getDataset(){
		if (_dataset == null) 
			_dataset = loadData();
		return _dataset;
	}

	
	public static ArrayList<Triple> loadData(){
		 
		String fileName = DataParser.class.getClassLoader().getResource("../../" + data_file).getPath();
		 
		RandomAccessFile file; 
		ArrayList<Triple>  listTriples = new ArrayList<Triple>();
		try{
			file = new RandomAccessFile(fileName,"r"); 
			String line = file.readLine(); 
			while (line!=null){
				listTriples.add(parseLine(line));	
				line = file.readLine(); 
			}
			file.close();
		}
		catch (Exception ex){
			ex.printStackTrace(System.out);
		}
		return listTriples;
	}
	
	static Triple parseLine(String line)	{
		StringTokenizer st = new StringTokenizer(line, "<> ");
		String tid, s, p, o;
		tid = s = p = o = "";

		int i = 0;
		while (st.hasMoreTokens()){
			switch (i){
			case 0: 
				tid = st.nextToken();
				break;
			case 1:
				s = st.nextToken();
				break;
			case 2:
				p = st.nextToken();
				break;
			default:
				o = st.nextToken();
			}
			i++;
		}
		
		Triple triple = new Triple(s, p, o);
		triple.set_tid(tid);
		return triple;
	}
	
	//unit test
	public static void main(String[] args){
		ArrayList<Triple> dataSet = loadData();
		Iterator<Triple> myIterator = dataSet.iterator();
		while (myIterator.hasNext()){
			System.out.println((Triple)myIterator.next());
		}
	}

}
