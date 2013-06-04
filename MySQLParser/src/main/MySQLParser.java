package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MySQLParser {
	/**
	 * @param args
	 */

	private static ArrayList<String> projList = new ArrayList<String>() ;
	private static ArrayList<String> sqlFileList = new ArrayList<String>() ;
		
	public static void main(String[] args) {
		//goProd();
		goTest();
	}
	
	private static void goTest(){
		try{
			String sqlBlocks = readContentFromFile("E:\\MyJava\\MySQLParser\\bin\\main\\TestSQL\\CreateTableAS.txt");
			System.out.println(analyAllRefTable(sqlBlocks));
		}
		catch(Exception e){
			System.out.println(e.toString());
		}
	}
	
	private static void goProd(){
		getProjectList();
		getAllSQLFiles();
		System.out.println("Total project counts : " + projList.size());
		System.out.println("Total SQL file counts : " + sqlFileList.size());
		doAJK();
	}
	
	//Analyze all used table in the SQL block, result is ArrayList<String> of "seq|TargetTable|SourceTable" 
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
	
	private static void getProjectList(){
		int i = 0;	//测试的时候控制循环
		
		projList.clear();
		File f = new File("E:\\MyJava\\dw\\");
		File[] files = f.listFiles();
		if(files != null){
			for(File singleFile : files){
				if(i < 10){
					projList.add(singleFile.getAbsolutePath());
				}
				//i++;
			}
		}
	}
	
	private static void getAllSQLFiles(){
		sqlFileList.clear();
		if(projList != null){
			for(String projName : projList){
				getSQLFileList(projName);
			}
		}
	}

	private static void getSQLFileList(String filePath){
		File f = new File(filePath);
		File[] files = f.listFiles();
		
		if(files != null){
			for(File singleFile : files){
				if(singleFile.isDirectory()){
					getSQLFileList(singleFile.getAbsolutePath());
				}
				else{
					if(singleFile.getName().toLowerCase().endsWith(".sql")){
						sqlFileList.add(singleFile.getAbsolutePath());
					}
				}
			}
		}
	}
	
	
	private static void doAJK(){
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter("E:\\ProjAnaly\\sql.txt",true));
			String sqlBlock = "";
			ArrayList<String> tmpRefList = new ArrayList<String>();
			
			for(String sqlFile : sqlFileList){
				sqlBlock = readContentFromFile(sqlFile);
				tmpRefList = analyAllRefTable(sqlBlock);
				for(String tmpRefTable : tmpRefList )
				{
					bw.write(sqlFile+"\t"+tmpRefTable);
					bw.newLine();
					bw.flush();
				}
			}
			bw.close();
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
	
}
