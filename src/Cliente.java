import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Cliente {
	public static void main(String[] argc) throws UnknownHostException, IOException{
		Socket cliente = new Socket("127.0.0.1", 9665);
		System.out.println("O cliente se conectou ao servidor!");
		
		PrintStream saida = new PrintStream(cliente.getOutputStream());
		Scanner server = new Scanner(cliente.getInputStream());
		Scanner teclado = new Scanner(System.in);

		String bemvindo = server.nextLine();
		System.out.println(bemvindo);
		while( !cliente.isClosed() && (teclado.hasNext() || server.hasNext())) {

			System.out.println(cliente.isOutputShutdown());
			System.out.println(cliente.isInputShutdown());
			System.out.println(cliente.isConnected() + " " + cliente.isClosed());
			if(teclado.hasNext()) {
				saida.println(teclado.nextLine());	
			}
			try {
				System.out.println("Resposta do servidor:" + server.nextLine());
			}catch(Exception e) {;}
		}
		
	}
}
