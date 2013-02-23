package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;

class QueryHandler implements HttpHandler {
	private static String plainResponse = "Request received, but I am not smart enough to echo yet!\n";
	private Ranker _ranker;
	private HashMap<InetSocketAddress,Integer> session=new HashMap<InetSocketAddress,Integer>();
	
	public QueryHandler(Ranker ranker) {
		_ranker = ranker;
	}

	public static Map<String, String> getQueryMap(String query) {
		String[] params = query.split("&");
		Map<String, String> map = new HashMap<String, String>();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, value);
		}
		return map;
	}

	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
		if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
			return;
		}
		// Print the user request header.
		Headers requestHeaders = exchange.getRequestHeaders();
		System.out.print("Incoming request: ");
		for (String key : requestHeaders.keySet()) {
			System.out.print(key + ":" + requestHeaders.get(key) + "; ");
		}
		System.out.println();
		String queryResponse = "";
		String uriQuery = exchange.getRequestURI().getQuery();
		String uriPath = exchange.getRequestURI().getPath();
		InetSocketAddress remote=exchange.getRemoteAddress();
		String responseType="text/plain";

		if ((uriPath != null) && (uriQuery != null)) {
			if (uriPath.equals("/search")) {
				Map<String, String> query_map = getQueryMap(uriQuery);
				Set<String> keys = query_map.keySet();
				if (keys.contains("query")) {
					//randomly create a session ID for each IP:Port
					int identifier=-1;
					if(session.containsKey(remote)){
						identifier=session.get(remote);
					}else{
						identifier=(int) Math.abs(Math.random()*30000);
						session.put(remote, identifier);
					}					
					Vector<ScoredDocument> sds = null;
					query_map.put("query", query_map.get("query").replace("+", " "));
					if (keys.contains("ranker")) {
						String ranker_type = query_map.get("ranker");
						if (ranker_type.equals("cosine")) {
							System.out.println("cosine");
							sds = _ranker.runqueryVSM(query_map.get("query"));
						} else if (ranker_type.equals("QL")) {
							System.out.println("QL");
							sds = _ranker.runqueryQL(query_map.get("query"));
						} else if (ranker_type.equals("phrase")) {
							System.out.println("phrase");
							sds = _ranker.runqueryPhrase(query_map.get("query"));
						} else if (ranker_type.equals("linear")) {
							System.out.println("linear");
							sds = _ranker.runqueryLinear(query_map.get("query"));
						} else {
							System.out.println("numview");
							sds = _ranker.runqueryNumView(query_map.get("query"));
						}
					} else {
						sds = _ranker.runquery(query_map.get("query"));
					}
					//if the request format is an HTML
					if(query_map.containsKey("format")&&query_map.get("format").equals("html")){
						HTMLResponse.readTemplate("template.txt");
						String response=HTMLResponse.getResponse(sds,query_map.get("query"),identifier);
						queryResponse+=response;
						responseType="text/html";
					}else{  //otherwise return plain text
						Iterator<ScoredDocument> itr = sds.iterator();
						while (itr.hasNext()) {
							ScoredDocument sd = itr.next();
							if (queryResponse.length() > 0) {
								queryResponse = queryResponse + "\n";
							}
							queryResponse = queryResponse + query_map.get("query") + "\t" + sd.asString();
						}
						if (queryResponse.length() > 0) {
							queryResponse = queryResponse + "\n";
						}
					}
					//write to file
					writeToFile("01.txt",queryResponse);
					//write to log
					writeToLog("log.txt",identifier,query_map.get("query"),sds);
				}
			}else if(uriPath.equals("/url")){   //Query logging and return document content
				Map<String, String> query_map = getQueryMap(uriQuery);
				int id=-1, did=-1;
				String query=null;
				if(query_map.containsKey("id")&&query_map.containsKey("did")&&query_map.containsKey("q")){
					id=Integer.parseInt(query_map.get("id"), 16);
					did=Integer.parseInt(query_map.get("did"), 16);
					query=query_map.get("q").replace("+", " ");
					String line=id+"\t"+query+"\t"+did+"\t"+"click"+"\t"+System.currentTimeMillis();
					changeLog(line, "log.txt");
					queryResponse=line+"\n\n";
					Document d=_ranker.getDocument(did);
					queryResponse+="Title\n"+d.get_title_string()+"\n\n";
					String body="";
					for(String t : d.get_body_vector()){
						body+=t+" ";
					}
					queryResponse+="Body\n"+body+"\n";
				}				
			}
		}
		
		// Construct a simple response.
		Headers responseHeaders = exchange.getResponseHeaders();
		responseHeaders.set("Content-Type", responseType);
		exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
		OutputStream responseBody = exchange.getResponseBody();
		responseBody.write(queryResponse.getBytes());
		responseBody.close();
	}
	
	public void changeLog(String log, String filename){
		String[] elements=log.split("\t");
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename+"_tmp",false));
			String line=null;
			while((line=br.readLine())!=null){
				String[] record=line.split("\t");
				if(record[0].equals(elements[0])&&record[1].equals(elements[1])&&record[2].equals(elements[2])){
					break;
				}else{
					bw.append(line);
					bw.append("\r\n");
				}
			}
			bw.append(log);
			bw.append("\r\n");
			while((line=br.readLine())!=null){
				bw.append(line);
				bw.append("\r\n");
			}
			br.close();
			bw.flush();
			bw.close();
			File file=new File(filename);
			file.delete();
			file=new File(filename+"_tmp");
			file.renameTo(new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeToLog(String filename, int id, String query, Vector<ScoredDocument> documents){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename,true));
			for(ScoredDocument doc : documents){
				String line=id+"\t"+query+"\t"+doc._did+"\t"+"render"+"\t"+System.currentTimeMillis()+"\r\n";
				bw.append(line);
			}			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeToFile(String filename, String response){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename,true));
			bw.append(response);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
