package main;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parser {
	
	public static String PAT_SELECT_INTO_OUTFILE = "(?i)\\s*(SELECT.*)\\s+INTO\\s+OUTFILE\\s+(.*)";
	public static String SQLTYPE_SELECT_INTO_OUTFILE = "SELECT_INTO_OUTFILE";
	
	public static String PAT_CREATE_TABLE_AS = "(?i)\\s*CREATE\\s+TABLE\\s+(\\S*)\\s+(?:AS)?(.*)";
	public static String SQLTYPE_CREATE_TABLE_AS = "CREATE_TABLE_AS";
	
	public static String PAT_INSERT_INTO = "(?i)\\s*INSERT\\s+(?:IGNORE\\s+)?(?:INTO\\s+)?(.*?)(?:\\(.*?\\))?\\s+(SELECT.*)";
	public static String SQLTYPE_INSERT_INTO = "INSERT_INTO";
	
	public static String PAT_LOAD_DATA_INFILE = "(?i)\\s*LOAD\\s+DATA\\s+(?:LOCAL\\s+)?(?:INFILE|INPATH)\\s+(.*?)\\s+(?:OVERWRITE\\s+)?INTO\\s+TABLE\\s+(.*?)(?:\\s+.*)?";	
	public static String SQLTYPE_LOAD_DATA_INFILE = "LOAD_DATA_INFILE";
	
	public static String PAT_UPDATE = "(?i)\\s*UPDATE\\s+.*";
	public static String SQLTYPE_UPDATE = "UPDATE";

	public static String testSQLType(String sql){
		String sqlType = "";
		
		if(sql.matches(Parser.PAT_CREATE_TABLE_AS)){
			sqlType = Parser.SQLTYPE_CREATE_TABLE_AS;
			return sqlType;
		}
		
		if(sql.matches(Parser.PAT_INSERT_INTO)){
			sqlType = Parser.SQLTYPE_INSERT_INTO;
			return sqlType;
		}
		
		if(sql.matches(Parser.PAT_LOAD_DATA_INFILE)){
			sqlType = Parser.SQLTYPE_LOAD_DATA_INFILE;
			return sqlType;
		}
		
		if(sql.matches(Parser.PAT_SELECT_INTO_OUTFILE)){
			sqlType = Parser.SQLTYPE_SELECT_INTO_OUTFILE;
			return sqlType;
		}
		
		if(sql.matches(Parser.PAT_UPDATE)){
			sqlType = Parser.SQLTYPE_UPDATE;
			return sqlType;
		}
		
		return sqlType;
	}

	//check if count of "(" equals count of ")"
	public static boolean checkDoubleBracket(String sql){
		return (sql.replaceAll("\\(", "").length() == sql.replaceAll("\\)", "").length());
	}
	
	public static ArrayList<String> analySourceTarget(String sql){
		ArrayList<String> list = new ArrayList<String>();
		
		String sqlType = testSQLType(sql);
		
		//System.out.println("SQL Type is : "+sqlType);

		if(sqlType == SQLTYPE_CREATE_TABLE_AS){
			list = analy_CREATE_TABLE_AS(sql);
		}
		
		if(sqlType == SQLTYPE_SELECT_INTO_OUTFILE){
			list = analy_SELECT_INTO_OUTFILE(sql);
		}
		
		if(sqlType == SQLTYPE_INSERT_INTO){
			list = analy_INSERT_INTO(sql);
		}
		
		if(sqlType == SQLTYPE_LOAD_DATA_INFILE){
			list = analy_LOAD_DATA_INFILE(sql);
		}
		
		if(sqlType == SQLTYPE_UPDATE){
			list = analy_UPDATE(sql);
		}
		
		//System.out.println(list);
		return list;
		
	}
	
	private static ArrayList<String> analy_UPDATE(String sql){
		ArrayList<String> list = new ArrayList<String>();
		
		int idx1 = sql.lastIndexOf(")");
		int idx2 = sql.lastIndexOf("where");
		
		//Delete from "where" if it occurs after ")" OR there is no ")"
		if(idx2 > idx1 ){
			sql = sql.substring(0,idx2);
		}
		
		Pattern p = Pattern.compile("(?i)(.*)SET\\s+(\\w+)\\..*");
		Matcher m = p.matcher(sql);
		if(m.matches()){
			String targetTableAlias = m.group(2);
			String selectSQL = m.group(1).replaceAll("(?i)\\s*UPDATE\\s+","SELECT * FROM "); //replace UPDATE with SELECT * FROM ,so we can use SelectSQL to analyze used tables
			String targetTable = "";
			
			SelectSQL sel = new SelectSQL(selectSQL);
			sel.formatSQL();
			sel.createChilds();
			
			System.out.println(targetTableAlias);
			
			//find real table name if targetTableAlias is just alias
			if(sel.getSimpleSubTree().indexOf(targetTableAlias) == -1){
				//p = Pattern.compile("(?i).*?((\\w+\\.)?\\w+)\\s+(?:AS\\s+)?"+targetTableAlias+".*");
				p = Pattern.compile("(?i).*?(?:\\s|,)([^\\s]*)\\s+(?:AS\\s+)?"+targetTableAlias+"(\\s|,).*");
				m = p.matcher(selectSQL);
				if(m.matches()){
					targetTable = m.group(1);
				}		
			}
			else{
				targetTable = targetTableAlias;
			}
			
			if(targetTable == ""){
				//System.out.println("Error SQl syntax");
			}
			else
			{
				for(String sourceTable : sel.getSimpleSubTree()){
					if(!sourceTable.equals(targetTable)){
						list.add(targetTable+"|"+sourceTable);
					}
				}
			}
			
			
		}
		else{
			//System.out.println("get target table error in sql " + sql);
		}
		
		return list;
	}
	
	private static ArrayList<String> analy_LOAD_DATA_INFILE(String sql){
		ArrayList<String> list = new ArrayList<String>();
		
		Pattern p = Pattern.compile(Parser.PAT_LOAD_DATA_INFILE);
		Matcher m = p.matcher(sql);
		if(m.matches()){
			String target = m.group(2);
			String source = m.group(1);
			list.add(target+"|"+source);
		}
		else{
			System.out.println("Not match");
		}
		return list;
	}
	
	private static ArrayList<String> analy_INSERT_INTO(String sql){
		ArrayList<String> list = new ArrayList<String>();
		
		Pattern p = Pattern.compile(Parser.PAT_INSERT_INTO);
		Matcher m = p.matcher(sql);
		if(m.matches()){
			String target = m.group(1).split("\\s")[0];
			String sourceSQL = m.group(2);
			
			SelectSQL sel = new SelectSQL(sourceSQL);
			sel.formatSQL();
			sel.createChilds();
			
			//System.out.println("Target is : "+target);
			//System.out.println("sourceSQL is : "+sourceSQL);
			//System.out.println("Source is : "+sel.getSimpleSubTree());
			
			for(String sourceTable : sel.getSimpleSubTree()){
				list.add(target + "|" + sourceTable);
			}
		}
		return list;
	}
	
	private static ArrayList<String> analy_SELECT_INTO_OUTFILE(String sql){
		ArrayList<String> list = new ArrayList<String>();
		
		Pattern p = Pattern.compile(PAT_SELECT_INTO_OUTFILE);
		Matcher m = p.matcher(sql);
		if(m.matches()){
			String target = m.group(2);
			String sourceSQL = m.group(1);
			
			SelectSQL sel = new SelectSQL(sourceSQL);
			sel.formatSQL();
			sel.createChilds();
			
			for(String sourceTable : sel.getSimpleSubTree()){
				list.add(target + "|" + sourceTable);
			}
		}
		
		return list;
	}
	
	private static ArrayList<String> analy_CREATE_TABLE_AS(String sql){
		ArrayList<String> list = new ArrayList<String>();
		
		Pattern p = Pattern.compile(PAT_CREATE_TABLE_AS);
		Matcher m = p.matcher(sql);
		if(m.matches()){
			String target = m.group(1);
			String sourceSQL = m.group(2);
			
			SelectSQL sel = new SelectSQL(sourceSQL);
			sel.formatSQL();
			sel.createChilds();
			//System.out.println("formatted sql is : " + sel.getFormattedSQL());
			
			for(String sourceTable : sel.getSimpleSubTree()){
				list.add(target + "|" + sourceTable);
			}
		}
		
		return list;
	}
}
