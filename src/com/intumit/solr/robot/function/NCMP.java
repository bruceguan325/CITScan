package com.intumit.solr.robot.function;

import com.intumit.solr.robot.QAContext;

import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.operator.Operator;

/**
 * Return number comparison
 * @author herb
 */
public class NCMP extends FunctionBase {
	
	Operator eq = new Operator("=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

        @Override
        public double apply(double[] values) {
            if (values[0] == values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };
	
	Operator gteq = new Operator(">=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

        @Override
        public double apply(double[] values) {
            if (values[0] >= values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };
	
	Operator gt = new Operator(">", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

        @Override
        public double apply(double[] values) {
            if (values[0] > values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };
    
	Operator lteq = new Operator("<=", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

        @Override
        public double apply(double[] values) {
            if (values[0] <= values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };
    
	Operator lt = new Operator("<", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

        @Override
        public double apply(double[] values) {
            if (values[0] < values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };

	public NCMP(String originalText, String data) {
		super(originalText, data);
	}

	@Override
	public Object exec(QAContext ctx, UserInput in) {
		if (data.contains("[[") && data.contains("]]")) {
			data = data.replace("[[", "{{").replace("]]", "}}");
			data = FunctionUtil.collectExecAndReplace(data, ctx);
		}
		
		double result = new ExpressionBuilder(data)
		        .operator(eq)
		        .operator(gt)
		        .operator(lt)
		        .operator(gteq)
		        .operator(lteq)
		        .build()
		        .evaluate();
		
		return result == 1d ? Boolean.TRUE : Boolean.FALSE;
	}
}