package edu.cmu.lti.f13.hw4.hw4_cx.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_cx.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_cx.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_cx.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;
	/** query and text relevant values **/
	public ArrayList<Integer> relList;
	public ArrayList<Double> scoreList;
	public ArrayList<Map<String,Integer>> hVectorList;
	public ArrayList<String> vRawString;
	public ArrayList<Integer> vRank;
	public Map<Double,Integer> hScoreRank;
		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();
		relList = new ArrayList<Integer>();
		scoreList = new ArrayList<Double>();
		hVectorList = new ArrayList<Map<String,Integer>>();
		vRawString = new ArrayList<String>();
		vRank = new ArrayList<Integer> ();	
		hScoreRank = new HashMap<Double,Integer>();
	}

	/**
	 * TODO :: 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS

			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
			List<Token> TokenInDoc = Utils.fromFSListToCollection(doc.getTokenList(),Token.class);
			Map<String,Integer> hVector = MakeMapFromList(TokenInDoc);
			hVectorList.add(hVector);
			String RawText = doc.getText();
			vRawString.add(RawText);	
		}

	}
	
	private Map<String,Integer> MakeMapFromList(List<Token> vToken)
	{
		Map<String,Integer> hVector = new HashMap<String,Integer>();
		for (Token token : vToken)
		{
			hVector.put(token.getText(), token.getFrequency());
		}
		return hVector;
	}
	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);

		//compute the cosine similarity measure
		AssignScore();
		//compute the rank of retrieved sentences
		CalcRank();
		//compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr();
		
		//show result
		ShowResult();
		
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}
	
	/**
	 * output result to stdout
	 */
	private void ShowResult()
	{
		for (int i = 0; i < qIdList.size(); i ++)
		{
			System.out.format("Score: %f\trank=%d\trel=%d\tqid=%d\t%s\n", scoreList.get(i),vRank.get(i),relList.get(i),qIdList.get(i),vRawString.get(i));
		}
	}
	

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;

		// TODO :: compute cosine similarity between two sentences
		double NormQ = NormOfVector(queryVector);
		double NormD = NormOfVector(docVector);
		if (NormQ == 0 || NormD == 0)
		{
			return 0;
		}
		cosine_similarity = InnerProduct(queryVector,docVector) / (NormQ * NormD);
		return cosine_similarity;
	}
	
	private double NormOfVector(Map<String,Integer> hVector)
	{
		double Norm = 0;
		for (Entry<String,Integer> item : hVector.entrySet())
		{
			Integer value = item.getValue();
			Norm += value * value;
		}
		Norm = Math.sqrt(Norm);
		return Norm;		
	}
	private double InnerProduct(Map<String,Integer> hA, Map<String,Integer> hB)
	{
		double res = 0;
		for (Entry<String,Integer> item : hA.entrySet())
		{
			String key = item.getKey();
			Integer value = item.getValue();
			if (hB.containsKey(key))
			{
				res += hB.get(key) * value;				
			}					
		}		
		return res;
	}
	
	
	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;

		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		//only need the relList and vRank
		int cnt = 0;
		for (int i = 0; i < relList.size(); i ++)
		{
			if (relList.get(i) == 1)
			{
				metric_mrr += 1.0 / vRank.get(i);
				cnt += 1;
			}			
		}
		metric_mrr /= cnt;
		return metric_mrr;
	}
	/**
	 * calc the cosine score of each document.
	 * document is in  hVectorList, if relList.get(i) is 99, then it is the current query.
	 * assume the query appears first
	 * @return
	 */	
	private Boolean AssignScore()
	{
		Map<String,Integer> vCurrentQ = new HashMap<String,Integer>();
		for (int i = 0; i < relList.size(); i ++)
		{
			if (relList.get(i) == 99)
			{
				vCurrentQ = hVectorList.get(i);
				scoreList.add(0.0);
				continue;
			}
			Double score = computeCosineSimilarity(vCurrentQ,hVectorList.get(i));
			scoreList.add(score);
		}		
		return true;
	}
	/**
	 * calc the rank of each doc, based on their score, and qIdList.
	 * how to:
	 * 	make a qid-score to rank hashmap
	 * 		for each qid, get all its score, sort them, and then add to qid
	 * @return
	 */
	private Boolean CalcRank()
	{
		ArrayList<Double> vCurrentScore = new ArrayList<Double>();
		for (int i = 0; i < scoreList.size(); i ++)
		{
			if (i != 0)
			{
				if (qIdList.get(i) != qIdList.get(i - 1))
				{
					MakeQidScoreHash(vCurrentScore, qIdList.get(i - 1));
					vCurrentScore.clear();
				}
			}
			vCurrentScore.add(scoreList.get(i));
		}
		MakeQidScoreHash(vCurrentScore,qIdList.get(qIdList.size() - 1));
		vCurrentScore.clear();
		//now the qid.score -> rank mapping is done
		vRank.clear();
		for (int i = 0; i < scoreList.size(); i ++)
		{
			Double key = qIdList.get(i) * 10 + scoreList.get(i);
			Integer rank = hScoreRank.get(key);
			vRank.add(rank);
		}
		return true;
	}
	
	/**
	 * 
	 * @param vScore the score for this qid
	 * @param qid the id of query
	 * @return a map from Qid.Score to rank
	 */
	private void MakeQidScoreHash(ArrayList<Double> vScore, Integer qid)
	{
		Collections.sort(vScore,Collections.reverseOrder());
		for (int i = 0; i < vScore.size(); i ++)
		{
			Double key = qid * 10 + vScore.get(i);
			if (!hScoreRank.containsKey(key))
			{
				hScoreRank.put(key, i + 1);
			}			
		}	
	}

}
