package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {
	
	public class Eval{
		String query;
		double score;
		public Eval(String q, double s){
			query=q;
			score=s;
		}
	}

	public static void main(String[] args) throws IOException {
		HashMap<String, HashMap<Integer, Double>> relevance_judgments = new HashMap<String, HashMap<Integer, Double>>();
		if (args.length < 1) {
			System.out.println("need to provide relevance_judgments");
			return;
		}
		String p = args[0];
		// first read the relevance judgments into the HashMap
		readRelevanceJudgments(p, relevance_judgments);
		// now evaluate the results from stdin
		//evaluateStdin(relevance_judgments);
		evaluateStdinPrecision(relevance_judgments,5);
	}
	
	public static void output(Eval eval){
		System.out.println(eval.query+"     "+eval.score);
	}

	public static void readRelevanceJudgments(String p, HashMap<String, HashMap<Integer, Double>> relevance_judgments) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(p));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					// parse the query,did,relevance line
					Scanner s = new Scanner(line).useDelimiter("\t");
					String query = s.next();
					int did = Integer.parseInt(s.next());
					String grade = s.next();
					double rel = 0.0;
					// convert to binary relevance
					if ((grade.equals("Perfect")) || (grade.equals("Excellent")) || (grade.equals("Good"))) {
						rel = 1.0;
					}
					if (relevance_judgments.containsKey(query) == false) {
						HashMap<Integer, Double> qr = new HashMap<Integer, Double>();
						relevance_judgments.put(query, qr);
					}
					HashMap<Integer, Double> qr = relevance_judgments.get(query);
					qr.put(did, rel);
				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
	}

	public static void evaluateStdin(HashMap<String, HashMap<Integer, Double>> relevance_judgments) {
		// only consider one query per call
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			double RR = 0.0;
			double N = 0.0;
			while ((line = reader.readLine()) != null) {
				Scanner s = new Scanner(line).useDelimiter("\t");
				String query = s.next();
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				if (relevance_judgments.containsKey(query) == false) {
					throw new IOException("query not found");
				}
				HashMap<Integer, Double> qr = relevance_judgments.get(query);
				if (qr.containsKey(did) != false) {
					RR += qr.get(did);
				}
				++N;
			}
			System.out.println(Double.toString(RR / N));
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
		}
	}
	
	public static double evaluateStdinPrecision(HashMap<String, HashMap<Integer, Double>> relevance_judgments,int K) {
		// only consider one query per call
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			String query=null;
			int i=0;
			int RR=0;
			while ((line = reader.readLine()) != null&&i<K) {
				Scanner s = new Scanner(line).useDelimiter("\t");
				query = s.next();
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				if (relevance_judgments.containsKey(query) == false) {
					throw new IOException("query not found: "+query);
				}
				HashMap<Integer, Double> qr = relevance_judgments.get(query);
				if (qr.containsKey(did) != false) {
					if(qr.get(did)==1) ++RR;
				}
				++i;
			}
			double score=(double)RR/(double)K;
			System.out.println(query+"    "+score);
			return score;
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
			return 0;
		}
	}
	
	public static double evaluateStdinRecall(HashMap<String, HashMap<Integer, Double>> relevance_judgments,int K) {
		// only consider one query per call
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			String query=null;
			int i=0;
			int RR=0;
			while ((line = reader.readLine()) != null&&i<K) {
				Scanner s = new Scanner(line).useDelimiter("\t");
				query = s.next();
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				if (relevance_judgments.containsKey(query) == false) {
					throw new IOException("query not found: "+query);
				}
				HashMap<Integer, Double> qr = relevance_judgments.get(query);
				if (qr.containsKey(did) != false) {
					if(qr.get(did)==1) ++RR;
				}
				++i;
			}
			HashMap<Integer, Double> qr = relevance_judgments.get(query);
			Set<Entry<Integer,Double>> set=qr.entrySet();
			int R=0;
			for(Entry<Integer,Double> entry : set){
				if (entry.getValue()==1) ++R;
			}
			double score=(double)RR/(double)R;
			System.out.println(query+"    "+score);
			return score;
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
			return 0;
		}
	}
	
	public static double evaluateStdinFM(HashMap<String, HashMap<Integer, Double>> relevance_judgments,int K,double alpha) {
		// only consider one query per call
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			String query=null;
			int i=0;
			int RR=0;
			while ((line = reader.readLine()) != null&&i<K) {
				Scanner s = new Scanner(line).useDelimiter("\t");
				query = s.next();
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				if (relevance_judgments.containsKey(query) == false) {
					throw new IOException("query not found: "+query);
				}
				HashMap<Integer, Double> qr = relevance_judgments.get(query);
				if (qr.containsKey(did) != false) {
					if(qr.get(did)==1) ++RR;
				}
				++i;
			}
			HashMap<Integer, Double> qr = relevance_judgments.get(query);
			Set<Entry<Integer,Double>> set=qr.entrySet();
			int R=0;
			for(Entry<Integer,Double> entry : set){
				if (entry.getValue()==1) ++R;
			}
			double Pscore=(double)RR/(double)K;
			double Rscore=(double)RR/(double)R;
			double score=1/((alpha/Pscore+(1-alpha)/Rscore));
			System.out.println(query+"    "+score);
			return score;
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
			return 0;
		}
	}
}
