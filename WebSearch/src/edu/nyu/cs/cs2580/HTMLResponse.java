package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class HTMLResponse {
	
	static String template=null;
	
	public static void readTemplate(String filename){
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			template=br.readLine()+"\n";
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public static String getResponse(Vector<ScoredDocument> documents, String query, int identifier){
		StringBuilder response=new StringBuilder();
		response.append("<body>\n<html>\n");
		for(ScoredDocument doc : documents){
			String url="url?"+"id="+Integer.toHexString(identifier)+"&did="+Integer.toHexString(doc._did)+"&q="+query.replace(" ", "+");
			String line=template.replace("$HREF", url);
			line=line.replace("$CONTENT", query+"\t"+doc.asString());
			response.append(line);
		}
		response.append("</body>\n</html>\n");
		return response.toString();
	}
}
