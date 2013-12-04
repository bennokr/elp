package nlp.assignments.parsing;

import java.util.ArrayList;
import java.util.List;

import nlp.ling.Tree;
import nlp.util.CollectionUtils;

public class UnaryClosedGrammar extends Grammar {

	UnaryClosure unaryClosure;

	public UnaryClosedGrammar(List<Tree<String>> trainTrees) {
		super(trainTrees);
		unaryClosure = new UnaryClosure(this.unaryRules);
	}

	public List<Rule> getUnaryRulesByChild(String child) {
		return unaryClosure.getClosedUnaryRulesByChild(child);
	}

	public List<Rule> getUnaryRulesByParent(String parent) {
		return unaryClosure.getClosedUnaryRulesByParent(parent);
	}

	public List<Rule> getUnaryRules() {

		List<Rule>  output = new ArrayList<Rule>();
		for(Rule rule : unaryRules){
			output.add(rule);
			String parent = rule.getParent();
			output.addAll(getUnaryRulesByParent(parent)); //NB only possible if unaryClosure is already instantiated
			// BUT may contain double rules!
		}
		return output;

	}




	//	public UnaryClosedGrammar(Grammar grammar) {
	//		super(grammar);
	//		unaryClosure = new UnaryClosure(this);
	//	}

}
