package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MySQLParser {
	/**
	 * @param args
	 */

	public static void main(String[] args) {
		try{
			String sqlBlocks = readContentFromFile("E:\\MyJava\\MySQLParser\\bin\\main\\TestSQL\\CreateTableAS.txt");
			System.out.println(analyAllRefTable(sqlBlocks));
		}
		catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	//Analy all used table in the SQL block, result is ArrayList<String> of "seq|TargetTable|SourceTable" 
	private static ArrayList<String> analyAllRefTable(String sqlBlocks){
		SQLBlocks b = new SQLBlocks(sqlBlocks);
		//System.out.println("Total valid sql statements is : "+ b.getValidSQLList().size());
		int seq = 1;
		ArrayList<String> listAll = new ArrayList<String>();
		for(String sql : b.getValidSQLList()){
			for(String tables : Parser.analySourceTarget(sql)){
				listAll.add(String.valueOf(seq)+"|"+tables);
			}
			seq += 1;
		}
		return listAll;
	}
	
	private static String readContentFromFile(String fileName) throws IOException{		
		//InputStream is = MySQLParser.class.getResourceAsStream(fileName);
		InputStream is = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));		
		StringBuilder sb = new StringBuilder();
		String line ;		
		while((line = reader.readLine()) != null){
			sb.append(line + " ");
		}
		return sb.toString() ;
	}

}
