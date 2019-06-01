import java.util.Scanner;

public class Teste {
	public static void main(String[] args) {
		Scanner entrada = new Scanner(System.in);
		String teclado = null;
		if(entrada.hasNext())
			teclado = entrada.next();
		System.out.println(teclado);
	}
}
