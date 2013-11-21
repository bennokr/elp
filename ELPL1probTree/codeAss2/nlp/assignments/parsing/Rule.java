package nlp.assignments.parsing;

public class Rule {

	private String parent;
	private int arity;
	private String[] children;
	private double score;
	private boolean isAnnotated;

	public String getParent() {
		return parent;
	}

	public String[] getChildren() {
		return children;
	}

	public int getArity(){
		return arity;	    	
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public void setAnnotated(boolean annotated){
		this.isAnnotated = annotated;
	}

	public boolean getAnnotated(){
		return isAnnotated;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Rule)) return false;

		final Rule rule = (Rule) o;

		if (parent != null ? !parent.equals(rule.parent) : rule.parent != null) return false;
		for (int i = 0; i<children.length; i++){
			if (children[i] != null ? !children[i].equals(rule.children[i]) : rule.children[i] != null) return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (parent != null ? parent.hashCode() : 0);
		for (String child: children){
			result = 29 * result + (child != null ? child.hashCode() : 0);
		}
		return result;
	}

	public String toString() {
		String result = parent+" ->";
		for (String child: children){
			result += child + " ";
		}
		return result + "%% " + score;
	}

	public Rule(String parent, String[] children, boolean annotated) {
		this.arity = children.length;
		this.parent = parent;
		this.children = children;
		this.arity = children.length;
		this.isAnnotated = annotated;
	}

	public void setChildren(String[] children) {
		this.children = children;
		this.arity = children.length;
	}


}
