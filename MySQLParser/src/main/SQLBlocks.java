package main;

import java.util.ArrayList;

public class SQLBlocks {

	private String sqlStatements = "";
	private ArrayList<String> listSQLStatements = new ArrayList<String>();
	
	public ArrayList<String> getValidSQLList(){
		return this.listSQLStatements;
	}
	
	public SQLBlocks(String sqls){
		this.sqlStatements = sqls;
		this.clearSQL();
	}
	
	private void clearSQL(){
		//delete comments
		//replace multiple spaces with one space
		//replace "overwrite table" with "into" to be compatible with Hive QL
		//replace "replace into" with "insert into"
		sqlStatements = sqlStatements.replaceAll("(\\/\\*.*?\\*\\/)?", "").replaceAll("\\s+", " ").replaceAll("(?i)overwrite\\s+table", "into").replaceAll("(?i)replace\\s+into", "insert into"); 
		
		for(String sql : sqlStatements.split(";")){
			if(!Parser.testSQLType(sql).equals("")){
				listSQLStatements.add(sql);
			}
		}
	}
}
