import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class TemperaturaInterna extends Thread{
	public InetSocketAddress hostAddress = null;
	public SocketChannel client = null;
	
	public TemperaturaInterna() throws IOException{
		
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		client.write(ByteBuffer.wrap("11".getBytes()));//Manda a mensagem de Identificacao: header + id
		
		
		ByteBuffer msgServer = ByteBuffer.allocate(256);
		int bytesRead = 0;
		do {
			bytesRead = client.read(msgServer);
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		byte msgServerByte[] = msgServer.array();
		if(msgServerByte[0] == '2') {
			System.out.println("Equipamento registrado");
		}else {
			System.out.println("Equipamento nao foi registrado");
			//lançar um throws notificando....tem que customizar esse throws...
		}
	}
	
	@Override
	public void run() {
		Scanner teclado = new Scanner(System.in);
		String input = null;
		ByteBuffer buffer = ByteBuffer.allocate(256);
		int byteRead = 0;
		String msgServer = null;
		input = "45";
		
		boolean alterna = false;
		
		while(true) {/*Se a conexao nao estiver feixada e quando o server repassar algo entra no while*/
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
				System.out.println("Falha no sensor de temperatura, desligando equipamento!");
				return;
			}
			
			buffer = ByteBuffer.wrap(input.getBytes());
			try {/*Envia a temperatura pro gerenciador*/
				client.write(buffer);
				buffer.clear();
				msgServer = new String(buffer.array()).trim();
				System.out.println("Enviado:" + msgServer);
				buffer.clear();
				if(alterna == true) {
					input = "45";
					alterna = false;
				}else {
					input = "46";
					alterna = true;
				}
			}catch(Exception e) {
				System.out.println("Servidor desligado, desligando sensor!");
				try {
					client.close();
				} catch (IOException e1) {;}
				return;
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException{
		try {
			TemperaturaInterna sensor = new TemperaturaInterna();
			sensor.start();
		}catch(Exception e) {
			System.out.println("Problema na conexao com o servidor");
		}
	}
}
