package edu.cmu.lti.f13.hw4.hw4_cx.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_cx.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_cx.typesystems.Token;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 *what I do:
	 *	tokenize the sentence using StanforCoreNLP tokenizer.
	 *	for each sentence, add the tokenizes to a HashMap, as the vector space model for it.
	 * @param jcas: the jcas in annotation system
	 * @param doc: the type system Document to process
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		Properties props = new Properties();
		props.put("annotators", "tokenize");
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		
		edu.stanford.nlp.pipeline.Annotation sentence = new edu.stanford.nlp.pipeline.Annotation(docText);
		pipeline.annotate(sentence);
		
		HashMap<String, Integer> hToken = new HashMap<String, Integer>(); //called stanford nlp to tokenize the document
		
		for (CoreLabel token : sentence.get(TokensAnnotation.class))
		{
			String word = token.word().toLowerCase();
			
			if (!hToken.containsKey(word))
			{
				hToken.put(word,1);				
			}
			hToken.put(word, hToken.get(word) + 1);
		}
		//hToken now contains all token counts
		
		List<Token> TokenInDoc = new ArrayList<Token>();
		for (Entry<String, Integer> item : hToken.entrySet())
		{
			Token token = new Token(jcas);
			token.setFrequency(item.getValue());
			token.setText(item.getKey());
			token.addToIndexes(); //add tokens to jcas
			TokenInDoc.add(token);				
		}
		doc.setTokenList(FSCollectionFactory.createFSList(jcas,TokenInDoc));
	}

}
