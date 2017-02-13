import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {

   private static final String REGEX = "[cos sin tan]\\(([^\\)]*)\\)";
   private static final String INPUT = "4+cos(e+200*pi)+sin(400*pi-7)+300*pi";
   //private static final String INPUT = "caot cat(4+pi) cat(3+pi)";
   
   public static void main( String args[] ) {
      Pattern p = Pattern.compile(REGEX);
      Matcher m = p.matcher(INPUT);   // get a matcher object
      int count = 0;
      while(m.find()) {
         count++;
         System.out.println("Group: " +m.group(1));
      }
   }
}