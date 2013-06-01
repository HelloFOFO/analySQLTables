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
		sqlStatements = sqlStatements.replaceAll("(\\/\\*.*?\\*\\/)?", "").replaceAll("\\s+", " "); 
		
		for(String sql : sqlStatements.split(";")){
			if(!Parser.testSQLType(sql).equals("")){
				listSQLStatements.add(sql);
			}
		}
	}
}
