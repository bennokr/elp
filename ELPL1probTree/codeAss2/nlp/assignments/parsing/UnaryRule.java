package nlp.assignments.parsing;

public class UnaryRule extends Rule {

//	String parent;
//	String child;
//	double score;

	public String getParent() {
		return super.getParent();
	}

	public String getChild() {
		return super.getChildren()[0];
	}

	public double getScore() {
		return super.getScore();
	}

	public void setScore(double score) {
		super.setScore(score);
	}

	public boolean equals(Object o) {
		return super.equals(o);	
				}

	public int hashCode() {
		return super.hashCode();
	}

	public String toString() {
		return super.toString();
	}

	public UnaryRule(String parent, String child) {
		super(parent,new String[0],false);
		String[] children = {child};
		super.setChildren(children);
	}
}
