package nlp.assignments.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.ling.Tree;
import nlp.util.CounterMap;


class BaselineCkyParser implements Parser {


	CounterMap<List<String>, Tree<String>> knownParses;
	CounterMap<Integer, String> spanToCategories;

	TreeAnnotator annotator;

	Lexicon lexicon;
	Grammar grammar;

	static public class Chart {
		static enum type{PRETERMINAL, UNARY, BINARY};
		static class EdgeInfo {
		
			type type;
			double score; 
			Rule rule = null;
			int mid = -1;
			
			EdgeInfo(double score) {
				this.score = score;
				this.type = type.PRETERMINAL;
			}
			
			EdgeInfo(double score, Rule rule){
				this(score);
				this.rule = rule;
				this.type = type.UNARY;
			}
			
			EdgeInfo(double score, Rule rule, int mid){
				this(score, rule);
				this.mid = mid;
				this.type = type.BINARY;
			}
			
			@Override
			public String toString() {
				if (rule == null) {
					return Double.toString(score);
				} else {
					return score + ": " + "[ rule = " + rule + ", mid = " + mid + "]";
				}
			}
	}

		
		Map<Integer,Map<Integer,Map<String, EdgeInfo>>> chart = 
				new HashMap<Integer, Map<Integer,Map<String,EdgeInfo>>>();
		Chart(int seqLength) {
			for (int i = 0; i < seqLength; i++) {
				chart.put(i, new HashMap<Integer, Map<String,EdgeInfo>>());
				for (int j = i + 1; j <= seqLength; j++) {
					chart.get(i).put(j, new HashMap<String, EdgeInfo>());
				}
			}
		}

		void set(int i, int j, String label, double score, Rule rule, int midPoint){
			chart.get(i).get(j).put(label, new EdgeInfo(score, rule, midPoint));
		}
		
		void set(int i, int j, String label, double score){
			chart.get(i).get(j).put(label, new EdgeInfo(score));
		}
		
		void set(int i, int j, String label, double score, Rule rule){
			chart.get(i).get(j).put(label, new EdgeInfo(score, rule));		
		}
		
		double get(int i, int j, String label) {
			Map<String,EdgeInfo> edgeScores = chart.get(i).get(j);
			if (!edgeScores.containsKey(label)) {
				return Double.NEGATIVE_INFINITY;
			} else {
				return edgeScores.get(label).score;
			}
		}
		
		EdgeInfo getInfo(int i, int j, String label){
			return chart.get(i).get(j).get(label);
		}
		
		Set<String> getAllCandidateLabels(int i, int j) {
			return chart.get(i).get(j).keySet();
		}

		String getBestLabel(int i, int j) {
			Map<String,EdgeInfo> edgeScores = chart.get(i).get(j);
			double bestScore = Double.NEGATIVE_INFINITY;
			String optLabel = null;
			for (String label : edgeScores.keySet()) {
				if (bestScore < edgeScores.get(label).score) {
					optLabel = label;
					bestScore = edgeScores.get(label).score;
				}
			}
			return optLabel;
		}

		@Override
		public String toString() {
			return chart.toString();
		}

	}

	private Tree<String> traverseBackPointersHelper(
			List<String> sentence, Chart chart, int i, int j, String parent) {
		Chart.EdgeInfo edge = chart.getInfo(i, j, parent);
		switch (edge.type) {
			case PRETERMINAL: {
				List<Tree<String>> child = Collections.singletonList(new Tree<String>(sentence.get(i)));
				return new Tree<String>(parent, child);
			}
			case UNARY: {
				String childLabel = edge.rule.getChildren()[0];
				List<Tree<String>> children;
				if (childLabel .equals(parent)){
					System.err.print("HELP, my child is named"+childLabel);
					children = new ArrayList<Tree<String>>();
				}
				else{
					children = Collections.singletonList(traverseBackPointersHelper(sentence, chart, i, j, childLabel));
				}
				return new Tree<String>(parent, children);
			}
			case BINARY: {
				String[] childLabels = edge.rule.getChildren();
				int m = edge.mid;
				List<Tree<String>> children = new ArrayList<Tree<String>>();
				children.add(traverseBackPointersHelper(sentence, chart, i, m, childLabels[0]));
				children.add(traverseBackPointersHelper(sentence, chart, m, j, childLabels[1]));
				return new Tree<String>(parent, children);
			}
			default: {
				// If the type is incorrect, return an empty node
				return new Tree<String>(parent);
			}
		}
	}

	Tree<String> traverseBackPointers(List<String> sentence, Chart chart) {

		return traverseBackPointersHelper(sentence, chart, 0, sentence.size(), "ROOT");

	}



	public Tree<String> getBestParse(List<String> sentence) {

		/*
		 * TODO
		 * This method needs to be extended to support unary rules 
		 * The UnaryClosure class should simplify this task substantially 
		 */

		// note that chart.get(i, j, c) translates to chart[i][j][c] we used in the slides	

		Chart chart = new Chart(sentence.size());

		// preterminal rules
		for (int k = 0; k < sentence.size(); k++) {
			for (String preterm : lexicon.getAllTags()) {
				chart.set(k, k + 1, preterm, Math.log(lexicon.scoreTagging(sentence.get(k), preterm)));
			}
		}


		for(int max = 1; max<=sentence.size(); max++){
			for(int min = max-1; min>=0; min--){
				if(max-min>1){
					// Try binary rules:
					for (String parent: grammar.states){
						double bestScore = Double.NEGATIVE_INFINITY;
						int optMid =-1;
						Rule optRule = null;

						// parent -> c1 c2
						for (Rule rule : grammar.getBinaryRulesByParent(parent)) {
							for (int mid = min + 1; mid <  max; mid++) {
								double currScore = Math.log(rule.getScore());
								currScore += chart.get(min, mid, rule.getChildren()[0]);
								currScore += chart.get(mid, max, rule.getChildren()[1]);
								if (currScore > bestScore) {
									bestScore = currScore;
									optMid = mid;
									optRule = rule;
								}
							}
						}
						if (bestScore != Double.NEGATIVE_INFINITY) { 
							chart.set(min, max, parent, bestScore, optRule, optMid);
						}
					}
				}
				// Try unary rules:
				for (String parent: grammar.states){
					
					double bestScore = Double.NEGATIVE_INFINITY;
					Rule optRule = null;
					// parent -> c1
					for (Rule rule: grammar.getUnaryRulesByParent(parent)){
						//NB: reflexive transitive closure!
						double currScore = Math.log(rule.getScore())+chart.get(min, max, rule.getChildren()[0]);
						
						if (currScore > bestScore) {
							bestScore = currScore;
							optRule = rule;
						}

					}

					if (bestScore > chart.get(min, max, parent)) { 
						chart.set(min, max, parent, bestScore, optRule);
					}
				}
			}
		}



		// use back pointers to create a tree
		Tree<String> annotatedBestParse = traverseBackPointers(sentence, chart);

		return annotator.unAnnotateTree(annotatedBestParse);
	}



	public BaselineCkyParser(List<Tree<String>> trainTrees, TreeAnnotator annotator) {

		this.annotator = annotator;


		System.out.print("Annotating / binarizing training trees ... ");
		List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);

		System.out.println("done.");

		System.out.print("Building grammar ... ");
		grammar = new UnaryClosedGrammar(annotatedTrainTrees);
		lexicon = new Lexicon(annotatedTrainTrees);
		System.out.println("done. (" + grammar.getStates().size() + " states)");

		// use the unary closure to support unary rules in the CKY algorithm
		//unaryClosure = new UnaryClosure(grammar);

	}

	private List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			annotatedTrees.add(annotator.annotateTree(tree));
		}
		return annotatedTrees;
	}


	public double getLogScore(Tree<String> tree){
		Tree<String> annotatedTree = annotator.annotateTree(tree);
		return logScoreHelper(annotatedTree);
	}

	private double logScoreHelper(Tree<String> tree) {
		List<Tree<String>> children = tree.getChildren();
		int arity = children.size();
		String inputSymbol = tree.getLabel();
		String[] outputs = new String[arity];
		for (int i = 0; i<arity; i++){
			outputs[i]=children.get(i).getLabel();
		}
		double logScore = 0;
		boolean found = false;

		if(tree.isPreTerminal()){
			double score = lexicon.scoreTagging(outputs[0], tree.getLabel());
			if (!Double.isNaN(score)){
				logScore += Math.log(score);
				found = true;
			}
		}
		else{
			//add the scores of the children 
			for (Tree<String> child : children){
				logScore += logScoreHelper(child);
			}
			// and compute the score of this node:
			List<Rule> possibleRules;
			switch(arity){
			case 1:{
				possibleRules = grammar.getUnaryRulesByParent(inputSymbol); break;
			}
			case 2:{
				possibleRules = grammar.getBinaryRulesByParent(inputSymbol); break;
			}
			default:{
				System.err.println("logScoreHelper called for non-binary tree");
				possibleRules = grammar.getBinaryRulesByParent(inputSymbol);
			}
			}
			for (Rule rule : possibleRules){
				//check if the rule matches:
				found = true;
				for (int i = 0; i<arity; i++){
					if (!rule.getChildren()[i].equals(outputs[i])){
						found = false;
					}
				}
				//if it does: add the score
				if (found){
					logScore += Math.log(rule.getScore());
					break; //and stop searching for the rule
				}
			}
		}
		return found? logScore : Double.NEGATIVE_INFINITY;
	}
}
