package util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Stack;

import algebra.Matrix;
import algebra.Polynomial;
import numbers.*;

//This class will use to convert an expression into Latex display format.
public class ConvertDisplay {
	public String screenInput = "", evaluateInput = "", latexOutput;
	public int CursorPos = 0, backCur = 0;
	static boolean Adjusted = false, bracketFun = false, endBracket = true, powerFlag = false;
	static Expression E = new Expression();
	static String errorMsg = null;
	static String Algebraout;
	static String Coefficient = "", Power = "";
	Stack<String> stack = new Stack<String>();
	public int Mrows = 0, Mcols = 0;
	int AnsPos = 0,FacStart=0;
	boolean FractionFound, TrignoFunction = false;
	Polynomial P;
	Matrix M;
	public BigDecimal Ans;

	public String[] Btns;
	public AngleMode cDAngleMode;
	public void setE(Expression ex){
		E = ex;
	}
	public Matrix getM() {
		return M;
	}

	public void setM(Matrix m) {
		M = m;
	}

	public Polynomial getP() {
		return P;
	}

	public void setP(Polynomial p) {
		P = p;
	}

	RationalNum R;

	public RationalNum getR() {
		return R;
	}

	public void setR(RationalNum r) {
		R = r;
	}

	long p;

	public ConvertDisplay() {
		super();
	}

	public enum Mode {
		ALGEBRA(1), MATRIX(2), RATIONAL(3), NORMAL(4);
		private int value;

		private Mode(int Value) {
			this.value = Value;
		}
	};

	public Mode DisplayMode;

	private String adjustMinus(String Input) {
		Input = Input.trim();
		int i, pos = 0;
		boolean operatorFound;
		String Output = Input;
		operatorFound = false;
		char c;
		for (i = 0; i <= Input.length() - 1; i++) {
			c = Input.charAt(i);
			if (operatorFound && c == '-') {
				operatorFound = false;
				Adjusted = true;
				CursorPos = i + 2;
				Output = Input.substring(0, pos + 1) + "(" + "- )" + Input.substring(pos + 2);

			}
			if (i > 1) {
				if (Character.isDigit(c) && Input.charAt(i - 1) == ')') {
					// Enter number within ()
					CursorPos--;
					Output = insertString(Input.substring(0, CursorPos - 1), String.valueOf(c), CursorPos - 1) + ")";
				}
			}

			if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^') {
				operatorFound = true;
				pos = i;
			} else {
				operatorFound = false;
			}
		}
		return Output;
	}

	private String insertString(String Main, String Rem, int CurPos) {
		String Output;
		if (Main.length() > 0) {
			Output = Main.substring(0, CurPos) + Rem + Main.substring(CurPos, Main.length());
		} else {
			Output = Rem;
		}
		return Output;

	}

	private void charRun(String s) {
		Character ch;
		int Len = s.length();

		if (FractionFound) {
			FractionFound = false;
			R = new RationalNum(p, Long.valueOf(s));
		}
		if (s.equals("FRAC")) {
			p = Long.valueOf(stack.pop().toString());
			stack.push(String.valueOf(p));
			FractionFound = true;
		}
		if (!s.equals("BACK")) {
			stack.add(s);
		}
		if (Len != 1) {

			if (s.equals("BACK")) {
				stack.pop();
				String[] Btn;
				Btn = stack.toArray(new String[stack.size()]);
				stack.removeAllElements();
				E = giveExpression(Btn);
			} else {
				if (s.equals("NEXT")) {
					stack.pop();
					if (CursorPos < screenInput.length() - 1) {
						if (screenInput.charAt(CursorPos) == ')' && screenInput.charAt(CursorPos + 1) == ')') {
							CursorPos++;
						} else {
							CursorPos = screenInput.length();
						}
					} else {
						CursorPos = screenInput.length();
					}
				} else {
					if (s.equals("e") || s.equals("pi") || s.equals("Ans")) {
						screenInput = insertString(screenInput, s, CursorPos);
						CursorPos = CursorPos + s.length();
					} else {
						if (s.equals("FAC"))
						{
							screenInput = insertString(screenInput, s, CursorPos);
							evaluateInput=screenInput.replaceAll("([+ - * /])?(\\d+)FAC", "$1FAC($2)");
							screenInput=evaluateInput.replaceAll("([+ - * /])?(Ans)FAC", "$1FAC($2)");
							evaluateInput = screenInput;
							CursorPos = CursorPos +s.length()+2;
						}
						else {
						screenInput = insertString(screenInput, s + "()", CursorPos);
						CursorPos = CursorPos + s.length() + 1;
						}
						

					}
				}
			}
		} else // When input is only 1 Character
		{
			backCur = CursorPos;

			if (s.equals("-")) {
				screenInput = insertString(screenInput, s, CursorPos);
				screenInput = adjustMinus(screenInput);
			} else {

				if (screenInput.length() == 0) {
					screenInput = s;
				} else {
					screenInput = insertString(screenInput, s, CursorPos);
				}

			}
			// if (screenInput.charAt(CursorPos) == ')') {CursorPos++;}
			CursorPos = CursorPos + 1;
		}
		
		evaluateInput = screenInput.replaceAll("null", "");
		screenInput=evaluateInput;

	}

	public void giveLatex() {
		if (evaluateInput.length() > 0) {
			if (DisplayMode == Mode.NORMAL) {
				latexOutput = evaluateInput;
				latexOutput = latexOutput.replaceAll("FAC\\(([0-9]*)(\\\\Box)?\\)", "$1!$2");
				latexOutput = latexOutput.replaceAll("FAC\\(Ans\\)", "Ans!");
				latexOutput = latexOutput.replace("/", "\\div");
				latexOutput = latexOutput.replaceAll("(.*)\\\\div([a-zA-Z])", "$1\\\\div $2");
				latexOutput = latexOutput.replace("*", "\\times");
				latexOutput = latexOutput.replaceAll("(.*)\\\\times([a-zA-Z])", "$1\\\\times $2");
				latexOutput = latexOutput.replaceAll("sqrt\\((.*)\\)", "\\\\sqrt{$1}");
				latexOutput = latexOutput.replace("(", "{(");
				// latexOutput = latexOutput.replaceAll("sqrt\\{([^)]*)\\)",
				// "sqrt\\{$1\\}");
				latexOutput = latexOutput.replace("pi", "\\pi");
				latexOutput = latexOutput.replace(")", ")}");
				latexOutput = latexOutput.replace("cbrt", "\\sqrt[3]");
				latexOutput = latexOutput.replaceAll("\\\\([0-9])", "$1");
				latexOutput = latexOutput.replaceAll("\\^(-?[0-9]*)", "\\^\\{$1\\}");
				latexOutput = latexOutput.replaceAll("\\{\\}", "");
				latexOutput = latexOutput.replace("asin", "sin^{-1}");
				latexOutput = latexOutput.replace("acos", "cos^{-1}");
				latexOutput = latexOutput.replace("atan", "tan^{-1}");
				latexOutput = latexOutput.replace("log", "ln");
				latexOutput = latexOutput.replace("ln10", "log");
				latexOutput = latexOutput.replace("\\\\", "\\");
				latexOutput = latexOutput.replaceAll("null", "");
				return;
			}
		}
		if (DisplayMode == Mode.RATIONAL) {
			if (R.wn.intValue() == 0) {
				if (R.q.intValue() == 1) {
					latexOutput = R.p.toString();
					return;
				} else {
					if (R.p.intValue() < 0) {
						latexOutput = "-\\frac{" + R.p.abs().toString() + "}{" + R.q.toString() + "}";
					} else {
						latexOutput = "\\frac{" + R.p.toString() + "}{" + R.q.toString() + "}";
					}

					return;
				}
			} else {
				if (R.q.intValue() == 1 && R.p.intValue() != 0) {
					latexOutput = R.wn.toString() + "\\frac{" + R.p.toString() + "}";
					return;
				} else {
					latexOutput = R.wn.toString() + "\\frac{" + R.p.toString() + "}{" + R.q.toString() + "}";
					return;
				}

			}
		}
		if (DisplayMode == Mode.ALGEBRA) {
			latexOutput = P.toString();
			latexOutput = latexOutput.replace("(", "");
			latexOutput = latexOutput.replace(")", "");
			latexOutput = latexOutput.replaceAll("\\^(-?[0-9]*)", "\\^\\{$1\\}");
		}
		if (DisplayMode == Mode.MATRIX) {
			// DecimalFormat df = new DecimalFormat("#.####");
			// df.setRoundingMode(RoundingMode.CEILING);
			String start = "\\begin{bmatrix}", end = "end{bmatrix}", Rowsep = "\\\\", row = "", out = "";
			int r = 0, c = 0;
			latexOutput = start;
			Mrows = M.rows;
			Mcols = M.columns;
			for (r = 1; r <= Mrows; r++) {
				for (c = 1; c <= Mcols; c++) {
					BigDecimal bd = new BigDecimal(M.getValue(r, c)).setScale(2, RoundingMode.HALF_UP);
					// System.out.println(bd.toString());
					if (bd.toString().substring(bd.toString().length() - 2).equals("00")) {
						out = bd.toString().substring(0, bd.toString().length() - 3);
					} else {
						out = bd.toString();
					}

					row = row + out + "&";
				}
				row = row.substring(0, row.length() - 1);
				latexOutput = latexOutput + row + Rowsep;
				row = "";
			}
			latexOutput = latexOutput.substring(0, latexOutput.length() - 1) + end;
			return;
		}
	}

	public Expression giveExpression(String[] Buttons) {
		Btns = Buttons;
		CursorPos = 0;
		screenInput = "";
		evaluateInput = "";
		Expression E = new Expression();
		int i;
		for (i = 0; i < Buttons.length; i++) {
			charRun(Buttons[i].trim());
		}

		if (evaluateInput.contains("Ans")) {
			screenInput = evaluateInput;
			evaluateInput = evaluateInput.replace("Ans", this.Ans.toPlainString());
			//AnsPos = screenInput.indexOf("Ans",AnsPos+1);
			//CursorPos = AnsPos+3;
		}
		evaluateInput = evaluateInput.replace(" ", "");
		E.setExpression(evaluateInput);
		evaluateInput = screenInput;
		System.out.println("TESTING lenght: " + evaluateInput.length() + " Cursor Pos: " + CursorPos);
		if (evaluateInput.length() > CursorPos) {
			evaluateInput = evaluateInput.substring(0, CursorPos) + "\\Box" + evaluateInput.substring(CursorPos);

		} else {
//		if (evaluateInput.matches("(.*\\([\\d,Ans]*)([\\)]+$)")) {
//			String abc;
//			abc= evaluateInput.replaceAll("(.*\\([\\d,Ans]*)([\\)]+$)", "$1\\\\Box$2");
//			evaluateInput=abc;
//		}
//		else
//			{
			evaluateInput = evaluateInput + "\\Box";
			}

//		}
		
		// giveLatex();
		return E;
	}

	public Polynomial giveAlgebra(String[] btn) {
		String s, exp, c, x = "", csign = "", expsign = "", output = "";
		int i;
		int exponent;
		Double coefficient;
		String ch;
		char dgt;
		boolean expFlag = false, TermFoundFlag = false;
		c = "";
		x = "";
		exp = "";
		P = new Polynomial();
		for (i = 0; i <= btn.length - 1; i++) {
			ch = btn[i];
			if (ch == "+" || ch == "-") {
				if (expFlag && exp != "") {
					// makeTerm
					TermFoundFlag = true;
					output = output + csign + c + x + expsign + exp;
					exponent = Integer.parseInt(expsign + exp);

					coefficient = Double.parseDouble(csign + c);
					P.put(exponent, coefficient);
					csign = ch;
					c = "";
					expsign = "";
					exp = "";
					x = "";
					expFlag = false;

				} else if (expFlag && exp == "") {
					expsign = ch;
				} else {
					csign = ch;
				}
			} else {
				if (ch == "x^" || ch == "x") {
					if (c == "") {
						c = "1";
					}
					if (ch == "x") {
						expFlag = true;
						x = "x";
						exp = "1";

					} else {
						x = "x^";
						exp = "";
						expFlag = true;
					}
				} else {
					dgt = ch.charAt(0);
					if (Character.isDigit(dgt)) {
						if (expFlag) {
							exp = exp + dgt;
						} else {
							c = c + dgt;
						}
					}
				}
			}
			// output=csign+c+x+expsign+exp;
			// exponent=Integer.parseInt(expsign+exp);
			// coefficient = Double.valueOf(csign+c);
			// P.put(exponent, coefficient);
		}
		exponent = 0;
		coefficient = 0.0;
		if (c == "") {
			coefficient = 0.0;
		}
		if (exp == "") {
			exponent = 0;
		}
		if (c != "") {
			coefficient = Double.valueOf(csign + c);
		}
		if (exp != "") {
			exponent = Integer.parseInt(expsign + exp);
		}
		P.put(exponent, coefficient);
		return P;
	}

	public RationalNum giveRational(String[] btn) {
		int i;
		String num = "", den = "", wn = "";
		BigInteger p, q, w;
		boolean DenFlag = false, Minus = false;
		RationalNum r = new RationalNum();
		char digit;
		for (i = 0; i <= btn.length - 1; i++) {
			if (btn[i].length() < 2) {
				if (btn[i].equals("-")) {
					Minus = !Minus;
					if (DenFlag) {
						den = "-";
					} else {
						num = "-";
					}
				} else {
					digit = btn[i].charAt(0);
					if (Character.isDigit(digit)) {
						if (DenFlag) {
							den = den + digit;
						} else {
							num = num + digit;
						}
					}
				}

			} else {
				if (btn[i].equals("bata")) {
					DenFlag = true;
				}
				if (btn[i].equals("whole")) {
					r.wn = BigInteger.valueOf(Long.valueOf(num)).abs();
					num = "";
				}
			}
		}
		if (r.wn.intValue() != 0) {
			if (num == "") {
				r.p = BigInteger.ZERO;
				p = BigInteger.ZERO;
				r.q = BigInteger.ONE;
				q = BigInteger.ONE;
			} else {
				if (den == "") {
					r.q = BigInteger.ONE;
					q = BigInteger.ONE;
					r.p = BigInteger.valueOf(Long.valueOf(num)).abs();
					p = r.p;
				} else {
					r.p = BigInteger.valueOf(Long.valueOf(num)).abs();
					r.q = BigInteger.valueOf(Long.valueOf(den)).abs();
					r.p = r.q.multiply(r.wn).add(r.p);
					r.q = r.q;
					r.wn = BigInteger.ZERO;
				}
			}
			w = r.wn.abs();
			// r.p=w.multiply(q).add(p);
			if (Minus) {
				r.wn = r.wn.multiply(BigInteger.valueOf(Long.parseLong("-1")));
			}
			// r.wn=BigInteger.ZERO;
		} else {
			if (num == "") {
				r.p = BigInteger.ZERO;
				r.q = BigInteger.ONE;
			} else {
				if (den == "") {
					r.q = BigInteger.ONE;
					r.p = BigInteger.valueOf(Long.valueOf(num)).abs();
				} else {
					r.p = BigInteger.valueOf(Long.valueOf(num)).abs();
					r.q = BigInteger.valueOf(Long.valueOf(den)).abs();
				}
			}
			if (Minus) {
				r.p = r.p.multiply(BigInteger.valueOf(Long.parseLong("-1")));
			}
			r.wn = BigInteger.ZERO;
		}
		return r;
	}

	public RationalNum solveRational(RationalNum R1, String Op, RationalNum R2) {
		RationalNum R = new RationalNum();
		switch (Op) {
		case "+":
			R = R1.add(R2);
			break;
		case "-":
			R = R1.subtract(R2);
			break;
		case "*":
			R = R1.multiply(R2);
			break;
		case "/":
			R = R1.divide(R2);
			break;
		}
		return R;
	}

	public clsAngle Angle = new clsAngle(0);

	public static class clsAngle {

		clsAngle(double val) {
			value = val;
		}

		public double value;
		public int degress;
		public int minutes;
		public double seconds;

		public String giveDtoDSM() {
			int d = (int) value; // Truncate the decimals
			double t1 = (value - d) * 60;
			int m = (int) t1;
			double s1 = (t1 - m) * 60;

			int s = (int) s1;
			degress = d;
			minutes = m;
			seconds = s1;
			return String.valueOf(d) + "\\jlatexmathring" + String.valueOf(m) + "\\textapos"
					+ String.format(Locale.ROOT, "%.5g", s1) + "\\thickspace\\textapos\\textapos";
		}

		public double D2R() {
			double ans;
			ans = Math.toRadians(value);
			value = ans;
			return value;
		}

		public double R2D() {
			double ans;
			ans = Math.toDegrees(value);
			ans = Double.parseDouble(String.format(Locale.ROOT, "%.7g", ans));
			value = ans;
			return value;
		}
	}

	private int lastIntegerPosition(String[] Buttons) {
		int pos = 0;
		Character c = null;
		boolean Found = false;
		for (int j = FacStart; j < Buttons.length; j++) {
			if (Buttons[j].equals("FAC")) {
				FacStart = ++j;
				if (Found) {
					return j;
				} else {
					return 0;
				}
			}
			if (!Buttons[j].equals("FAC")) {
				c = Buttons[j].charAt(0);
				if (c == '+' || c == '-' || c == '*' || c == '/' || Character.isAlphabetic(c)) {
					Found = true;
					pos = j;
				}
			}
		}
		if (Found) {
			return pos + 1;
		} else {
			return pos;
		}

	}
}
