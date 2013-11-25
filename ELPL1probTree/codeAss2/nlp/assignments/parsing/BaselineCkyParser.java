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

	//UnaryClosure unaryClosure;

	static class Chart {
		/*
		 * TODO
		 *  This class (and enclosed EdgeInfo) needs to be changed to keep track of unary rules (and unary chains) used
		 */

		static class EdgeInfo {
			double score; 
			Rule rule = null;
			int mid = -1;
			EdgeInfo(double score) {
				this.score = score;
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

		Map<Integer,Map<Integer,Map<String, EdgeInfo>>> chart = new HashMap<Integer, Map<Integer,Map<String,EdgeInfo>>>();
		Chart(int seqLength) {
			for (int i = 0; i < seqLength; i++) {
				chart.put(i, new HashMap<Integer, Map<String,EdgeInfo>>());
				for (int j = i + 1; j <= seqLength; j++) {
					chart.get(i).put(j, new HashMap<String, EdgeInfo>());
				}
			}
		}

		void set(int i, int j, String label, double score) {
			chart.get(i).get(j).put(label, new EdgeInfo(score));
		}
		double get(int i, int j, String label) {
			Map<String,EdgeInfo> edgeScores = chart.get(i).get(j);
			if (!edgeScores.containsKey(label)) {
				return 0;
			} else {
				return edgeScores.get(label).score;
			}
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

		void setBackPointer(int i, int j, String label, Rule rule, int midPoint) {
			EdgeInfo edgeInfo = chart.get(i).get(j).get(label);
			edgeInfo.rule = rule;
			edgeInfo.mid = midPoint;
		}



		int getMidPoint(int i, int j, String label) {
			return chart.get(i).get(j).get(label).mid;
		}

		Rule getRule(int i, int j, String label) {
			return chart.get(i).get(j).get(label).rule;
		}

		@Override
		public String toString() {
			return chart.toString();
		}

	}

	private List<Tree<String>> traverseBackPointersHelper2(List<String> sentence,
			Chart chart, int i, int j, String parent) {
		int mid = chart.getMidPoint(i, j, parent);

		Rule rule = chart.getRule(i, j, parent);
		String[] children = rule.getChildren();
		int nChildren = rule.getArity();

		List<Tree<String>> toReturn = new ArrayList<Tree<String>>(nChildren);

		boolean second = false;
		for (String child : children){
			Tree<String> baby = new Tree<String>(child);
			baby.setChildren(second?
					traverseBackPointersHelper2(sentence,chart,i,mid, child):
					traverseBackPointersHelper2(sentence,chart,mid,j, child));
			if(nChildren ==2){
				second = true;
			}
			toReturn.add(baby);
		}



		return toReturn;
	}


	void traverseBackPointersHelper(List<String> sent, Chart chart, int i, int j, Tree<String> currTree) {
		String parent = currTree.getLabel();



		/** 
		 * TODO 
		 * This method needs to be updated to keep print out unary rules used 
		 */

		// binary rules
		if (j - i > 1) {

			Rule rule = chart.getRule(i, j, parent);
			int mid = chart.getMidPoint(i, j, parent);
			int nChildren = rule.getChildren().length; //should be 2
			List<Tree<String>> children = new ArrayList<Tree<String>>(nChildren);

			for (int c = 0; c<nChildren; c++){
				Tree<String> t = new Tree<String>(rule.getChildren()[c]); 
				traverseBackPointersHelper(sent, chart, i, mid, t);
				children.set(c, t);
			}


			currTree.setChildren(children);

			// preterminal production
		} else {
			assert j - i == 1;

			Tree<String> termProd = new Tree<String>(sent.get(i));
			currTree.setChildren(Collections.singletonList(termProd));
		}

	}

	// traverse back pointers and create a tree
	Tree<String> traverseBackPointers(List<String> sentence, Chart chart) {

		Tree<String> annotatedBestParse;
		if (!chart.getAllCandidateLabels(0, sentence.size()).contains("ROOT")) {
			// this line is here only to make sure that a baseline without binary rules can output something 
			annotatedBestParse = new Tree<String>(chart.getBestLabel(0, sentence.size()));
		} else {
			// in reality we always want to start with the ROOT symbol of the grammar
			annotatedBestParse = new Tree<String>("ROOT");
		}
		traverseBackPointersHelper(sentence, chart, 0, sentence.size(), annotatedBestParse);
		return annotatedBestParse;

	}

	Tree<String> traverseBackPointers2(List<String> sentence, Chart chart) {
		Tree<String> annotatedBestParse = new Tree<String>("ROOT");
		annotatedBestParse.setChildren(traverseBackPointersHelper2(sentence, chart, 0, sentence.size(), "ROOT"));

		return annotatedBestParse;
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
				chart.set(k, k + 1, preterm, lexicon.scoreTagging(sentence.get(k), preterm));

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
								double currScore = rule.getScore();
								currScore += chart.get(min, mid, rule.getChildren()[0]);
								currScore += chart.get(mid, max, rule.getChildren()[1]);
								if (currScore > bestScore) {
									bestScore = currScore;
									optMid = mid;
									optRule = rule;;
								}
							}
						}
						if (bestScore != Double.NEGATIVE_INFINITY) { 
							chart.set(min, max, parent, bestScore);
							chart.setBackPointer(min, max, parent, optRule, optMid);
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
						double currScore = rule.getScore();
						for (String child : rule.getChildren()){
							currScore += chart.get(min, max, child);
						}
						if (currScore > bestScore) {
							bestScore = currScore;
							optRule = rule;;
						}

					}

					if (bestScore != Double.NEGATIVE_INFINITY) { 
						chart.set(min, max, parent, bestScore);
						chart.setBackPointer(min, max, parent, optRule, max); //mid = max, convenient for traverseBackPointers

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
		grammar = new Grammar(annotatedTrainTrees);
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
