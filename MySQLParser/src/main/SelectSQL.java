package main;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class SelectSQL {
	
	/*
	Sub query will be replaced with MaskTable_[0]_[1], MaskTable_ is defined by MASK_TABLE_PREFIX, 
	and [0] is defined by prefix which equals the level of sub query started from 0,
	and [1] is defined by table position in the formattedSQL started from 0 
	
	for example : 
	
	RawSQL is "SELECT * FROM (SELECT * FROM Table_A) a INNER JOIN Table_B b ON a.ID = b.ID"
	formattedSQL will be : SELECT * FROM MaskTable_0_0 INNER JOIN Table_B b ON a.ID = b.ID
	
	 
	 */
	
	public static String MASK_TABLE_PREFIX = "MaskTable_";		//table prefix to imply SubQuery
	private String prefix = "0";	
	private String rawSQL = "";
	private String formattedSQL = "";

	//ArrayList<String> of index pair of start and end of each sub query, for example : 5,30 |45,79
	private ArrayList<String> subQueryPos = new ArrayList<String>();	

	private int indexOfTables = -1; 				//index of where first table occurs 
	private boolean isValid = false;
	private boolean hasSubQuery = false;	
	private int level = 0;
	private boolean hasChild = false;
	private ArrayList<String> refTableList = new ArrayList<String>();
	
	private SelectSQL parent = null;
	private SelectSQL[] childs = null;
	
	public boolean isValid(){
		return isValid;
	}
	public boolean hasChild(){
		return hasChild;
	}
	public boolean hasSubQuery(){
		return hasSubQuery;
	}
	public String getRawSQL(){
		return rawSQL;
	}
	public String getFormattedSQL(){
		return formattedSQL;
	}
	public int level(){
		return level;
	}
	public void setLevel(int level){
		this.level = level;
	}
	public void setPrefix(String pref){
		this.prefix = pref;
	}
	public void setParent(SelectSQL parent) {
		this.parent = parent;
	}	
	public SelectSQL getParent() {
		return parent;
	}
	
	//format rawSQL,use MASK_TABLE_PREFIX  and subQueryPos
	public void formatSQL(){
		formattedSQL = rawSQL;
		if(hasSubQuery && subQueryPos.size() > 0){
			for(int i=subQueryPos.size()-1;i>=0;i--){
				String[] pos = subQueryPos.get(i).split(",");
				formattedSQL = formattedSQL.substring(0,Integer.valueOf(pos[0]))+MASK_TABLE_PREFIX+prefix+"_"+String.valueOf(i)+formattedSQL.substring(Integer.valueOf(pos[1]));
			}
		}
		analyRefTables();
	}

	//use sub query to create all the childs recursively
	public void createChilds(){
		if(this.hasSubQuery && this.subQueryPos.size() > 0){
			this.hasChild = true;
			this.childs = new SelectSQL[this.subQueryPos.size()];
			for(int i=0;i<this.subQueryPos.size();i++){
				String[] pos = this.subQueryPos.get(i).split(",");
				childs[i] = this.newChild(this.rawSQL.substring(Integer.valueOf(pos[0])+1,Integer.valueOf(pos[1])-1).trim(),i);
			}
		}
	}
	
	private SelectSQL newChild(String sql,int i){
		SelectSQL child = null;
		child = new SelectSQL(sql);
		child.setLevel(this.level+1);
		child.setPrefix(this.prefix+"_"+String.valueOf(i));
		child.formatSQL();
		child.setParent(this);
		child.createChilds();
		//child.printMe();
		return child;
	}
	
	public void printMe(){
		System.out.println("Level is : "+this.level);
		System.out.println("Prefix is : "+this.prefix);
		System.out.println("RawSQL is : " + rawSQL);
		System.out.println("formattedSQL is :" + formattedSQL);
		System.out.println("RefTableList is : " + this.refTableList.toString());
		System.out.println("==================");
	}
	
	
	private void analyRefTables(){
		if(this.isValid && this.indexOfTables > -1){
			/*
			 * substring from last where has been deleted in clearRawSQL()
			//substring from "FROM" AND "WHERE"
			int indexOfWhere = this.formattedSQL.toUpperCase().indexOf(" WHERE ");
			String shortSQL = this.formattedSQL.substring(this.indexOfTables,indexOfWhere == -1 ? this.formattedSQL.length() : indexOfWhere);
			*/
			//System.out.println("ShortSQL here is : "+shortSQL);
			
			String shortSQL = this.formattedSQL.substring(this.indexOfTables);
			
			//check if there is JOIN there
			//warning: "JOIN" should be checked before ","  
			if(shortSQL.toUpperCase().indexOf(" JOIN ") > -1){
				String PAT_JOIN_ON = "\\s+ON\\s+(\\(?\\w+\\.)?\\w+\\s*=\\s*(\\w+\\.)?\\w+\\)?\\s*";
				shortSQL = shortSQL.replaceAll("(?i)"+PAT_JOIN_ON, "\\s");
				String[] res = shortSQL.split("(?i)JOIN");
				for (String table : res){
					this.refTableList.add(table.trim().split("\\s")[0]);	//choose string before first space
				}
			}
			else{
				if(shortSQL.indexOf(",")>-1){
					String[] res = shortSQL.split(",");
					for (String table : res){
						this.refTableList.add(table.trim().split("\\s")[0]);//choose string before first space
					}
				}
				else{
					refTableList.add(shortSQL.trim().split("\\s")[0]);
				}
			}
		}
		//this.printMe();
	}

	public void printTree(){
		System.out.println(this.getSubTree().toString());
		System.out.println("---------------------");
		System.out.println(this.getSimpleSubTree().toString());
	}
	
	public ArrayList<String> getSubTree(){
		ArrayList<String> refTree = new ArrayList<String>();
		//add tables in refTableList first
		for(String refTable : this.refTableList){
			refTree.add(MASK_TABLE_PREFIX+this.prefix+"|"+refTable);
		}
		//System.out.println(refTree.toString());
		if(this.childs != null){
			for(SelectSQL sql : this.childs){
				refTree.addAll(sql.getSubTree());
			}
		}
		return refTree;
	}
	
	public ArrayList<String> getSimpleSubTree(){
		ArrayList<String> refTreeSimple = new ArrayList<String>();
		
		//add tables in refTableList first, but ignore tables of masks such as "MaskTable_0_1"
		for(String refTable : this.refTableList){
			if(refTable.indexOf(MASK_TABLE_PREFIX) == -1){
				refTreeSimple.add(refTable);
			}
		}
		//System.out.println(refTree.toString());
		if(this.childs != null){
			for(SelectSQL sql : this.childs){
				refTreeSimple.addAll(sql.getSimpleSubTree());
			}
		}		
		return new ArrayList<String>(new LinkedHashSet<String>(refTreeSimple));
	}
	
	//
	//Analy sub query , insert index pair of start and end of each sub query into ArrayList<String> subQueryPos, for example : 5,30 |45,79
	private void analySubQuery(){
		String sql;
		int i=0;
		int begin = 0,end = 0;
		int num = 0;
		sql = this.rawSQL.toUpperCase();
		if(sql.indexOf(" FROM ")>-1){
			this.isValid = true;
			this.indexOfTables = sql.indexOf(" FROM ")+6; 
			i = sql.indexOf(" FROM ")+6;
			while(i<sql.length()){
				if(sql.charAt(i) == '('){
					this.hasSubQuery = true;
					
					//begin/end/i starts from the index of "(", i and end increase
					//num means the count of "(" ,the sub query ends if num reduced to 0
					begin = i;
					end = i;
					num ++ ;
					while(num > 0){
						end = sql.indexOf(")",end+1);	
						if(end == -1){								//i goes to the end and break if there is no ")" left
							i = sql.length();
							isValid = false;
							break;
						}
						i = sql.indexOf("(",i+1);
						if( (i == -1 && end > 0) || ( end < i )){
							//num minus 1, if there is no "(" after current "(" OR index of ")" smaller than index of next "("
							num --;
						}
					}
					if( begin < end ){
						subQueryPos.add(String.valueOf(begin)+","+String.valueOf(end+1));
					}

					if(i == -1){		//break if there is no ")" left
						break;
					}
				}
				else{
					if(sql.charAt(i) == ' '){						
						i++;
					}
					else{
						//goto next "," OR "JOIN" to accelerate
						if(sql.indexOf(",", i) > -1 || sql.indexOf(" JOIN ", i) > -1)  {
							i = (sql.indexOf(",", i)) > -1 ? sql.indexOf(",", i) + 1 : sql.indexOf(" JOIN ", i) + 4 ;
						}
						else{
							break;
						}
					}
				}
			}
		}
	}
	
	public SelectSQL(String sql){
		this.rawSQL = clearRawSQL(sql);
		this.analySubQuery();
	}
	
	/*
	 * clear raw SQL:
	 * 1. replace comments
	 * 2. replace multiple space to one space
	 * 3. treat union as two sub queries 
	 * 4. delete from last "WHERE","GROUP BY" and "ORDER BY"
	 */
	private String clearRawSQL(String sql){
		//delete comments
		//replace multiple spaces with one space
		//replace "from(" with "from (" to separate with from* functions
		String newSQL = sql.replaceAll("(\\/\\*.*\\*\\/)?", "").replaceAll("\\s+", " ").replaceAll("(?i)from\\(", "from ("); 
		
		//System.out.println("newSQL here is : "+newSQL);
		
		// ,replace "union" with ") a,("
		// ,add "SELECT for_union FROM ( SELECT" at the beginning
		// ,add ") b" at the end
		

		if(newSQL.toUpperCase().indexOf(" UNION ") > 0 ){
			String firstQuery = newSQL.substring(0,newSQL.toUpperCase().indexOf(" UNION "));
			if( (firstQuery.length()-firstQuery.replaceAll("\\(|\\)", "").length()) % 2 == 0 ){
				newSQL = newSQL.replaceFirst("(?i)SELECT", "SELECT for_union FROM ( SELECT").replaceAll("(?i)\\s+UNION\\s+(ALL)?", ") a,(")+") b";
			}
		}
		
		int indexOfDelete = newSQL.toUpperCase().indexOf("WHERE");
		while(indexOfDelete > -1)
		{
			if(Parser.checkDoubleBracket(newSQL.substring(indexOfDelete))){
				newSQL = newSQL.substring(0,indexOfDelete);
				break;
			}
			else{
				indexOfDelete = newSQL.toUpperCase().indexOf("WHERE",indexOfDelete+5);
			}
		}
		
		indexOfDelete = newSQL.toUpperCase().indexOf("GROUP BY");
		while(indexOfDelete > -1)
		{
			if(Parser.checkDoubleBracket(newSQL.substring(indexOfDelete))){
				newSQL = newSQL.substring(0,indexOfDelete);
				break;
			}
			else{
				indexOfDelete = newSQL.toUpperCase().indexOf("GROUP BY",indexOfDelete+8);
			}
		}
		
		indexOfDelete = newSQL.toUpperCase().indexOf("ORDER BY");
		while(indexOfDelete > -1)
		{
			if(Parser.checkDoubleBracket(newSQL.substring(indexOfDelete))){
				newSQL = newSQL.substring(0,indexOfDelete);
				break;
			}
			else{
				indexOfDelete = newSQL.toUpperCase().indexOf("ORDER BY",indexOfDelete+8);
			}
		}
		
		indexOfDelete = newSQL.toUpperCase().indexOf("ON DUPLICATE KEY UPDATE");
		while(indexOfDelete > -1)
		{
			if(Parser.checkDoubleBracket(newSQL.substring(indexOfDelete))){
				newSQL = newSQL.substring(0,indexOfDelete);
				break;
			}
			else{
				indexOfDelete = newSQL.toUpperCase().indexOf("ON DUPLICATE KEY UPDATE",indexOfDelete+8);
			}
		}
		
		return newSQL;
	}
		
}
