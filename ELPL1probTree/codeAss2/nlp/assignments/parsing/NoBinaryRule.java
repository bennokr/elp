package nlp.assignments.parsing;

public class NoBinaryRule extends Rule {

	public NoBinaryRule(String parent, String[] children, boolean annotated) {
		super(parent, children, annotated);
		// TODO Auto-generated constructor stub
	}
	
	  public NoBinaryRule(String parent, String leftChild, String rightChild) {
		  super(parent, new String[0],false);
		  String[] children = {leftChild,rightChild};
	      super.setChildren(children);
	  }
	  
	  public String getLeftChild() {
	      return super.getChildren()[0];
	    }

	    public String getRightChild() {
	      return super.getChildren()[1];
	    }


}
