package nlp.assignments.parsing;

import java.util.ArrayList;
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
			BinaryRule rule = null;
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

		void setBackPointer(int i, int j, String label, BinaryRule rule, int midPoint) {
			EdgeInfo edgeInfo = chart.get(i).get(j).get(label);
			edgeInfo.rule = rule;
			edgeInfo.mid = midPoint;
		}



		int getMidPoint(int i, int j, String label) {
			return chart.get(i).get(j).get(label).mid;
		}

		BinaryRule getRule(int i, int j, String label) {
			return chart.get(i).get(j).get(label).rule;
		}

		@Override
		public String toString() {
			return chart.toString();
		}

	}

	void traverseBackPointersHelper(List<String> sent, Chart chart, int i, int j, Tree<String> currTree) {
		String parent = currTree.getLabel();

		/** 
		 * TODO 
		 * This method needs to be updated to keep print out unary rules used 
		 */

		// binary rules
		if (j - i > 1) {

			BinaryRule rule = chart.getRule(i, j, parent);
			int mid = chart.getMidPoint(i, j, parent);
			List<Tree<String>> children = new ArrayList<Tree<String>>(2);

			Tree<String> t1 = new Tree<String>(rule.getLeftChild()); 
			traverseBackPointersHelper(sent, chart, i, mid, t1);
			children.add(t1);

			Tree<String> t2 = new Tree<String>(rule.getRightChild());
			traverseBackPointersHelper(sent, chart, mid, j, t2);
			children.add(t2);

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

		// CKY for binary trees
		for (int max = 2; max <= sentence.size(); max++) {
			for (int min = max - 2; min >= 0; min--) {
				// first without unary roles
				for (String parent : grammar.states) {
					double bestScore = Double.NEGATIVE_INFINITY;
					int optMid =-1;
					BinaryRule optRule = null;
					// parent -> c1 c2
					for (BinaryRule rule : grammar.getBinaryRulesByParent(parent)) {
						for (int mid = min + 1; mid <  max; mid++) {
							double score1 = chart.get(min, mid, rule.getLeftChild());
							double score2 = chart.get(mid, max, rule.getRightChild());
							double currScore = score1 * score2 * rule.getScore();
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
		String output="";		
		if (children.size()>0){output = children.get(0).getLabel();}
		double logScore;
		if (tree.isPreTerminal()){
			logScore = Math.log(lexicon.scoreTagging(tree.getLabel(), output));
			if (Double.isNaN(logScore)){
				logScore = 0; // Double.NEGATIVE_INFINITY ?
			}
		//	System.out.println("Preterminal: "+tree.getLabel() +", output: "+ output);
		//	System.out.println("Lexiconscore: " +lexicon.scoreTagging(output,tree.getLabel()));
		//	if (lexicon.isKnown(output.toLowerCase())){				
		//		logScore = Math.log(lexicon.scoreTagging(tree.getLabel(), output));
		//	}
		//	else{
		//		logScore = Double.NEGATIVE_INFINITY; //unknown word: -infinity?
		//	}
		}
		else{ //add the scores of the children 
			logScore = 0;
			for (Tree<String> child : children){
				logScore += logScoreHelper(child);
			}
			// and the score of this node
			String inputSymbol = tree.getLabel();
			switch(children.size()){
			case 0: {
				System.err.println("logScoreHelper called for leaf");
				logScore = 0; //We're at a leaf somehow, return no score
				break;
			}			
			case 1:{
				if (inputSymbol.equals(output)) { //reflexivity, prob = 1 so logprob is 0
					logScore += 0;
				}
				else{
					List<UnaryRule> possibleRules1 =  grammar.getUnaryRulesByParent(inputSymbol);
					for (UnaryRule rule : possibleRules1){
						if (rule.child.equals(output)){
							logScore += Math.log(rule.score);
						}
					}
				}
				break;
			}
			case 2:{
				String outputL = output;
				String outputR = children.get(1).getLabel();
				List<BinaryRule> possibleRules2 =  grammar.getBinaryRulesByParent(inputSymbol);
				for (BinaryRule rule : possibleRules2){
					if (rule.leftChild.equals(outputL) & rule.rightChild.equals(outputR)){
						logScore += Math.log(rule.score);
					}
				}
				break;
			}
			default:{System.err.println("logScoreHelper called for non-binary tree");}
			}
		}
		return logScore;
	}

	/*
	@Override
	public double getLogScore(Tree<String> tree) {
		Tree<String> annotatedTree = annotator.annotateTree(tree);

//	 * TODO: Add method which uses the annotatedTree (not the 'tree') and compute the log probability of the tree

		double logScore = 0;

		if (annotatedTree.isLeaf()){
			return 0; //something
		}else {
			List<Tree<String>> children = annotatedTree.getChildren();
			String inputSymbol = annotatedTree.getLabel(); 		// to recover the rule that was used (should be possible to do easier, by annotations)

			for (Tree<String> child : children){
				logScore += getLogScore(child);					// add the scores of the subtrees
			}

			logScore += (annotatedTree.isPreTerminal()?
					lexiconLogScore(inputSymbol,children.get(0).getLabel())
					:
					nodeLogScore(inputSymbol, children));


		}
//		System.err.println("score is now " + logScore);
		return logScore;
	}

	private double lexiconLogScore(String preterminal, String terminal) {
		return Math.log(lexicon.scoreTagging(terminal, preterminal));
	}

	 *//*Get the score of the rule used to generate the `outputSymbols` from the `inputSymbol`
	 * @param inputSymbol String of unknown format?
	 * @param outputSymbols Array of length 1 or length 2!
	 * @return
	 *//*
	private double nodeLogScore(String inputSymbol, List<Tree<String>> outputSymbols) {

		double score = Double.NEGATIVE_INFINITY;		
		switch(outputSymbols.size()){
			case 1: {
				String child = outputSymbols.get(0).getLabel();

				if (inputSymbol.equals(child)) { //reflexivity, prob = 1 so logprob is 0
					return 0;
				}
				List<UnaryRule> possibleRules1 =  grammar.getUnaryRulesByParent(inputSymbol);
				for (UnaryRule rule : possibleRules1){
					if (rule.child.equals(child)){
						score = Math.log(rule.score);
						return score;
					}
				}
				System.err.println("could not find rule: " + inputSymbol  + " to " + child + " in " + possibleRules1.size() + " rules:");
				for (UnaryRule rule : possibleRules1){
					System.err.println(" " + rule);
				}
				break;
			}
			case 2: {
				String left = outputSymbols.get(0).getLabel();
				String right = outputSymbols.get(1).getLabel();
				List<BinaryRule> possibleRules2 = grammar.getBinaryRulesByParent(inputSymbol);
				for (BinaryRule rule : possibleRules2){
					if (rule.leftChild.equals(left)&&rule.rightChild.equals(right)){
						score = Math.log(rule.score);
						return score;
					}
				}
				break;
			}
			default: {
				System.err.println("SHOULD NEVER BE REACHED: length=" + outputSymbols.size());
				break;
			}
		}
//		System.err.println("nodeLogScore is " + score);
		return score;
	}
	  

*/

}