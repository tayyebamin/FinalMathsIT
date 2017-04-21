import java.io.LineNumberInputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import algebra.*;
import numbers.*;
import util.*;
import util.ConvertDisplay.Mode;

public class testing {
	static Expression E = new Expression();
	static ConvertDisplay cD = new ConvertDisplay();
	static BigDecimal bd;
	static BigDecimal min;
	static String[] btn;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//cD.DisplayMode=Mode.ALGEBRA;
		//Polynomial P = new Polynomial();
		//String[] btn={"4","x^","-","5","-","3","2","end"};
		//P=cD.giveAlgebra(btn);
		//System.out.println(P.toString());
		
		cD.DisplayMode=Mode.NORMAL;
		
		cD.cDAngleMode=AngleMode.DEGREE;
		cD.Angle.degree=30;
		cD.Angle.minutes=10;
		cD.Angle.seconds=15;
		cD.Angle.Evaluate();
		System.out.println(cD.Angle.giveDtoDSM());
			E.setExpression("sin(" + cD.Angle.value + ")");
			cD.setE(E);
		System.out.println(E.eval().toPlainString());
		min=new BigDecimal(1E-10);
		bd=new BigDecimal(0.66660000000000000000000000000000000000000000000000000000000001);
		System.out.println("Tayyeb" + bd.compareTo(min));
		
		//String[] btn1 = {"sin","cos","tan","4","5","NEXT","NEXT","NEXT","+","1"};
		//String[] btn1 = {"sin","FAC","4","NEXT","+","1","*","FAC","3"};
		//String[] btn1 = {"log","4","NEXT","+","9"};
//		String exp="sin(8)";
//		Expression ex = new Expression();
//		ex.setExpression(exp);
//		System.out.println(ex.eval().toPlainString());
//		cD.setE(ex);
//		cD.giveLatex();
//		System.out.println(cD.latexOutput);
////		
		// create a BigDecimal object
//	    System.out.println("----------------------------");
//		BigDecimal bd = new BigDecimal(4.999999999);
//		BigDecimal bd1 = new BigDecimal(0);
//		bd1=bd.setScale(10,RoundingMode.HALF_UP);
//		BigDecimal diff = new BigDecimal(0);
//		diff = bd1.subtract(bd);
//		int result=0;
//		result = diff.compareTo(new BigDecimal(1E-10));
//		System.out.println(bd.toPlainString());
//		System.out.println(bd1.toPlainString());
//		System.out.println(diff.toPlainString());
//		
//		if (result == 1) {
//			System.out.println(bd.toPlainString() + " accuracy is less than 1E-10 " );
//			System.out.println("Return value " + bd.toPlainString());
//		}
//		if (result == 0) {
//			System.out.println("Return value " + bd);
//		}
//		if (result == -1) {
//			System.out.println(bd.toPlainString() + " accuracy is less than 1E-10 " );
//			System.out.println("Return value " + bd1.toPlainString());
//		}
//		
//		System.out.println("----------------------------");
		
		
		
		
		
		
		
		//,"NEXT","+","cos","3","0","D"
		
	      
		String[] btn2 = {"6","*","sqrt","2","5","NEXT","-","e","^","5","+","pi","*","5","^","3","+","2","^","2","-","6","^","-","1","NEXT","*","log10","2"};
		String[] btn1 = {"2","+","3"};
		E=cD.giveExpression(btn1);
		System.out.println("Awais: " + E.getExpression());
		E.EAngleMode = cD.cDAngleMode;
		try {
			cD.Ans = E.eval();
			
			System.out.println("Answer(Simple String) [C H E C K ] = " +cD.GiveAns());
			//System.out.println("Answer(Plain String) = " +cD.Ans.toPlainString());
			 
		} catch (Exception e) {
			System.out.println("Error");
		}
		//System.out.println("Cur Pos" + cD.CursorPos + " at " + cD.evaluateInput.charAt(cD.CursorPos));
		System.out.println("Angle Mode CD: " + cD.cDAngleMode );
		System.out.println("Expression angle mode: " + E.EAngleMode);
		System.out.println("Expression: " + E.toString());
		cD.giveLatex();
		System.out.println("Latex output: " + cD.latexOutput);
		System.out.println("-----------------------------");
		E=cD.giveExpression(btn2);
		E.EAngleMode = cD.cDAngleMode;
		
		//System.out.println("Cur Pos" + cD.CursorPos + " at " + cD.evaluateInput.charAt(cD.CursorPos));
		System.out.println("Angle Mode CD: " + cD.cDAngleMode );
		System.out.println("Expression angle mode: " + E.EAngleMode);
		System.out.println("Expression: " + E.toString());
		cD.giveLatex();
		System.out.println("Latex output: " + cD.latexOutput);
		
		
//		
		//btn= giveArray("sin cos 45 NEXT + 12");
//		String [] btn1 = {"log10", "2"};
//		//btn=btn1;
		//EVAL("simple");	
//		btn=giveArray("Ans + 2");
		//EVAL("extended");
//		btn=giveArray("FAC "
//				+ "8 NEXT + 3");
//		EVAL("simple");
//		btn=giveArray("sqrt Ans");
		//EVAL();
		
//		btn=giveArray("sin 62831.8530717959");
//		btn=giveArray("tan 91.5*pi");
//		//EVAL();
//		btn=giveArray("2+(4-log20 NEXT +(3-4)) * sqrt16+4^2  -sin pi");
//		btn=giveArray("2*sin pi NEXT *4 *-2");
//		btn=giveArray("2*(4*sqrt16 NEXT + 9 * sin60)");
//		//EVAL("simple");
//		cD.cDAngleMode=AngleMode.DEGREE;
//		cD.cDAngleMode=AngleMode.RADIAN;
//		btn=giveArray("2+1/2");
//		//EVAL("extended");
		
		
		
//		RationalNum r1;
//		cD.DisplayMode=Mode.RATIONAL;
//		String[] btn1 = {"2","whole","1","bata","3"};
//		r1=cD.giveRational(btn1);
//		System.out.println(r1.toString());
//		RationalNum r2;
//		String[] btn2 = {"3","bata","7"};
//		r2=cD.giveRational(btn2);
//		System.out.println(r2.toString());
//		System.out.println (r2.Compare(r1));
		
		
		
		
		
		//btn=giveArray("1 whole 7 bata 9");
		//RATIONAL();
//		cD.DisplayMode=Mode.ALGEBRA;
//		btn = giveArray("2x^2 + 3x -4");
//		//String [] btna={"-","3","x^","2" ,"+","1","5","x","-","4"};
//		String [] btna={"x^","2" ,"+","x","+","5","0"};
//		Polynomial p1, p2;
//		p1 = new Polynomial();
//		p1=Algebra(btna);
//		System.out.println(p1.toString());
//		System.out.println(p1.evaluate(3.0));
	
//		String [] btna1={"1","x^","2" };
//		p2 = new Polynomial();
//		p2 = Algebra(btna1);
//		System.out.println(p2.toString());
//		System.out.println(p1.multiply(p2).toString());
////		p2 = new Polynomial();
//		p1=Algebra(btna);
//		if (p1.isValidQuadratic()) { System.out.println(p1.solveQuadratic()); 
//		
//		} else {
//			System.out.println("Invalid Quadratic");
//		}
//		System.out.println(p1.toString());
//		Matrix M = new Matrix(new double[][]{{14,6},{36,17}});
//		System.out.println(M.toString());
//		System.out.println(String.valueOf(M.determinant(M,2)));
//		Matrix Minv = new Matrix(2,2);
//		Minv = M.inverse();
//		System.out.println(Minv.toString());
//		Matrix Minvagain = new Matrix(2,2);
//		Minvagain = Minv.inverse();
//		System.out.println(Minvagain.toString());
//		System.out.println(String.valueOf(Minvagain.determinant(Minvagain,2)));
		//double dd = Math.signum(d) * (Math.abs(d) + (m / 60.0) + (s / 3600.0));
		//System.out.println(String.valueOf(dd));
		}
	public static String[] giveArray(String str){
		ArrayList<String> Arr = new ArrayList<String>();
		String[] Str;
		String func="";
		int ind=0;
		boolean FuncFlag=false;
		for (int i = 0; i<=str.length()-1;i++)
		{
			if (Character.isSpace(str.charAt(i))){
			
				FuncFlag=false;
				if (func.length() != 0){
					Arr.add(func);
					func="";
				}
				continue;
			}
			if (Character.isAlphabetic(str.charAt(i))){
				func=func+str.charAt(i);
				FuncFlag=true;
				continue;
			}
			else
			{
				if (!FuncFlag){
				Arr.add(String.valueOf(str.charAt(i)));
				}
				else
				{
					Arr.add(func);
					func="";
					FuncFlag=false;
					Arr.add(String.valueOf(str.charAt(i)));
				}
			}
		}
		if (func.length()!=0) {
			Arr.add(func);
		}
		Str = new String[Arr.size()];
		Str = Arr.toArray(Str);
		return Str;
	}
	public static String giveStr(String[] Str){
		String output="", Array="";
		for (int i=0; i<Str.length;i++)
		{
			Array = Array+"\""+ Str[i]+"\",";
			output = output+Str[i];
		}
		System.out.println("Array: " +Array);
		return output;
	}
	private static void EVAL(String showMode ){
		E = cD.giveExpression(btn);
		E.EAngleMode = cD.cDAngleMode;
		cD.Ans = E.eval();
		System.out.println("Angle Mode CD: " + cD.cDAngleMode );
		System.out.println("Expression angle mode: " + E.EAngleMode);
		System.out.println("Expression: " + E.toString());
		System.out.println("Latex output: " + cD.latexOutput);
		cD.giveLatex();
		System.out.println("Latex output: " + cD.latexOutput);
		try {
			switch (showMode) {
			case "simple":
			System.out.println("Plain Eval: " + E.eval().toPlainString());
			System.out.println("Eng Eval: " + E.eval().toEngineeringString());
			System.out.println("Normal Eval: " + E.eval().toString());
			break;
			case "extended":
				System.out.println("Plain Eval: " + E.eval().toPlainString());
				System.out.println("Eng Eval: " + E.eval().toEngineeringString());
				System.out.println("Normal Eval: " + E.eval().toString());
				break;
			}
			//System.out.println(E.getDeclaredFunctions());
			//System.out.println(E.getDeclaredOperators());
			//System.out.println(E.getDeclaredVariables());
			
			System.out.println("--------------------------------");
		}
		catch (Exception ex){
			System.out.println("Error: " + ex.getMessage());
		}
	}
	private static void RATIONAL(){
		cD.DisplayMode = Mode.RATIONAL;
		RationalNum R = new RationalNum();
		try {
			R = cD.giveRational(btn);
			//R.SimplificationMode=R.SimplificationMode.RATIONAL;
			System.out.println("Rational [Rational]: " + R.toString());
//			R.SimplificationMode=R.SimplificationMode.WHOLENUMBER;
//			System.out.println("Rational [Whole]: " + R.toString());
//			R.SimplificationMode=R.SimplificationMode.SIMPLIFY;
//			System.out.println("Rational [Simplify]: " + R.toString());
			cD.setR(R);
			cD.giveLatex();
			System.out.println("Latex output: " + cD.latexOutput);
			System.out.println("--------------------------------");
		}
		catch (Exception ex){
			System.out.println("Error: " + ex.getMessage());
		}
	}
	private static Polynomial Algebra(String [] btn1){
		
		Polynomial P = new Polynomial();
		try {
			P = cD.giveAlgebra(btn1);
			//R.SimplificationMode=R.SimplificationMode.RATIONAL;
			//System.out.println("Algebra [Algebra]: " + P.toString());
//			R.SimplificationMode=R.SimplificationMode.WHOLENUMBER;
//			System.out.println("Rational [Whole]: " + R.toString());
//			R.SimplificationMode=R.SimplificationMode.SIMPLIFY;
//			System.out.println("Rational [Simplify]: " + R.toString());
			cD.setP(P);
			cD.giveLatex();
			System.out.println("Latex output: " + cD.latexOutput);
			System.out.println("--------------------------------");
		}
		catch (Exception ex){
			System.out.println("Error: " + ex.getMessage());
		}
		return P;
	}
	
	
}



