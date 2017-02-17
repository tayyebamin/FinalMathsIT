import java.io.LineNumberInputStream;
import java.util.ArrayList;

import algebra.*;
import numbers.*;
import util.*;
import util.ConvertDisplay.Mode;

public class testing {
	static Expression E = new Expression();
	static ConvertDisplay cD = new ConvertDisplay();
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
		btn= giveArray("cbrt 27 NEXT *  sqrt 16");
		EVAL("simple");	
		btn=giveArray("Ans + 2");
		EVAL("extended");
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
//		cD.DisplayMode=Mode.RATIONAL;
//		btn=giveArray("1 whole 7 bata 9");
//		RATIONAL();
//		cD.DisplayMode=Mode.ALGEBRA;
//		btn = giveArray("2x^2 + 3x -4");
//		//String [] btna={"-","3","x^","2" ,"+","1","5","x","-","4"};
		String [] btna={"-","3","x^","3" ,"-","3","x^","2","+","5","0","0"};
		Polynomial p1;
		p1 = new Polynomial();
		p1=Algebra(btna);
		System.out.println(p1.evaluate(5.0).toPlainString());
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
			System.out.println("Algebra [Algebra]: " + P.toString());
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



