package nlp.assignments.parsing;

public class BinaryRuleMock extends Rule {

	public BinaryRuleMock(String parent, String[] children, boolean annotated) {
		super(parent, children, annotated);
		// TODO Auto-generated constructor stub
	}
	
	  public BinaryRuleMock(String parent, String leftChild, String rightChild) {
		  super(parent, null,false);
		  String[] children = new String[2];
	      children[0]=leftChild;
	      children[1]=rightChild;
	      super.setChildren(children);
	  }
	  
	  public String getLeftChild() {
	      return super.getChildren()[0];
	    }

	    public String getRightChild() {
	      return super.getChildren()[1];
	    }


}
