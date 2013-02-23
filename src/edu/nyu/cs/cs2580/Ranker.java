package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.Scanner;

class Ranker {
	private Index _index;
	
	public Ranker(String index_source){
		_index = new Index(index_source);
	}
	
	public Document getDocument(int did){
		return _index.getDoc(did);
	}
	
	public Vector < ScoredDocument > runquery(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(runquery(query, i));
		}
		return retrieval_results;
	}
	
	private ScoredDocument runquery(String query, int did){
		// Build query vector
		Scanner s = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (s.hasNext()){
			String term = s.next();
			qv.add(term);
		}
		s.close();
		// Get the document vector. For hw1, you don't have to worry about the
		// details of how index works.
		Document d = _index.getDoc(did);
		Vector < String > dv = d.get_title_vector();
		// Score the document. Here we have provided a very simple ranking model,
		// where a document is scored 1.0 if it gets hit by at least one query term.
		double score = 0.0;
		for (int i = 0; i < dv.size(); ++i){
			for (int j = 0; j < qv.size(); ++j){
				if (dv.get(i).equals(qv.get(j))){
					score = 1.0;
					break;
				}
			}
		}
		return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	//Vector Space Model
	public Vector < ScoredDocument > runqueryVSM(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(vectorSpaceModel(query, i));
		}
		sortDsc(retrieval_results);
		return retrieval_results;
	}
		
	private ScoredDocument vectorSpaceModel(String query, int did){
		// Build query vector
		Scanner s = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (s.hasNext()){
			String term = s.next();
			qv.add(term);
		}
		s.close();
		HashMap<String,Double> qtf=l2NormVSMQue(qv);
		Document d = _index.getDoc(did);
		HashMap<String,Double> dtf=l2NormVSMDoc(d);
		double score=0;
		double sumdq=0, sumd=0, sumq=0;
		Set<Entry<String,Double>> qset=qtf.entrySet();
		for(Entry<String,Double> entry : qset){
			sumq+=entry.getValue()*entry.getValue();
			sumdq+=dtf.containsKey(entry.getKey())? dtf.get(entry.getKey())*entry.getValue():0;
		}
		Set<Entry<String,Double>> dset=dtf.entrySet();
		for(Entry<String,Double> entry : dset){
			sumd+=entry.getValue()*entry.getValue();
		}
		score=sumdq/Math.sqrt(sumd*sumq);
		score=score==Double.NaN? 0: score;
		return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	private HashMap<String,Double> l2NormVSMQue(Vector<String> qv){
		HashMap<String, Integer> _qf=new HashMap<String, Integer>();
	    for (int i = 0; i < qv.size(); ++i){
	    	String term=qv.get(i);
	    	if(_qf.containsKey(term)){
	    		int old_dtf = _qf.get(term);
	    		_qf.put(term, old_dtf + 1);
	    	}else{
	    		_qf.put(term, 1);
	    	}
	    }
	    double _total=0;
		HashMap<String,Double> _qtf=new HashMap<String,Double>();
		Set<Entry<String,Integer>> set=_qf.entrySet();
		for(Entry<String,Integer> entry : set){
			double a=(Math.log((double)entry.getValue()+1.0))*Math.log((double)_index.numDocs()/(double)Document.documentFrequency(entry.getKey()));
			_total+=a*a;
		}
		_total=Math.sqrt(_total);
		for(Entry<String,Integer> entry : set){
			double a=(Math.log((double)entry.getValue()+1.0))*Math.log((double)_index.numDocs()/(double)Document.documentFrequency(entry.getKey()));
			_qtf.put(entry.getKey(), a/_total);
		}
		return _qtf;
	}
	
	private HashMap<String,Double> l2NormVSMDoc(Document d){
		HashMap<String, Integer> _dtf=new HashMap<String, Integer>();
		Vector<String> _body=d.get_body_vector();
		Vector<String> _title=d.get_title_vector();
	    for (int i = 0; i < _title.size(); ++i){
	    	String term=_title.get(i);
	    	if(_dtf.containsKey(term)){
	    		int old_dtf = _dtf.get(term);
	    		_dtf.put(term, old_dtf + 1);
	    	}else{
	    		_dtf.put(term, 1);
	    	}	    	  
	    }
	    for (int i = 0; i < _body.size(); ++i){
	    	String term=_body.get(i);
	    	if(_dtf.containsKey(term)){
	    		int old_dtf = _dtf.get(term);
	    		_dtf.put(term, old_dtf + 1);
	    	}else{
	    		_dtf.put(term, 1);
	    	}	    	  
	    }
	    double _total=0;
		HashMap<String,Double> _tf=new HashMap<String,Double>();
		Set<Entry<String,Integer>> set=_dtf.entrySet();
		for(Entry<String,Integer> entry : set){
			double a=(Math.log((double)entry.getValue()+1.0))*Math.log((double)_index.numDocs()/(double)Document.documentFrequency(entry.getKey()));
			_total+=a*a;
		}
		_total=Math.sqrt(_total);
		for(Entry<String,Integer> entry : set){
			double a=(Math.log((double)entry.getValue()+1.0))*Math.log((double)_index.numDocs()/(double)Document.documentFrequency(entry.getKey()));
			_tf.put(entry.getKey(), a/_total);
		}
		return _tf;
	}
	
	//Query Likelihood
	public Vector < ScoredDocument > runqueryQL(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(queryLiklihood(query, i, 0.5));
		}
		sortDsc(retrieval_results);
		return retrieval_results;
	}

	private ScoredDocument queryLiklihood(String query, int did, double lamda){
		// Build query vector
		Scanner s = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (s.hasNext()){
			String term = s.next();
			qv.add(term);
		}
		s.close();
		Document d = _index.getDoc(did);
		HashMap<String, Integer> _dtf=new HashMap<String, Integer>();
		Vector<String> _body=d.get_body_vector();
		Vector<String> _title=d.get_title_vector();
		int _total=0;
	    for (int i = 0; i < _title.size(); ++i){
	    	String term=_title.get(i);
	    	if(_dtf.containsKey(term)){
	    		int old_dtf = _dtf.get(term);
	    		_dtf.put(term, old_dtf + 1);
	    	}else{
	    		_dtf.put(term, 1);
	    	}
	    	_total++;
	    }
	    for (int i = 0; i < _body.size(); ++i){
	    	String term=_body.get(i);
	    	if(_dtf.containsKey(term)){
	    		int old_dtf = _dtf.get(term);
	    		_dtf.put(term, old_dtf + 1);
	    	}else{
	    		_dtf.put(term, 1);
	    	}
	    	_total++;
	    }
	    HashSet<String> added=new HashSet<String>();
	    double score=0;
	    for(String term : qv){
	    	if(!added.contains(term)){
	    		double freqdoc=_dtf.containsKey(term)? (double)_dtf.get(term)/(double)_total : 0;
	    		double freqCol=(Document.termFrequency(term)/Document.termFrequency());
	    		freqCol=freqCol==Double.NaN? 0:freqCol;
	    		score+=Math.log((1.0-lamda)*freqdoc+lamda*freqCol);
	    		added.add(term);
	    	}
	    }
	    return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	//phrase
	public Vector < ScoredDocument > runqueryPhrase(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(phrase(query, i));
		}
		sortDsc(retrieval_results);
		return retrieval_results;
	}
		
	private ScoredDocument phrase(String query, int did){
		// Build query vector
		Scanner s = new Scanner(query);
		Vector < String > qv = new Vector < String > ();
		while (s.hasNext()){
			String term = s.next();
			qv.add(term);
		}
		s.close();
		Document d=_index.getDoc(did);
		Vector<String> _body=d.get_body_vector();
		Vector<String> _title=d.get_title_vector();
		HashMap<String,HashMap<String,Integer>> bigramDoc=new HashMap<String,HashMap<String,Integer>>();
		for(int i=0; i<_body.size()-1; ++i){
			String term=_body.get(i);
			String termNext=_body.get(i+1);
			if(bigramDoc.containsKey(term)){
				HashMap<String,Integer> nexts=bigramDoc.get(term);
				int count=1;
				if(nexts.containsKey(termNext)){
					count+=nexts.get(termNext);
				}
				nexts.put(termNext, count);
			}else{
				HashMap<String,Integer> nexts=new HashMap<String,Integer>();
				nexts.put(termNext,1);
				bigramDoc.put(term, nexts);
			}
		}
		for(int i=0; i<_title.size()-1; ++i){
			String term=_title.get(i);
			String termNext=_title.get(i+1);
			if(bigramDoc.containsKey(term)){
				HashMap<String,Integer> nexts=bigramDoc.get(term);
				int count=1;
				if(nexts.containsKey(termNext)){
					count+=nexts.get(termNext);
				}
				nexts.put(termNext, count);
			}else{
				HashMap<String,Integer> nexts=new HashMap<String,Integer>();
				nexts.put(termNext,1);
				bigramDoc.put(term, nexts);
			}
		}
		int match=0;
		for(int i=0; i<qv.size()-1; ++i){
			String term=qv.get(i);
			String termNext=qv.get(i+1);
			if(bigramDoc.containsKey(term)&&bigramDoc.get(term).containsKey(termNext))
				match+=bigramDoc.get(term).get(termNext);
		}
		double score=match;
		return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	//numview
	public Vector < ScoredDocument > runqueryNumView(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(numView(query, i));
		}
		sortDsc(retrieval_results);
		return retrieval_results;
	}
		
	private ScoredDocument numView(String query, int did){
		Document d=_index.getDoc(did);
		double score=d.get_numviews();
		return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	//linear
	public Vector < ScoredDocument > runqueryLinear(String query){
		Vector < ScoredDocument > retrieval_results = new Vector < ScoredDocument > ();
		for (int i = 0; i < _index.numDocs(); ++i){
			retrieval_results.add(linearModel(query, i, 0.25, 0.25, 0.25, 0.25));
		}
		sortDsc(retrieval_results);
		return retrieval_results;
	}
	
	private ScoredDocument linearModel(String query, int did, double betaCos, double betaLM, double betaPhrase, double betaNumView){
		double cos=vectorSpaceModel(query, did)._score;
		double ql=queryLiklihood(query,did,0.5)._score;
		double phrase=phrase(query,did)._score;
		double numview=numView(query,did)._score;
		double score=cos*betaCos+ql*betaLM+phrase*betaPhrase+numview*betaNumView;
		Document d=_index.getDoc(did);
		return new ScoredDocument(did, d.get_title_string(), score);
	}
	
	private void sortDsc(Vector<ScoredDocument> documents){
		ComparatorDoc comparator=new ComparatorDoc();
		try{
		Collections.sort(documents,comparator);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private class ComparatorDoc implements Comparator<ScoredDocument>{
		public int compare(ScoredDocument doc1, ScoredDocument doc2) {
			if(doc1._score>doc2._score) return -1;
			else if(doc1._score<doc2._score) return 1;
			return 0;
		}
	}
}
