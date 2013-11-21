package nlp.assignments.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.ling.Tree;
import nlp.util.CollectionUtils;
import nlp.util.Counter;

/**
   * Simple implementation of a PCFG grammar, offering the ability to look up rules by their child symbols.  Rule
   * probability estimates are just relative frequency estimates off of training trees.
   */
  class Grammar {
    Map<String, List<Rule>> binaryRulesByLeftChild = new HashMap<String, List<Rule>>();
    Map<String, List<Rule>> binaryRulesByRightChild = new HashMap<String, List<Rule>>();
    Map<String, List<Rule>> binaryRulesByParent = new HashMap<String, List<Rule>>();
    List<Rule> binaryRules = new ArrayList<Rule>();
    Map<String, List<Rule>> unaryRulesByChild = new HashMap<String, List<Rule>>();
    Map<String, List<Rule>> unaryRulesByParent = new HashMap<String, List<Rule>>();
    List<Rule> unaryRules = new ArrayList<Rule>();
    Set<String> states = new HashSet<String>();

    public List<Rule> getBinaryRulesByLeftChild(String leftChild) {
      return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
    }

    public List<Rule> getBinaryRulesByRightChild(String rightChild) {
      return CollectionUtils.getValueList(binaryRulesByRightChild, rightChild);
    }

    public List<Rule> getBinaryRulesByParent(String parent) {
      return CollectionUtils.getValueList(binaryRulesByParent, parent);
    }

    public List<Rule> getBinaryRules() {
      return binaryRules;
    }

    public List<Rule> getUnaryRulesByChild(String child) {
      return CollectionUtils.getValueList(unaryRulesByChild, child);
    }

    public List<Rule> getUnaryRulesByParent(String parent) {
      return CollectionUtils.getValueList(unaryRulesByParent, parent);
    }

    public List<Rule> getUnaryRules() {
      return unaryRules;
    }

    public Set<String> getStates() {
      return states;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      List<String> ruleStrings = new ArrayList<String>();
      for (String parent : binaryRulesByParent.keySet()) {
        for (Rule binaryRule : getBinaryRulesByParent(parent)) {
          ruleStrings.add(binaryRule.toString());
        }
      }
      for (String parent : unaryRulesByParent.keySet()) {
        for (Rule unaryRule : getUnaryRulesByParent(parent)) {
          ruleStrings.add(unaryRule.toString());
        }
      }
      for (String ruleString : CollectionUtils.sort(ruleStrings)) {
        sb.append(ruleString);
        sb.append("\n");
      }
      return sb.toString();
    }

    private void addRule(Rule rule){
    	String parent = rule.getParent();
    	String[] children = rule.getChildren();
    	states.add(parent);
    	for( String child : children){
    		states.add(child);
    	}
    	int arity = rule.getArity();
    	if (arity==1){
    		unaryRules.add(rule);
    	      CollectionUtils.addToValueList(unaryRulesByParent, parent, rule);
    	      CollectionUtils.addToValueList(unaryRulesByChild, children[0], rule);
    	}
    	if (arity==2){
    		binaryRules.add(rule);
    	      CollectionUtils.addToValueList(binaryRulesByParent, parent, rule);
    	      CollectionUtils.addToValueList(binaryRulesByLeftChild, children[0], rule);
    	      CollectionUtils.addToValueList(binaryRulesByRightChild, children[1], rule);
    	}
    }


    public Grammar(List<Tree<String>> trainTrees) {
      Counter<Rule> unaryRuleCounter = new Counter<Rule>();
      Counter<Rule> binaryRuleCounter = new Counter<Rule>();
      Counter<String> symbolCounter = new Counter<String>();
      for (Tree<String> trainTree : trainTrees) {
        tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter);
      }
      for (Rule unaryRule : unaryRuleCounter.keySet()) {
        double unaryProbability = unaryRuleCounter.getCount(unaryRule) / symbolCounter.getCount(unaryRule.getParent());
        unaryRule.setScore(unaryProbability);
        addRule(unaryRule);
      }
      for (Rule binaryRule : binaryRuleCounter.keySet()) {
        double binaryProbability = binaryRuleCounter.getCount(binaryRule) / symbolCounter.getCount(binaryRule.getParent());
        binaryRule.setScore(binaryProbability);
        addRule(binaryRule);
      }
    }

    private void tallyTree(Tree<String> tree, Counter<String> symbolCounter, Counter<Rule> unaryRuleCounter, Counter<Rule> binaryRuleCounter) {
      if (tree.isLeaf()) return;
      if (tree.isPreTerminal()) return;
      int arity = tree.getChildren().size();
      Rule rule = makeRule(tree);
      symbolCounter.incrementCount(tree.getLabel(), 1.0);
      if(arity == 1){
    	  unaryRuleCounter.incrementCount(rule, 1.0);
      }
      if(arity == 2){
    	  binaryRuleCounter.incrementCount(rule, 1.0);
      }
      if (arity < 1 || arity > 2) {
        throw new RuntimeException("Attempted to construct a Grammar with an illegal tree (unbinarized?): " + tree);
      }
      for (Tree<String> child : tree.getChildren()) {
        tallyTree(child, symbolCounter, unaryRuleCounter, binaryRuleCounter);
      }
    }

    private Rule makeRule(Tree<String> tree){
    	List<Tree<String>> kids = tree.getChildren();
    	String[] children = new String[kids.size()];
    	for (int i = 0; i<kids.size(); i++){
    		children[i] = kids.get(i).getLabel();
    	}
    	return new Rule(tree.getLabel(),children,true);
    }
  
  }