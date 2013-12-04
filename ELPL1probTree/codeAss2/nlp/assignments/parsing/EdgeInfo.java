package nlp.assignments.parsing;

class EdgeInfo {

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





