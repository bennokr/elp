package nlp.assignments.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.util.CollectionUtils;
import nlp.util.Counter;

/**
 * Calculates and provides accessors for the REFLEXIVE, TRANSITIVE closure of the unary rules in the provided Grammar.
 * Each rule in this closure stands for zero or more unary rules in the original grammar.  Use the getPath() method to
 * retrieve the full sequence of symbols (from parent to child) which support that path.
 */
class UnaryClosure {
	private Map<String, List<Rule>> closedUnaryRulesByChild = new HashMap<String, List<Rule>>();
	private Map<String, List<Rule>> closedUnaryRulesByParent = new HashMap<String, List<Rule>>();
	private Map<Rule, List<String>> pathMap = new HashMap<Rule, List<String>>();

	public List<Rule> getClosedUnaryRulesByChild(String child) {
		return CollectionUtils.getValueList(closedUnaryRulesByChild, child);
	}

	public List<Rule> getClosedUnaryRulesByParent(String parent) {
		return CollectionUtils.getValueList(closedUnaryRulesByParent, parent);
	}

	public List<String> getPath(Rule unaryRule) {
		return pathMap.get(unaryRule);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String parent : closedUnaryRulesByParent.keySet()) {
			for (Rule unaryRule : getClosedUnaryRulesByParent(parent)) {
				List<String> path = getPath(unaryRule);
				//          if (path.size() == 2) continue;
				sb.append(unaryRule);
				sb.append("  ");
				sb.append(path);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public UnaryClosure(Collection<Rule> unaryRules) {
		Map<Rule, List<String>> closureMap = computeUnaryClosure(unaryRules);
		for (Rule unaryRule : closureMap.keySet()) {
			addUnary(unaryRule, closureMap.get(unaryRule));
		}
	}

	private void addUnary(Rule unaryRule, List<String> path) {
		CollectionUtils.addToValueList(closedUnaryRulesByChild, unaryRule.getChildren()[0], unaryRule);
		CollectionUtils.addToValueList(closedUnaryRulesByParent, unaryRule.getParent(), unaryRule);
		pathMap.put(unaryRule, path);
	}

	private static Map<Rule, List<String>> computeUnaryClosure(Collection<Rule> unaryRules) {

		Map<Rule, String> intermediateStates = new HashMap<Rule, String>();
		Counter<Rule> pathCosts = new Counter<Rule>();
		Map<String, List<Rule>> closedUnaryRulesByChild = new HashMap<String, List<Rule>>();
		Map<String, List<Rule>> closedUnaryRulesByParent = new HashMap<String, List<Rule>>();

		Set<String> states = new HashSet<String>();

		for (Rule unaryRule : unaryRules) {
			relax(pathCosts, intermediateStates, closedUnaryRulesByChild, closedUnaryRulesByParent, unaryRule, null, unaryRule.getScore());
			states.add(unaryRule.getParent());
			states.add(unaryRule.getChildren()[0]);
		}

		for (String intermediateState : states) {
			List<Rule> incomingRules = closedUnaryRulesByChild.get(intermediateState);
			List<Rule> outgoingRules = closedUnaryRulesByParent.get(intermediateState);
			if (incomingRules == null || outgoingRules == null) continue;
			for (Rule incomingRule : incomingRules) {
				for (Rule outgoingRule : outgoingRules) {
					Rule rule = new Rule(incomingRule.getParent(), outgoingRule.getChildren(),true);
					double newScore = pathCosts.getCount(incomingRule) * pathCosts.getCount(outgoingRule);
					relax(pathCosts, intermediateStates, closedUnaryRulesByChild, closedUnaryRulesByParent, rule, intermediateState, newScore);
				}
			}
		}

		for (String state : states) {
			String[] self = {state};
			Rule selfLoopRule = new Rule(state, self, true);
			relax(pathCosts, intermediateStates, closedUnaryRulesByChild, closedUnaryRulesByParent, selfLoopRule, null, 1.0);
		}

		Map<Rule, List<String>> closureMap = new HashMap<Rule, List<String>>();

		for (Rule unaryRule : pathCosts.keySet()) {
			unaryRule.setScore(pathCosts.getCount(unaryRule));
			List<String> path = extractPath(unaryRule, intermediateStates);
			closureMap.put(unaryRule, path);
		}

		System.out.println("SIZE: " + closureMap.keySet().size());

		return closureMap;

	}

	private static List<String> extractPath(Rule unaryRule, Map<Rule, String> intermediateStates) {
		List<String> path = new ArrayList<String>();
		path.add(unaryRule.getParent());
		String intermediateState = intermediateStates.get(unaryRule);
		if (intermediateState != null) {
			String[] inter = {intermediateState};
			List<String> parentPath = extractPath(new Rule(unaryRule.getParent(), inter, true), intermediateStates);
			for (int i = 1; i < parentPath.size() - 1; i++) {
				String state = parentPath.get(i);
				path.add(state);
			}
			path.add(intermediateState);
			List<String> childPath = extractPath(new Rule(intermediateState, unaryRule.getChildren(),true), intermediateStates);
			for (int i = 1; i < childPath.size() - 1; i++) {
				String state = childPath.get(i);
				path.add(state);
			}
		}
		if (path.size() == 1 && unaryRule.getParent().equals(unaryRule.getChildren())) return path;
		path.add(unaryRule.getChildren()[0]);
		return path;
	}


	private static void relax(Counter<Rule> pathCosts, Map<Rule, String> intermediateStates, Map<String, List<Rule>> closedUnaryRulesByChild, Map<String, List<Rule>> closedUnaryRulesByParent, Rule unaryRule, String intermediateState, double newScore) {
		if (intermediateState != null && (intermediateState.equals(unaryRule.getParent()) || intermediateState.equals(unaryRule.getChildren()[0]))) return;
		boolean isNewRule = !pathCosts.containsKey(unaryRule);
		double oldScore = (isNewRule ? Double.NEGATIVE_INFINITY : pathCosts.getCount(unaryRule));
		if (oldScore > newScore) return;
		if (isNewRule) {
			CollectionUtils.addToValueList(closedUnaryRulesByChild, unaryRule.getChildren()[0], unaryRule);
			CollectionUtils.addToValueList(closedUnaryRulesByParent, unaryRule.getParent(), unaryRule);
		}
		pathCosts.setCount(unaryRule, newScore);
		intermediateStates.put(unaryRule, intermediateState);
	}

}