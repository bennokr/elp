package nlp.assignments.parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import nlp.assignments.parsing.BaselineCkyParser.Chart;
import nlp.assignments.parsing.BaselineCkyParser.Chart.EdgeInfo;
import nlp.assignments.parsing.EdgeInfo.type;
import nlp.ling.Tree;

public class Chart {
	static class EdgeInfo {

		enum type{PRETERMINAL, UNARY, BINARY};
	
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
	
/*	
	void set(int i, int j, String label, double score) {
		chart.get(i).get(j).put(label, new EdgeInfo(score));
	}
	
	void setBackPointer(int i, int j, String label, Rule rule, int midPoint) {
		EdgeInfo edgeInfo = chart.get(i).get(j).get(label);
		edgeInfo.rule = rule;
		edgeInfo.mid = midPoint;
	}
*/	
	
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

	
	EdgeInfo getinfo(int i, int j, String label){
		return chart.get(i).get(j).get(label);
	}

	@Override
	public String toString() {
		return chart.toString();
	}

}
