import java.util.*;

public class teste{
	public static void main(String args[]) {
		Scanner teclado = new Scanner(System.in);
		System.out.println("Inteiro: ");
		int n = teclado.nextInt();
		System.out.println(String.format("%032d", (Integer.toBinaryString(n)).toString()));
		teclado.close();
	}
}