package com.huisou.constant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContextConstant {
	
	public static final String SORT_DESC = "desc";
	public static final String SORT_ASC = "asc";
	
	
	public static Set<String> getFields(){
		Set<String> Fields = new HashSet<String>();
		Fields.add("id");
		Fields.add("wid");
		Fields.add("category_id");
		Fields.add("price");
		Fields.add("sold_num");
		Fields.add("uv_num");
		Fields.add("pv_num");
		Fields.add("title");
		Fields.add("status");
		Fields.add("img");
		Fields.add("created_at");
		Fields.add("updated_at");
		Fields.add("deleted_at");
		return Fields;
	}
	
}
