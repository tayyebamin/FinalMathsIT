package util;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import util.AngleMode;

public class Expression {

	/**
	 * Definition of PI as a constant, can be used in expressions as variable.
	 */
	public static final BigDecimal PI = new BigDecimal(
			"3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

	/**
	 * Definition of e: "Euler's number" as a constant, can be used in
	 * expressions as variable.
	 */
	public static final BigDecimal e = new BigDecimal(
			"2.71828182845904523536028747135266249775724709369995957496696762772407663");

	/**
	 * The {@link MathContext} to use for calculations.
	 */
	private MathContext mc = null;

	/**
	 * The original infix expression.
	 */
	private String originalExpression;
	private String reducedExpression;

	public AngleMode EAngleMode;

	/**
	 * The current infix expression, with optional variable substitutions.
	 */
	private String expression = null;

	public void setExpression(String expression) {
		this.expression = expression;
		originalExpression = expression;
	}

	/**
	 * The cached RPN (Reverse Polish Notation) of the expression.
	 */
	private List<String> rpn = null;

	/**
	 * All defined operators with name and implementation.
	 */
	private Map<String, Operator> operators = new TreeMap<String, Operator>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * All defined functions with name and implementation.
	 */
	private Map<String, LazyFunction> functions = new TreeMap<String, LazyFunction>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * All defined variables with name and value.
	 */
	private Map<String, BigDecimal> variables = new TreeMap<String, BigDecimal>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * What character to use for decimal separators.
	 */
	private static final char decimalSeparator = '.';

	/**
	 * What character to use for minus sign (negative values).
	 */
	private static final char minusSign = '-';

	/**
	 * The BigDecimal representation of the left parenthesis, used for parsing
	 * varying numbers of function parameters.
	 */
	private static final LazyNumber PARAMS_START = new LazyNumber() {
		public BigDecimal eval() {
			return null;
		}
	};

	/**
	 * The expression evaluators exception class.
	 */
	public static class ExpressionException extends RuntimeException {
		private static final long serialVersionUID = 1118142866870779047L;

		public ExpressionException(String message) {
			super(message);
		}
	}

	/**
	 * LazyNumber interface created for lazily evaluated functions
	 */
	interface LazyNumber {
		BigDecimal eval();
	}

	interface LazyString {
		BigDecimal eval();

		String val();
	}

	public abstract class LazyFunction {
		/**
		 * Name of this function.
		 */
		private String name;
		/**
		 * Number of parameters expected for this function. <code>-1</code>
		 * denotes a variable number of parameters.
		 */
		private int numParams;

		/**
		 * Creates a new function with given name and parameter count.
		 *
		 * @param name
		 *            The name of the function.
		 * @param numParams
		 *            The number of parameters for this function.
		 *            <code>-1</code> denotes a variable number of parameters.
		 */
		public LazyFunction(String name, int numParams) {
			this.name = name.toUpperCase(Locale.ROOT);
			this.numParams = numParams;
		}

		public String getName() {
			return name;
		}

		public int getNumParams() {
			return numParams;
		}

		public boolean numParamsVaries() {
			return numParams < 0;
		}

		public abstract LazyNumber lazyEval(List<LazyNumber> lazyParams);
	}

	/**
	 * Abstract definition of a supported expression function. A function is
	 * defined by a name, the number of parameters and the actual processing
	 * implementation.
	 */
	public abstract class Function extends LazyFunction {

		public Function(String name, int numParams) {
			super(name, numParams);
		}

		public LazyNumber lazyEval(List<LazyNumber> lazyParams) {
			final List<BigDecimal> params = new ArrayList<BigDecimal>();
			for (LazyNumber lazyParam : lazyParams) {
				params.add(lazyParam.eval());
			}
			return new LazyNumber() {
				public BigDecimal eval() {
					return Function.this.eval(params);
				}
			};
		}

		/**
		 * Implementation for this function.
		 *
		 * @param parameters
		 *            Parameters will be passed by the expression evaluator as a
		 *            {@link List} of {@link BigDecimal} values.
		 * @return The function must return a new {@link BigDecimal} value as a
		 *         computing result.
		 */
		public abstract BigDecimal eval(List<BigDecimal> parameters);
	}

	/**
	 * Abstract definition of a supported operator. An operator is defined by
	 * its name (pattern), precedence and if it is left- or right associative.
	 */
	public abstract class Operator {
		/**
		 * This operators name (pattern).
		 */
		private String oper;
		/**
		 * Operators precedence.
		 */
		private int precedence;
		/**
		 * Operator is left associative.
		 */
		private boolean leftAssoc;

		/**
		 * Creates a new operator.
		 * 
		 * @param oper
		 *            The operator name (pattern).
		 * @param precedence
		 *            The operators precedence.
		 * @param leftAssoc
		 *            <code>true</code> if the operator is left associative,
		 *            else <code>false</code>.
		 */
		public Operator(String oper, int precedence, boolean leftAssoc) {
			this.oper = oper;
			this.precedence = precedence;
			this.leftAssoc = leftAssoc;
		}

		public String getOper() {
			return oper;
		}

		public int getPrecedence() {
			return precedence;
		}

		public boolean isLeftAssoc() {
			return leftAssoc;
		}

		/**
		 * Implementation for this operator.
		 * 
		 * @param v1
		 *            Operand 1.
		 * @param v2
		 *            Operand 2.
		 * @return The result of the operation.
		 */
		public abstract BigDecimal eval(BigDecimal v1, BigDecimal v2);
	}

	/**
	 * Expression tokenizer that allows to iterate over a {@link String}
	 * expression token by token. Blank characters will be skipped.
	 */
	private class Tokenizer implements Iterator<String> {

		/**
		 * Actual position in expression string.
		 */
		private int pos = 0;

		/**
		 * The original input expression.
		 */
		private String input;
		/**
		 * The previous token or <code>null</code> if none.
		 */
		private String previousToken;

		/**
		 * Creates a new tokenizer for an expression.
		 * 
		 * @param input
		 *            The expression string.
		 */
		public Tokenizer(String input) {
			this.input = input.trim();
		}

		@Override
		public boolean hasNext() {
			return (pos < input.length());
		}

		/**
		 * Peek at the next character, without advancing the iterator.
		 * 
		 * @return The next character or character 0, if at end of string.
		 */
		private char peekNextChar() {
			if (pos < (input.length() - 1)) {
				return input.charAt(pos + 1);
			} else {
				return 0;
			}
		}

		@Override
		public String next() {
			StringBuilder token = new StringBuilder();
			if (pos >= input.length()) {
				return previousToken = null;
			}
			char ch = input.charAt(pos);
			while (Character.isWhitespace(ch) && pos < input.length()) {
				ch = input.charAt(++pos);
			}
			if (Character.isDigit(ch)) {
				while ((Character.isDigit(ch) || ch == decimalSeparator || ch == 'e' || ch == 'E'
						|| (ch == minusSign && token.length() > 0
								&& ('e' == token.charAt(token.length() - 1) || 'E' == token.charAt(token.length() - 1)))
						|| (ch == '+' && token.length() > 0 && ('e' == token.charAt(token.length() - 1)
								|| 'E' == token.charAt(token.length() - 1))))
						&& (pos < input.length())) {
					token.append(input.charAt(pos++));
					ch = pos == input.length() ? 0 : input.charAt(pos);
				}
			} else if (ch == minusSign && Character.isDigit(peekNextChar()) && ("(".equals(previousToken)
					|| ",".equals(previousToken) || previousToken == null || operators.containsKey(previousToken))) {
				token.append(minusSign);
				pos++;
				token.append(next());
			} else if (Character.isLetter(ch) || (ch == '_')) {
				while ((Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_')) && (pos < input.length())) {
					token.append(input.charAt(pos++));
					ch = pos == input.length() ? 0 : input.charAt(pos);
				}
			} else if (ch == '(' || ch == ')' || ch == ',') {
				token.append(ch);
				pos++;
			} else {
				while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_' && !Character.isWhitespace(ch)
						&& ch != '(' && ch != ')' && ch != ',' && (pos < input.length())) {
					token.append(input.charAt(pos));
					pos++;
					ch = pos == input.length() ? 0 : input.charAt(pos);
					if (ch == minusSign) {
						break;
					}
				}
				if (!operators.containsKey(token.toString())) {
					throw new ExpressionException(
							"Unknown operator '" + token + "' at position " + (pos - token.length() + 1));
				}
			}
			return previousToken = token.toString();
		}

		// Tayyeb
		private boolean validhexa(Character ch) {
			if (ch == 'A' || ch == 'B' || ch == 'C' || ch == 'D' || ch == 'E' || ch == 'F' || Character.isDigit(ch)) {
				return true;
			}
			return false;
		}

		public String next16() {
			StringBuilder token = new StringBuilder();
			if (pos >= input.length()) {
				return previousToken = null;
			}
			char ch = input.charAt(pos);
			while (Character.isWhitespace(ch) && pos < input.length()) {
				ch = input.charAt(++pos);
			}
			if (validhexa(ch)) {
				while (validhexa(ch) || ch == decimalSeparator 
						|| (ch == minusSign && token.length() > 0
						|| (ch == '+' && token.length() > 0 ))
						&& (pos < input.length())) {
					token.append(input.charAt(pos++));
					ch = pos == input.length() ? 0 : input.charAt(pos);
				}
			} else if (ch == minusSign && (Character.isDigit(peekNextChar()) || validhexa(peekNextChar()))
					&& ("(".equals(previousToken) || ",".equals(previousToken) || previousToken == null
							|| operators.containsKey(previousToken))) {
				token.append(minusSign);
				pos++;
				token.append(next16());
			} else if (Character.isLetter(ch) || (ch == '_')) {
				while ((Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_')) && (pos < input.length())) {
					token.append(input.charAt(pos++));
					ch = pos == input.length() ? 0 : input.charAt(pos);
				}
			} else if (ch == '(' || ch == ')' || ch == ',') {
				token.append(ch);
				pos++;
			} else {
				while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_' && !Character.isWhitespace(ch)
						&& ch != '(' && ch != ')' && ch != ',' && (pos < input.length())) {
					token.append(input.charAt(pos));
					pos++;
					ch = pos == input.length() ? 0 : input.charAt(pos);
					if (ch == minusSign) {
						break;
					}
				}
				if (!operators.containsKey(token.toString())) {
					throw new ExpressionException(
							"Unknown operator '" + token + "' at position " + (pos - token.length() + 1));
				}
			}
			
			return previousToken = token.toString();
		}

		// end-Tayyeb
		@Override
		public void remove() {
			throw new ExpressionException("remove() not supported");
		}

		/**
		 * Get the actual character position in the string.
		 * 
		 * @return The actual character position.
		 */
		public int getPos() {
			return pos;
		}

	}

	/**
	 * Creates a new expression instance from an expression string with a given
	 * default match context of {@link MathContext#DECIMAL32}.
	 * 
	 * @param expression
	 *            The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
	 *            <code>"sin(y)>0 & max(z, 3)>3"</code>
	 */
	public Expression(String expression) {
		setExpression(expression);
		setOperators(MathContext.DECIMAL32);
	}

	public Expression() {
		super();
		setOperators(MathContext.DECIMAL32);
	}

	/**
	 * Creates a new expression instance from an expression string with a given
	 * default match context.
	 * 
	 * @param expression
	 *            The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
	 *            <code>"sin(y)>0 & max(z, 3)>3"</code>
	 * @param defaultMathContext
	 *            The {@link MathContext} to use by default.
	 */
	public void setOperators(MathContext defaultMathContext) {
		this.mc = MathContext.DECIMAL128;
		addOperator(new Operator("+", 20, true) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.add(v2, mc);
			}
		});
		addOperator(new Operator("-", 20, true) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.subtract(v2, mc);
			}
		});
		addOperator(new Operator("*", 30, true) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.multiply(v2, mc);
			}
		});
		addOperator(new Operator("/", 30, true) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.divide(v2, mc);
			}
		});
		addOperator(new Operator("%", 30, true) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.remainder(v2, mc);
			}
		});
		addOperator(new Operator("^", 40, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				/*- 
				 * Thanks to Gene Marin:
				 * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java
				 */
				int signOf2 = v2.signum();
				double dn1 = v1.doubleValue();
				v2 = v2.multiply(new BigDecimal(signOf2)); // n2 is now positive
				BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
				BigDecimal n2IntPart = v2.subtract(remainderOf2);
				BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
				BigDecimal doublePow = new BigDecimal(Math.pow(dn1, remainderOf2.doubleValue()));

				BigDecimal result = intPow.multiply(doublePow, mc);
				if (signOf2 == -1) {
					result = BigDecimal.ONE.divide(result, mc.getPrecision(), RoundingMode.HALF_UP);
				}
				return result;
			}
		});
		addOperator(new Operator("&&", 4, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				boolean b1 = !v1.equals(BigDecimal.ZERO);
				boolean b2 = !v2.equals(BigDecimal.ZERO);
				return b1 && b2 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addOperator(new Operator("||", 2, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				boolean b1 = !v1.equals(BigDecimal.ZERO);
				boolean b2 = !v2.equals(BigDecimal.ZERO);
				return b1 || b2 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addOperator(new Operator(">", 10, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.compareTo(v2) == 1 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addOperator(new Operator(">=", 10, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.compareTo(v2) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addOperator(new Operator("<", 10, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.compareTo(v2) == -1 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addOperator(new Operator("<=", 10, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.compareTo(v2) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addOperator(new Operator("=", 7, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.compareTo(v2) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("==", 7, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return operators.get("=").eval(v1, v2);
			}
		});

		addOperator(new Operator("!=", 7, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return v1.compareTo(v2) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("<>", 7, false) {
			@Override
			public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				return operators.get("!=").eval(v1, v2);
			}
		});

		addFunction(new Function("NOT", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				boolean zero = parameters.get(0).compareTo(BigDecimal.ZERO) == 0;
				return zero ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});

		addLazyFunction(new LazyFunction("IF", 3) {
			@Override
			public LazyNumber lazyEval(List<LazyNumber> lazyParams) {
				boolean isTrue = !lazyParams.get(0).eval().equals(BigDecimal.ZERO);
				return isTrue ? lazyParams.get(1) : lazyParams.get(2);
			}
		});

		addFunction(new Function("RANDOM", 0) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.random();
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("SIN", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal bd = BigDecimal.ZERO, angle = BigDecimal.ZERO;
				double d = 0.0;
				if (EAngleMode == null) {
					EAngleMode = AngleMode.DEGREE;
				}
				switch (EAngleMode) {
				case DEGREE:
					angle = parameters.get(0).remainder(new BigDecimal(360, mc));
					d = angle.doubleValue();
					// System.out.println("P: " +
					// parameters.get(0).toPlainString() + " angleDEG: " +
					// String.valueOf(angle.doubleValue()) + " Value: " +
					// String.valueOf(d));
					d = Math.sin(Math.toRadians(d));
					bd = new BigDecimal(d, mc);
					bd = bd.setScale(15, RoundingMode.HALF_UP);
					break;
				case RADIAN:
					angle = parameters.get(0).divideAndRemainder(PI.multiply(new BigDecimal(2.0)))[1];
					// angle = parameters.get(0).divide(PI.multiply(new
					// BigDecimal(2.0)));
					// System.out.println("P: " +
					// parameters.get(0).toPlainString() + " angleRAD: " +
					// angle.toPlainString() + "Pi: " +
					// angle.doubleValue()/PI.doubleValue());
					d = Math.sin(angle.doubleValue());
					bd = new BigDecimal(d, mc);
					// bd = bd.setScale(10,RoundingMode.HALF_UP);
					break;
				case GRADIAN:
					break;
				default:
					break;
				}
				return bd;
			}
		});
		addFunction(new Function("CBRT", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.cbrt(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("POW", 2) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.pow(parameters.get(0).doubleValue(), parameters.get(1).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("COS", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal bd = BigDecimal.ZERO, angle = BigDecimal.ZERO;
				double d = 0.0;
				switch (EAngleMode) {
				case DEGREE:
					angle = parameters.get(0).remainder(new BigDecimal(360, mc));
					d = angle.doubleValue();
					// System.out.println("P: " +
					// parameters.get(0).toPlainString() + " angleDEG: " +
					// String.valueOf(angle.doubleValue()) + " Value: " +
					// String.valueOf(d));
					d = Math.cos(Math.toRadians(d));
					bd = new BigDecimal(d, mc);
					bd = bd.setScale(15, RoundingMode.HALF_UP);
					break;
				case RADIAN:
					angle = parameters.get(0).remainder(PI.multiply(new BigDecimal(2.0, mc)));
					d = angle.doubleValue();
					// System.out.println("P: " +
					// parameters.get(0).toPlainString() + " angleRAD: " +
					// String.valueOf(angle.doubleValue()) + " Value: " +
					// String.valueOf(d));
					d = Math.cos(d);
					bd = new BigDecimal(d, mc);
					bd = bd.setScale(15, RoundingMode.HALF_UP);
					break;
				}
				return bd;
			}
		});
		addFunction(new Function("TAN", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal bd = BigDecimal.ZERO, angle = BigDecimal.ZERO;
				double d = 0.0;
				switch (EAngleMode) {
				case DEGREE:
					angle = parameters.get(0).remainder(new BigDecimal(360, mc));
					d = angle.doubleValue();
					if (d == 90.0 || d == 180.0) {
						throw new ExpressionException("NaN ");
					}
					// System.out.println("P: " +
					// parameters.get(0).toPlainString() + " angleDEG: " +
					// String.valueOf(angle.doubleValue()) + " Value: " +
					// String.valueOf(d));
					d = Math.tan(Math.toRadians(d));
					bd = new BigDecimal(d, mc);
					bd = bd.setScale(15, RoundingMode.HALF_UP);
					break;
				case RADIAN:
					angle = parameters.get(0).remainder(PI.multiply(new BigDecimal(2.0, mc)));
					d = angle.doubleValue();
					// System.out.println("P: " +
					// parameters.get(0).toPlainString() + " angleRAD: " +
					// String.valueOf(angle.doubleValue()) + " Value: " +
					// String.valueOf(d));
					d = Math.tan(d);
					if (d > 10000000) {
						throw new ExpressionException("NaN");
					}
					bd = new BigDecimal(d, mc);
					bd = bd.setScale(15, RoundingMode.HALF_UP);
					break;
				}
				return bd;
			}
		});
		addFunction(new Function("ASIN", 1) { // added by av
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal bd = BigDecimal.ZERO;
				double d = 0.0;
				switch (EAngleMode) {
				case DEGREE: {
					d = Math.toDegrees(Math.asin(parameters.get(0).doubleValue()));
					break;
				}
				case RADIAN: {
					d = Math.asin(parameters.get(0).doubleValue());
					break;
				}
				default:
					d = Math.toDegrees(Math.asin(parameters.get(0).doubleValue()));
					break;

				}
				bd = new BigDecimal(d, mc);
				bd = bd.setScale(15, RoundingMode.HALF_UP);
				return new BigDecimal(d, MathContext.DECIMAL64);
			}
		});
		addFunction(new Function("ACOS", 1) { // added by av
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal bd = BigDecimal.ZERO;
				double d = 0.0,parameter;
				parameter = parameters.get(0).doubleValue();
				if (Math.IEEEremainder(parameter, 0.5d) == 0.0d) {
					return new BigDecimal(60);
				}
				switch (EAngleMode) {
				case DEGREE: {
					d = Math.toDegrees(Math.acos(parameter));
					break;
				}
				case RADIAN: {
					d = Math.acos(parameter);
					break;
				}
				default:
					d = Math.toDegrees(Math.acos(parameter));
					break;

				}
				bd = new BigDecimal(d, MathContext.DECIMAL64);
				//bd = bd.setScale(15, RoundingMode.HALF_UP);
				return bd;
				//return new BigDecimal(d, MathContext.DECIMAL64);
			}
		});
		addFunction(new Function("ATAN", 1) { // added by av
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = 0.0;
				switch (EAngleMode) {
				case DEGREE: {
					d = Math.toDegrees(Math.atan(parameters.get(0).doubleValue()));
					break;
				}
				case RADIAN: {
					d = Math.atan(parameters.get(0).doubleValue());
					break;
				}

				}
				return new BigDecimal(d, MathContext.DECIMAL64);
			}
		});
		addFunction(new Function("SINH", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.sinh(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("COSH", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.cosh(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("TANH", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.tanh(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("RAD", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.toRadians(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("DEG", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.toDegrees(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("MAX", -1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				if (parameters.size() == 0) {
					throw new ExpressionException("MAX requires at least one parameter");
				}
				BigDecimal max = null;
				for (BigDecimal parameter : parameters) {
					if (max == null || parameter.compareTo(max) > 0) {
						max = parameter;
					}
				}
				return max;
			}
		});
		addFunction(new Function("MIN", -1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				if (parameters.size() == 0) {
					throw new ExpressionException("MIN requires at least one parameter");
				}
				BigDecimal min = null;
				for (BigDecimal parameter : parameters) {
					if (min == null || parameter.compareTo(min) < 0) {
						min = parameter;
					}
				}
				return min;
			}
		});
		addFunction(new Function("ABS", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				return parameters.get(0).abs(mc);
			}
		});
		addFunction(new Function("LOG", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.log(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("LOG10", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				double d = Math.log10(parameters.get(0).doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("ROUND", 2) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal toRound = parameters.get(0);
				int precision = parameters.get(1).intValue();
				return toRound.setScale(precision, mc.getRoundingMode());
			}
		});
		addFunction(new Function("FLOOR", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal toRound = parameters.get(0);
				return toRound.setScale(0, RoundingMode.FLOOR);
			}
		});
		addFunction(new Function("CEILING", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal toRound = parameters.get(0);
				return toRound.setScale(0, RoundingMode.CEILING);
			}
		});
		addFunction(new Function("SQRT", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				/*
				 * From The Java Programmers Guide To numerical Computing
				 * (Ronald Mak, 2003)
				 */
				BigDecimal x = parameters.get(0);
				if (x.compareTo(BigDecimal.ZERO) == 0) {
					return new BigDecimal(0);
				}
				if (x.signum() < 0) {
					throw new ExpressionException("Argument to SQRT() function must not be negative");
				}
				BigInteger n = x.movePointRight(mc.getPrecision() << 1).toBigInteger();

				int bits = (n.bitLength() + 1) >> 1;
				BigInteger ix = n.shiftRight(bits);
				BigInteger ixPrev;

				do {
					ixPrev = ix;
					ix = ix.add(n.divide(ix)).shiftRight(1);
					// Give other threads a chance to work;
					Thread.yield();
				} while (ix.compareTo(ixPrev) != 0);

				return new BigDecimal(ix, mc.getPrecision());
			}
		});
		addFunction(new Function("FAC", 1) {
			@Override
			public BigDecimal eval(List<BigDecimal> parameters) {
				int Num = parameters.get(0).intValue();
				if (Num < 0) {
					throw new ExpressionException("-ve Number");
				}
				if (Num == 0) {
					return new BigDecimal(1, mc);
				}
				long i;
				BigDecimal Ans = new BigDecimal(1);
				for (i = 1; i <= Num; i++) {
					Ans = Ans.multiply(BigDecimal.valueOf(i));
				}
				return Ans;
			}

		});

		variables.put("e", e);
		variables.put("PI", PI);
		variables.put("TRUE", BigDecimal.ONE);
		variables.put("FALSE", BigDecimal.ZERO);

	}

	/**
	 * Is the string a number?
	 * 
	 * @param st
	 *            The string.
	 * @return <code>true</code>, if the input string is a number.
	 */
	private boolean isNumber(String st) {
		if (st.charAt(0) == minusSign && st.length() == 1)
			return false;
		if (st.charAt(0) == '+' && st.length() == 1)
			return false;
		if (st.charAt(0) == 'e' || st.charAt(0) == 'E')
			return false;
		for (char ch : st.toCharArray()) {
			if (!Character.isDigit(ch) && ch != minusSign && ch != decimalSeparator && ch != 'e' && ch != 'E'
					&& ch != '+')
				return false;
		}
		return true;
	}

	/**
	 * Implementation of the <i>Shunting Yard</i> algorithm to transform an
	 * infix expression to a RPN expression.
	 * 
	 * @param expression
	 *            The input expression in infx.
	 * @return A RPN representation of the expression, with each token as a list
	 *         member.
	 */
	private List<String> shuntingYard(String expression) {
		List<String> outputQueue = new ArrayList<String>();
		Stack<String> stack = new Stack<String>();

		Tokenizer tokenizer = new Tokenizer(expression);

		String lastFunction = null;
		String previousToken = null;
		while (tokenizer.hasNext()) {
			String token = tokenizer.next();
			if (isNumber(token) || token.equals("A") || token.equals("B") || token.equals("C") || token.equals("D")
					|| token.equals("E") || token.equals("F")) {
				outputQueue.add(token);
			} else if (variables.containsKey(token)) {
				outputQueue.add(token);
			} else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
				stack.push(token);
				lastFunction = token;
			} else if (Character.isLetter(token.charAt(0))) {
				stack.push(token);
			} else if (",".equals(token)) {
				if (operators.containsKey(previousToken)) {
					throw new ExpressionException("Missing parameter(s) for operator " + previousToken
							+ " at character position " + (tokenizer.getPos() - 1 - previousToken.length()));
				}
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new ExpressionException("Parse error for function '" + lastFunction + "'");
				}
			} else if (operators.containsKey(token)) {
				if (",".equals(previousToken) || "(".equals(previousToken)) {
					throw new ExpressionException("Missing parameter(s) for operator " + token
							+ " at character position " + (tokenizer.getPos() - token.length()));
				}
				Operator o1 = operators.get(token);
				String token2 = stack.isEmpty() ? null : stack.peek();
				while (token2 != null && operators.containsKey(token2)
						&& ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(token2).getPrecedence())
								|| (o1.getPrecedence() < operators.get(token2).getPrecedence()))) {
					outputQueue.add(stack.pop());
					token2 = stack.isEmpty() ? null : stack.peek();
				}
				stack.push(token);
			} else if ("(".equals(token)) {
				if (previousToken != null) {
					if (isNumber(previousToken)) {
						throw new ExpressionException("Missing operator at character position " + tokenizer.getPos());
					}
					// if the ( is preceded by a valid function, then it
					// denotes the start of a parameter list
					if (functions.containsKey(previousToken.toUpperCase(Locale.ROOT))) {
						outputQueue.add(token);
					}
				}
				stack.push(token);
			} else if (")".equals(token)) {
				if (operators.containsKey(previousToken)) {
					throw new ExpressionException("Missing parameter(s) for operator " + previousToken
							+ " at character position " + (tokenizer.getPos() - 1 - previousToken.length()));
				}
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new ExpressionException("Mismatched parentheses");
				}
				stack.pop();
				if (!stack.isEmpty() && functions.containsKey(stack.peek().toUpperCase(Locale.ROOT))) {
					outputQueue.add(stack.pop());
				}
			}
			previousToken = token;
		}
		while (!stack.isEmpty()) {
			String element = stack.pop();
			if ("(".equals(element) || ")".equals(element)) {
				throw new ExpressionException("Mismatched parentheses");
			}
			if (!operators.containsKey(element)) {
				throw new ExpressionException("Unknown operator or function: " + element);
			}
			outputQueue.add(element);
		}
		return outputQueue;
	}

	// Tayyeb
	private boolean isvalidHexa(String str) {
		boolean ret = false;
		ret = str.matches("[0-9]*[A,B,C,D,E,F]*[0-9]*\\.?[0-9]*[A,B,C,D,E,F]*[0-9]*");
		return ret;
	}

	private List<String> shuntingYard16(String expression) {
		List<String> outputQueue = new ArrayList<String>();
		Stack<String> stack = new Stack<String>();

		Tokenizer tokenizer = new Tokenizer(expression);

		String lastFunction = null;
		String previousToken = null;
		while (tokenizer.hasNext()) {
			String token = tokenizer.next16();
			if (isvalidHexa(token)) {
				outputQueue.add(token);
			}  else if (",".equals(token)) {
				if (operators.containsKey(previousToken)) {
					throw new ExpressionException("Missing parameter(s) for operator " + previousToken
							+ " at character position " + (tokenizer.getPos() - 1 - previousToken.length()));
				}
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new ExpressionException("Parse error for function '" + lastFunction + "'");
				}
			} else if (operators.containsKey(token)) {
				if (",".equals(previousToken) || "(".equals(previousToken)) {
					throw new ExpressionException("Missing parameter(s) for operator " + token
							+ " at character position " + (tokenizer.getPos() - token.length()));
				}
				Operator o1 = operators.get(token);
				String token2 = stack.isEmpty() ? null : stack.peek();
				while (token2 != null && operators.containsKey(token2)
						&& ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(token2).getPrecedence())
								|| (o1.getPrecedence() < operators.get(token2).getPrecedence()))) {
					outputQueue.add(stack.pop());
					token2 = stack.isEmpty() ? null : stack.peek();
				}
				stack.push(token);
			} else if ("(".equals(token)) {
				if (previousToken != null) {
					if (isvalidHexa(previousToken)) {
						throw new ExpressionException("Missing operator at character position " + tokenizer.getPos());
					}
					// if the ( is preceded by a valid function, then it
					// denotes the start of a parameter list
					if (functions.containsKey(previousToken.toUpperCase(Locale.ROOT))) {
						outputQueue.add(token);
					}
				}
				stack.push(token);
			} else if (")".equals(token)) {
				if (operators.containsKey(previousToken)) {
					throw new ExpressionException("Missing parameter(s) for operator " + previousToken
							+ " at character position " + (tokenizer.getPos() - 1 - previousToken.length()));
				}
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new ExpressionException("Mismatched parentheses");
				}
				stack.pop();
				if (!stack.isEmpty() && functions.containsKey(stack.peek().toUpperCase(Locale.ROOT))) {
					outputQueue.add(stack.pop());
				}
			}
			previousToken = token;
		}
		while (!stack.isEmpty()) {
			String element = stack.pop();
			if ("(".equals(element) || ")".equals(element)) {
				throw new ExpressionException("Mismatched parentheses");
			}
			if (!operators.containsKey(element)) {
				throw new ExpressionException("Unknown operator or function: " + element);
			}
			outputQueue.add(element);
		}
		return outputQueue;
	}

	// end - Tayyeb
	/**
	 * Evaluates the expression.
	 * 
	 * @return The result of the expression.
	 */
	public BigDecimal eval() {

		Stack<LazyNumber> stack = new Stack<LazyNumber>();

		for (final String token : getRPN()) {
			if (operators.containsKey(token)) {
				final LazyNumber v1 = stack.pop();
				final LazyNumber v2 = stack.pop();
				LazyNumber number = new LazyNumber() {
					public BigDecimal eval() {
						return operators.get(token).eval(v2.eval(), v1.eval());
					}
				};
				stack.push(number);
			} else if (variables.containsKey(token)) {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return variables.get(token);
					}
				});
			} else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
				LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
				ArrayList<LazyNumber> p = new ArrayList<LazyNumber>(!f.numParamsVaries() ? f.getNumParams() : 0);
				// pop parameters off the stack until we hit the start of
				// this function's parameter list
				while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
					p.add(0, stack.pop());
				}
				if (stack.peek() == PARAMS_START) {
					stack.pop();
				}
				LazyNumber fResult = f.lazyEval(p);
				stack.push(fResult);
			} else if ("(".equals(token)) {
				stack.push(PARAMS_START);
			} else {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return new BigDecimal(token);
					}
				});
			}
		}
		BigDecimal bd = new BigDecimal(0, mc.DECIMAL32);
		bd = stack.pop().eval().stripTrailingZeros();
		return bd;
	}

	public static String decToBin(BigDecimal z, int limit) {
		final BigDecimal base = new BigDecimal(2);
		boolean negative = false;
		String symbols = "";

		if (z.compareTo(BigDecimal.ZERO) == 0) {
			symbols = "0";
		} else {
			if (z.compareTo(BigDecimal.ZERO) < 0) {
				z = z.negate();
				negative = true;
			}
			if (z.compareTo(BigDecimal.ONE) >= 0) {
				symbols = decToBin(z.toBigInteger()) + ".";
			}

			z = z.subtract(new BigDecimal(z.toBigInteger())); // z -= (int) z;
			z = z.multiply(base); // z *= base;
			int r;
			int counter = 0;
			while (z.compareTo(BigDecimal.ZERO) > 0 && counter <= limit) {
				r = z.intValue(); // (int) z;
				symbols += r;
				z = z.subtract(new BigDecimal(z.toBigInteger())); // z -= (int)
																	// z;
				z = z.multiply(base); // z *= base;
				counter++;
			}
		}

		if (negative)
			symbols = "-" + symbols;
		return symbols;
	}

	public static String decToBin(BigInteger n) {
		final BigInteger base = BigInteger.valueOf(2);
		boolean negative = false;
		String symbols = "";
		BigInteger q = n;
		int r;

		if (n.compareTo(ZERO) == 0) {
			symbols = "0";
		} else {
			if (n.compareTo(ZERO) < 0) {
				q = q.negate();
				negative = true;
			}

			while (q.compareTo(ZERO) > 0) {
				r = q.mod(base).intValue();
				symbols = r + symbols;
				q = q.divide(base);
			}
		}
		if (negative)
			symbols = "-" + symbols;
		return symbols;
	}

	public static BigDecimal binToBigDecimal(String bin, MathContext mc) {
		BigDecimal base = BigDecimal.valueOf(2.0);
		boolean negative = false;
		String symbol;
		double a_i;
		BigDecimal x;
		int point; // position of hexadecimal point

		if (bin.substring(0, 1).equals("-")) {
			negative = true;
			bin = bin.substring(1);
		}
		if (bin.substring(bin.length() - 1, bin.length()).equals("-")) {
			negative = true;
			bin = bin.substring(0, bin.length() - 1);
		}

		point = bin.indexOf('.');

		if (point == -1) { // the string represents an integer!
			return new BigDecimal(negative ? binToDec(bin).negate() : binToDec(bin));
		}

		if (point == 0) {
			bin = "0" + bin;
			point = 1;
		}

		x = new BigDecimal(binToDec(bin.substring(0, point)));

		bin = bin.substring(point + 1, bin.length());

		for (int i = 0; i < bin.length(); i++) {
			symbol = bin.substring(i, i + 1);
			a_i = Integer.parseInt(symbol);
			if (a_i > 1) {
				throw new NumberFormatException("No binary number \"" + bin + "\"");
			}
			x = x.add(BigDecimal.valueOf(a_i).divide(base.pow(i + 1, mc), mc));
		}
		return negative ? x.negate() : x;
	}

	public static BigInteger binToDec(String bin) {
		BigInteger base = BigInteger.valueOf(2);
		boolean negative = false;
		BigInteger a_i;
		BigInteger n = ZERO;

		if (bin.substring(0, 1).equals("-")) {
			negative = true;
			bin = bin.substring(1);
		}
		if (bin.substring(bin.length() - 1, bin.length()).equals("-")) {
			negative = true;
			bin = bin.substring(0, bin.length() - 1);
		}

		for (int i = 0; i < bin.length(); i++) {
			a_i = new BigInteger(bin.substring(i, i + 1));
			if (a_i.compareTo(ONE) > 0) {
				throw new NumberFormatException("No binary number");
			}
			n = n.add(a_i.multiply(base.pow(bin.length() - i - 1)));
		}

		if (negative)
			n = n.negate();
		return n;
	}

	public static String decToOct(BigDecimal z, int limit) {
		final BigDecimal base = new BigDecimal(8);
		boolean negative = false;
		String symbols = "";

		if (z.compareTo(BigDecimal.ZERO) == 0) {
			symbols = "0";
		} else {
			if (z.compareTo(BigDecimal.ZERO) < 0) {
				z = z.negate();
				negative = true;
			}
			if (z.compareTo(BigDecimal.ONE) >= 0) {
				symbols = decToOct(z.toBigInteger()) + ".";
			}

			z = z.subtract(new BigDecimal(z.toBigInteger())); // z -= (int) z;
			z = z.multiply(base); // z *= base;
			int r;
			int counter = 0;
			while (z.compareTo(BigDecimal.ZERO) > 0 && counter <= limit) {
				r = z.intValue(); // (int) z;
				symbols += r;
				z = z.subtract(new BigDecimal(z.toBigInteger())); // z -= (int)
																	// z;
				z = z.multiply(base); // z *= base;
				counter++;
			}
		}

		if (negative)
			symbols = "-" + symbols;
		return symbols;
	}

	public static String decToOct(BigInteger n) {
		final BigInteger base = BigInteger.valueOf(8);
		boolean negative = false;
		String symbols = "";
		BigInteger q = n;
		int r;

		if (n.compareTo(ZERO) == 0) {
			symbols = "0";
		} else {
			if (n.compareTo(ZERO) < 0) {
				q = q.negate();
				negative = true;
			}

			while (q.compareTo(ZERO) > 0) {
				r = q.mod(base).intValue();
				symbols = r + symbols;
				q = q.divide(base);
			}
		}
		if (negative)
			symbols = "-" + symbols;
		return symbols;
	}

	public static BigDecimal octToBigDecimal(String oct, MathContext mc) {
		BigDecimal base = BigDecimal.valueOf(8.0);
		boolean negative = false;
		String symbol;
		double a_i;
		BigDecimal x;
		int point; // position of hexadecimal point

		if (oct.substring(0, 1).equals("-")) {
			negative = true;
			oct = oct.substring(1);
		}
		if (oct.substring(oct.length() - 1, oct.length()).equals("-")) {
			negative = true;
			oct = oct.substring(0, oct.length() - 1);
		}

		point = oct.indexOf('.');

		if (point == -1) { // the string represents an integer!
			return new BigDecimal(negative ? octToDec(oct).negate() : octToDec(oct));
		}

		if (point == 0) {
			oct = "0" + oct;
			point = 1;
		}

		x = new BigDecimal(octToDec(oct.substring(0, point)));

		oct = oct.substring(point + 1, oct.length());

		for (int i = 0; i < oct.length(); i++) {
			symbol = oct.substring(i, i + 1);
			a_i = Integer.parseInt(symbol);
			if (a_i > 1) {
				throw new NumberFormatException("No binary number \"" + oct + "\"");
			}
			x = x.add(BigDecimal.valueOf(a_i).divide(base.pow(i + 1, mc), mc));
		}
		return negative ? x.negate() : x;
	}

	public static BigInteger octToDec(String oct) {
		BigInteger base = BigInteger.valueOf(8);
		boolean negative = false;
		BigInteger a_i;
		BigInteger n = ZERO;

		if (oct.substring(0, 1).equals("-")) {
			negative = true;
			oct = oct.substring(1);
		}
		if (oct.substring(oct.length() - 1, oct.length()).equals("-")) {
			negative = true;
			oct = oct.substring(0, oct.length() - 1);
		}

		for (int i = 0; i < oct.length(); i++) {
			a_i = new BigInteger(oct.substring(i, i + 1));
			if (a_i.compareTo(BigInteger.valueOf(7)) > 0) {
				throw new NumberFormatException("No binary number");
			}
			n = n.add(a_i.multiply(base.pow(oct.length() - i - 1)));
		}

		if (negative)
			n = n.negate();
		return n;
	}

	public BigDecimal eval8() {

		Stack<LazyNumber> stack = new Stack<LazyNumber>();

		for (final String token : getRPN()) {
			if (operators.containsKey(token)) {
				// Here convert to mode base
				final LazyNumber v1 = stack.pop();
				final LazyNumber v2 = stack.pop();

				LazyNumber number = new LazyNumber() {
					public BigDecimal eval() {
						final BigDecimal V1 = octToBigDecimal(v1.eval().toPlainString(), mc);
						final BigDecimal V2 = octToBigDecimal(v2.eval().toPlainString(), mc);
						return operators.get(token).eval(V2, V1);
					}
				};
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						BigDecimal bd = new BigDecimal(0);
						String ret = decToOct(number.eval(), 5);
						bd = BigDecimal.valueOf(Double.parseDouble(ret));
						return bd;
					}
				});
			} else if (variables.containsKey(token)) {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return variables.get(token);
					}
				});
			} else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
				LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
				ArrayList<LazyNumber> p = new ArrayList<LazyNumber>(!f.numParamsVaries() ? f.getNumParams() : 0);
				// pop parameters off the stack until we hit the start of
				// this function's parameter list
				while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
					p.add(0, stack.pop());
				}
				if (stack.peek() == PARAMS_START) {
					stack.pop();
				}
				LazyNumber fResult = f.lazyEval(p);
				stack.push(fResult);
			} else if ("(".equals(token)) {
				stack.push(PARAMS_START);
			} else {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return new BigDecimal(token);
					}
				});
			}
		}
		BigDecimal bd = new BigDecimal(0, mc.DECIMAL128);
		bd = stack.pop().eval().stripTrailingZeros();
		return bd;
	}

	public BigDecimal eval2() {

		Stack<LazyNumber> stack = new Stack<LazyNumber>();

		for (final String token : getRPN()) {
			if (operators.containsKey(token)) {
				// Here convert to mode base
				final LazyNumber v1 = stack.pop();
				final LazyNumber v2 = stack.pop();

				LazyNumber number = new LazyNumber() {
					public BigDecimal eval() {
						final BigDecimal V1 = binToBigDecimal(v1.eval().toPlainString(), mc);
						final BigDecimal V2 = binToBigDecimal(v2.eval().toPlainString(), mc);
						return operators.get(token).eval(V2, V1);
					}
				};
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						BigDecimal bd = new BigDecimal(0);
						bd = BigDecimal.valueOf(Double.parseDouble(decToBin(number.eval(), 5)));
						return bd;
					}
				});
			} else if (variables.containsKey(token)) {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return variables.get(token);
					}
				});
			} else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
				LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
				ArrayList<LazyNumber> p = new ArrayList<LazyNumber>(!f.numParamsVaries() ? f.getNumParams() : 0);
				// pop parameters off the stack until we hit the start of
				// this function's parameter list
				while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
					p.add(0, stack.pop());
				}
				if (stack.peek() == PARAMS_START) {
					stack.pop();
				}
				LazyNumber fResult = f.lazyEval(p);
				stack.push(fResult);
			} else if ("(".equals(token)) {
				stack.push(PARAMS_START);
			} else {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return new BigDecimal(token);
					}
				});
			}
		}
		BigDecimal bd = new BigDecimal(0, mc.DECIMAL128);
		bd = stack.pop().eval().stripTrailingZeros();
		return bd;
	}

	public static BigDecimal hexToBigDecimal(String hex, MathContext mc) {
		BigDecimal base = BigDecimal.valueOf(16);
		boolean negative = false;
		String symbol;
		int a_i;
		BigDecimal x;
		int point; // position of hexadecimal point

		if (hex.substring(0, 1).equals("-")) {
			negative = true;
			hex = hex.substring(1);
		}
		if (hex.substring(hex.length() - 1, hex.length()).equals("-")) {
			negative = true;
			hex = hex.substring(0, hex.length() - 1);
		}

		point = hex.indexOf('.');

		if (point == -1) { // the string represents an integer!
			return negative ? new BigDecimal(hexToDec(hex).negate()) : new BigDecimal(hexToDec(hex));
		}

		if (point == 0) {
			hex = "0" + hex;
			point = 1;
		}

		x = new BigDecimal(hexToDec(hex.substring(0, point)));

		// System.out.println("x="+x);

		hex = hex.substring(point + 1, hex.length());

		for (int i = 0; i < hex.length(); i++) {
			symbol = hex.substring(i, i + 1);
			if (symbol.equalsIgnoreCase("A")) {
				a_i = 10;
			} else if (symbol.equalsIgnoreCase("B")) {
				a_i = 11;
			} else if (symbol.equalsIgnoreCase("C")) {
				a_i = 12;
			} else if (symbol.equalsIgnoreCase("D")) {
				a_i = 13;
			} else if (symbol.equalsIgnoreCase("E")) {
				a_i = 14;
			} else if (symbol.equalsIgnoreCase("F")) {
				a_i = 15;
			} else {
				a_i = Integer.parseInt(symbol);
			}
			x = x.add(BigDecimal.valueOf(a_i).divide(base.pow(i + 1, mc), mc));
		}
		return negative ? x.negate() : x;
	}

	public static BigInteger hexToDec(String hex) {
		BigInteger base = BigInteger.valueOf(16);
		boolean negative = false;
		String symbol;
		int a_i;
		BigInteger n = ZERO;

		if (hex.substring(0, 1).equals("-")) {
			negative = true;
			hex = hex.substring(1);
		}
		if (hex.substring(hex.length() - 1, hex.length()).equals("-")) {
			negative = true;
			hex = hex.substring(0, hex.length() - 1);
		}

		for (int i = 0; i < hex.length(); i++) {
			symbol = hex.substring(i, i + 1);
			if (symbol.equalsIgnoreCase("A")) {
				a_i = 10;
			} else if (symbol.equalsIgnoreCase("B")) {
				a_i = 11;
			} else if (symbol.equalsIgnoreCase("C")) {
				a_i = 12;
			} else if (symbol.equalsIgnoreCase("D")) {
				a_i = 13;
			} else if (symbol.equalsIgnoreCase("E")) {
				a_i = 14;
			} else if (symbol.equalsIgnoreCase("F")) {
				a_i = 15;
			} else {
				a_i = Integer.parseInt(symbol);
			}
			// if (a_i > 2) {
			// throw new NumberFormatException ("No hexadecimal number
			// \""+hex+"\"");
			// }
			n = n.add(BigInteger.valueOf(a_i).multiply(base.pow(hex.length() - i - 1)));
		}

		if (negative)
			n = n.negate();
		return n;
	}

	public static String decToHex(BigDecimal z, int limit) {
		final BigDecimal base = new BigDecimal(16.);
		boolean negative = false;
		String symbols = "";

		if (z.compareTo(BigDecimal.ZERO) == 0) {
			symbols = "0";
		} else {
			if (z.compareTo(BigDecimal.ZERO) < 0) {
				z = z.negate();
				negative = true;
			}
			if (z.compareTo(BigDecimal.ONE) >= 0) {
				symbols = decToHex(z.toBigInteger()) + ".";
			}

			z = z.subtract(new BigDecimal(z.toBigInteger())); // z -= (int) z;
			z = z.multiply(base); // z *= base;
			int r;
			int counter = 0;
			while (z.compareTo(BigDecimal.ZERO) > 0 && counter <= limit) {
				r = z.intValue(); // (int) z;
				if (r <= 9) {
					symbols += r;
				} else if (r == 10) {
					symbols += "A";
				} else if (r == 11) {
					symbols += "B";
				} else if (r == 12) {
					symbols += "C";
				} else if (r == 13) {
					symbols += "D";
				} else if (r == 14) {
					symbols += "E";
				} else if (r == 15) {
					symbols += "F";
				}
				z = z.subtract(new BigDecimal(z.toBigInteger())); // z -= (int)
																	// z;
				z = z.multiply(base); // z *= base;
				counter++;
			}
		}

		if (negative)
			symbols = "-" + symbols;
		return symbols;
	}

	public static String decToHex(BigInteger n) {
		final BigInteger base = new BigInteger("16");
		boolean negative = false;
		String symbols = "";
		BigInteger q = n;
		int r;

		if (n.compareTo(ZERO) == 0) {
			symbols = "0";
		} else {
			if (n.compareTo(ZERO) < 0) {
				q = q.negate();
				negative = true;
			}

			while (q.compareTo(ZERO) > 0) {
				r = q.mod(base).intValue();
				if (r <= 9) {
					symbols = r + symbols;
				} else if (r == 10) {
					symbols = "A" + symbols;
				} else if (r == 11) {
					symbols = "B" + symbols;
				} else if (r == 12) {
					symbols = "C" + symbols;
				} else if (r == 13) {
					symbols = "D" + symbols;
				} else if (r == 14) {
					symbols = "E" + symbols;
				} else if (r == 15) {
					symbols = "F" + symbols;
				}
				q = q.divide(base); // q /= base;
			}
		}
		if (negative)
			symbols = "-" + symbols;
		return symbols;
	}

	public String eval16() {
		String finalOp = "";
		Stack<String> stack = new Stack<String>();

		for (final String token : getRPN16()) {
			if (operators.containsKey(token)) {
				// Here convert to mode base
				final String v1 = stack.pop();
				final String v2 = stack.pop();
				final BigDecimal V1 = hexToBigDecimal(v1, mc);
				final BigDecimal V2 = hexToBigDecimal(v2, mc);
				stack.push(eval_hexa(token, V2, V1));
			}  else {
				stack.push(token);
			}
			finalOp = token;
		}
		BigDecimal bd = new BigDecimal(0, mc.DECIMAL128);
		final String val = stack.pop();
		Pattern pattern = Pattern.compile(
				"(-?[0-9]*[A B C D E F]*[0-9]*\\.[0-9]*[A B C D E F]*[0-9]*)[+ - \\* /](-?[0-9]*[A B C D E F]*[0-9]*\\.[0-9]*[A B C D E F]*[0-9]*)");
		Matcher matcher = pattern.matcher(val);
		try {
			if (matcher.find()) {
				final BigDecimal fV1 = hexToBigDecimal(matcher.group(1), mc);
				final BigDecimal fV2 = hexToBigDecimal(matcher.group(2), mc);
				return eval_hexa(finalOp, fV2, fV1);
			} else {
				return val;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return null;
	}

	public String eval_hexa(String token, BigDecimal arg1, BigDecimal arg2) {
		return decToHex(operators.get(token).eval(arg1, arg2), 5);
	}

	/**
	 * Sets the precision for expression evaluation.
	 * 
	 * @param precision
	 *            The new precision.
	 * 
	 * @return The expression, allows to chain methods.
	 */
	public Expression setPrecision(int precision) {
		this.mc = new MathContext(precision);
		return this;
	}

	/**
	 * Sets the rounding mode for expression evaluation.
	 * 
	 * @param roundingMode
	 *            The new rounding mode.
	 * @return The expression, allows to chain methods.
	 */
	public Expression setRoundingMode(RoundingMode roundingMode) {
		this.mc = new MathContext(mc.getPrecision(), roundingMode);
		return this;
	}

	/**
	 * Adds an operator to the list of supported operators.
	 * 
	 * @param operator
	 *            The operator to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 *         there was none.
	 */
	public Operator addOperator(Operator operator) {
		return operators.put(operator.getOper(), operator);
	}

	/**
	 * Adds a function to the list of supported functions
	 * 
	 * @param function
	 *            The function to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 *         there was none.
	 */
	public Function addFunction(Function function) {
		return (Function) functions.put(function.getName(), function);
	}

	/**
	 * Adds a lazy function function to the list of supported functions
	 *
	 * @param function
	 *            The function to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 *         there was none.
	 */
	public LazyFunction addLazyFunction(LazyFunction function) {
		return functions.put(function.getName(), function);
	}

	/**
	 * Sets a variable value.
	 * 
	 * @param variable
	 *            The variable name.
	 * @param value
	 *            The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public Expression setVariable(String variable, BigDecimal value) {
		variables.put(variable, value);
		return this;
	}

	/**
	 * Sets a variable value.
	 * 
	 * @param variable
	 *            The variable to set.
	 * @param value
	 *            The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public Expression setVariable(String variable, String value) {
		if (isNumber(value))
			variables.put(variable, new BigDecimal(value));
		else {
			expression = expression.replaceAll("(?i)\\b" + variable + "\\b", "(" + value + ")");
			rpn = null;
		}
		return this;
	}

	/**
	 * Sets a variable value.
	 * 
	 * @param variable
	 *            The variable to set.
	 * @param value
	 *            The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public Expression with(String variable, BigDecimal value) {
		return setVariable(variable, value);
	}

	/**
	 * Sets a variable value.
	 * 
	 * @param variable
	 *            The variable to set.
	 * @param value
	 *            The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public Expression and(String variable, String value) {
		return setVariable(variable, value);
	}

	/**
	 * Sets a variable value.
	 * 
	 * @param variable
	 *            The variable to set.
	 * @param value
	 *            The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public Expression and(String variable, BigDecimal value) {
		return setVariable(variable, value);
	}

	/**
	 * Sets a variable value.
	 * 
	 * @param variable
	 *            The variable to set.
	 * @param value
	 *            The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public Expression with(String variable, String value) {
		return setVariable(variable, value);
	}

	/**
	 * Get an iterator for this expression, allows iterating over an expression
	 * token by token.
	 * 
	 * @return A new iterator instance for this expression.
	 */
	public Iterator<String> getExpressionTokenizer() {
		return new Tokenizer(this.expression);
	}

	/**
	 * Cached access to the RPN notation of this expression, ensures only one
	 * calculation of the RPN per expression instance. If no cached instance
	 * exists, a new one will be created and put to the cache.
	 * 
	 * @return The cached RPN instance.
	 */
	private List<String> getRPN() {
		// if (rpn == null) {
		rpn = shuntingYard(this.expression);
		validate(rpn);
		// }
		return rpn;
	}

	// Tayyeb
	private List<String> getRPN16() {
		// if (rpn == null) {
		rpn = shuntingYard16(this.expression);
		//validate16(rpn);
		// }
		return rpn;
	}

	// end-Tayyeb
	/**
	 * Check that the expression has enough numbers and variables to fit the
	 * requirements of the operators and functions, also check for only 1 result
	 * stored at the end of the evaluation.
	 */
	private void validate(List<String> rpn) {
		/*-
		* Thanks to Norman Ramsey:
		* http://http://stackoverflow.com/questions/789847/postfix-notation-validation
		*/
		// each push on to this stack is a new function scope, with the value of
		// each
		// layer on the stack being the count of the number of parameters in
		// that scope
		Stack<Integer> stack = new Stack<Integer>();

		// push the 'global' scope
		stack.push(0);

		for (final String token : rpn) {
			if (operators.containsKey(token)) {
				if (stack.peek() < 2) {
					throw new ExpressionException("Missing parameter(s) for operator " + token);
				}
				// pop the operator's 2 parameters and add the result
				stack.set(stack.size() - 1, stack.peek() - 2 + 1);
			} else if (variables.containsKey(token)) {
				stack.set(stack.size() - 1, stack.peek() + 1);
			} else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
				LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
				int numParams = stack.pop();
				if (!f.numParamsVaries() && numParams != f.getNumParams()) {
					throw new ExpressionException(
							"Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
				}
				if (stack.size() <= 0) {
					throw new ExpressionException("Too many function calls, maximum scope exceeded");
				}
				// push the result of the function
				stack.set(stack.size() - 1, stack.peek() + 1);
			} else if ("(".equals(token)) {
				stack.push(0);
			} else {
				stack.set(stack.size() - 1, stack.peek() + 1);
			}
		}

		if (stack.size() > 1) {
			throw new ExpressionException("Too many unhandled function parameter lists");
		} else if (stack.peek() > 1) {
			throw new ExpressionException("Too many numbers or variables");
		} else if (stack.peek() < 1) {
			throw new ExpressionException("Empty expression");
		}
	}

	private void validate16(List<String> rpn) {
		/*-
		* Thanks to Norman Ramsey:
		* http://http://stackoverflow.com/questions/789847/postfix-notation-validation
		*/
		// each push on to this stack is a new function scope, with the value of
		// each
		// layer on the stack being the count of the number of parameters in
		// that scope
		Stack<Integer> stack = new Stack<Integer>();

		// push the 'global' scope
		stack.push(0);

		for (final String token : rpn) {
			if (operators.containsKey(token)) {
				if (stack.peek() < 2) {
					throw new ExpressionException("Missing parameter(s) for operator " + token);
				}
				// pop the operator's 2 parameters and add the result
				stack.set(stack.size() - 1, stack.peek() - 2 + 1);
			} else if ("(".equals(token)) {
				stack.push(0);
			} else {
				stack.set(stack.size() - 1, stack.peek() + 1);
			}
		}

		if (stack.size() > 1) {
			throw new ExpressionException("Too many unhandled function parameter lists");
		} else if (stack.peek() > 1) {
			throw new ExpressionException("Too many numbers or variables");
		} else if (stack.peek() < 1) {
			throw new ExpressionException("Empty expression");
		}
	}
	/**
	 * Get a string representation of the RPN (Reverse Polish Notation) for this
	 * expression.
	 * 
	 * @return A string with the RPN representation for this expression.
	 */
	public String toRPN() {
		StringBuilder result = new StringBuilder();
		for (String st : getRPN()) {
			if (result.length() != 0)
				result.append(" ");
			result.append(st);
		}
		return result.toString();
	}

	/**
	 * Exposing declared variables in the expression.
	 * 
	 * @return All declared variables.
	 */
	public Set<String> getDeclaredVariables() {
		return Collections.unmodifiableSet(variables.keySet());
	}

	/**
	 * Exposing declared operators in the expression.
	 * 
	 * @return All declared operators.
	 */
	public Set<String> getDeclaredOperators() {
		return Collections.unmodifiableSet(operators.keySet());
	}

	/**
	 * Exposing declared functions.
	 * 
	 * @return All declared functions.
	 */
	public Set<String> getDeclaredFunctions() {
		return Collections.unmodifiableSet(functions.keySet());
	}

	/**
	 * @return The original expression string
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Returns a list of the variables in the expression.
	 * 
	 * @return A list of the variable names in this expression.
	 */
	public List<String> getUsedVariables() {
		List<String> result = new ArrayList<String>();
		Tokenizer tokenizer = new Tokenizer(expression);
		while (tokenizer.hasNext()) {
			String token = tokenizer.next();
			if (functions.containsKey(token) || operators.containsKey(token) || token.equals("(") || token.equals(")")
					|| token.equals(",") || isNumber(token) || token.equals("PI") || token.equals("e")
					|| token.equals("TRUE") || token.equals("FALSE")) {
				continue;
			}
			result.add(token);
		}
		return result;
	}

	/**
	 * The original expression used to construct this expression, without
	 * variables substituted.
	 */
	public String getOriginalExpression() {
		return this.originalExpression;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Expression that = (Expression) o;
		if (this.expression == null) {
			return that.expression == null;
		} else {
			return this.expression.equals(that.expression);
		}
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return this.expression == null ? 0 : this.expression.hashCode();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return this.expression;
	}

	public String myFunc(boolean Reduction) {
		String output = "", dummy;
		int count = 0;
		BigDecimal bd, result;

		try {
			Pattern pattern = Pattern.compile("[cos sin tan]\\(([^\\)]*)\\)");
			Matcher matcher = pattern.matcher(expression);
			output = this.getExpression();
			dummy = output;
			while (matcher.find()) {
				this.setExpression(matcher.group(1));
				try {
					bd = eval();
					if (Reduction) {
						result = PI.multiply(BigDecimal.valueOf(2.0));
						while (bd.compareTo(result) == 1) {
							bd = bd.subtract(result);
						}
						// System.out.println("Original Expression: " +
						// matcher.group(1) + "Evaluation: " +
						// bd.toPlainString());
						dummy = Pattern.compile("[cos sin tan]\\(([^\\)]*)\\)").matcher(dummy)
								.replaceFirst(bd.toPlainString());
						// System.out.println(dummy);

						// this.setExpression(matcher.toString());
						// System.out.println("New Expression: " + expression);
					} else {
						System.out.println(
								"Original Expression: " + matcher.group(1) + "Evaluation: " + bd.toPlainString());
					}
				} catch (Exception ex) {
					System.out.println("Exc1: " + ex.getMessage());
				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		this.setExpression(output);
		return output;
	}

	private BigDecimal Evalute(String expStr) {
		Stack<LazyNumber> stack = new Stack<LazyNumber>();

		for (final String token : getRPN()) {
			if (operators.containsKey(token)) {
				final LazyNumber v1 = stack.pop();
				final LazyNumber v2 = stack.pop();
				LazyNumber number = new LazyNumber() {
					public BigDecimal eval() {
						return operators.get(token).eval(v2.eval(), v1.eval());
					}
				};
				stack.push(number);
			} else if (variables.containsKey(token)) {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return variables.get(token).round(mc);
					}
				});
			} else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
				LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
				ArrayList<LazyNumber> p = new ArrayList<LazyNumber>(!f.numParamsVaries() ? f.getNumParams() : 0);
				// pop parameters off the stack until we hit the start of
				// this function's parameter list
				while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
					p.add(0, stack.pop());
				}
				if (stack.peek() == PARAMS_START) {
					stack.pop();
				}
				LazyNumber fResult = f.lazyEval(p);
				stack.push(fResult);
			} else if ("(".equals(token)) {
				stack.push(PARAMS_START);
			} else {
				stack.push(new LazyNumber() {
					public BigDecimal eval() {
						return new BigDecimal(token, mc);
					}
				});
			}
		}
		return stack.pop().eval().stripTrailingZeros();
	}

	
	 public static void main(String[] args) { Expression e= new Expression();
	  e.setExpression("-101 + 11");
	  System.out.println(e.eval2().toPlainString()); 
	  e.setExpression("20+3");
	  System.out.println(e.eval().toPlainString());
	  e.setExpression("6B - 3E"); 
	  System.out.println(e.eval16());
	  e.setExpression("65 + 45");
	  System.out.println(e.eval8().toPlainString());
	  
	 }
	 
}
