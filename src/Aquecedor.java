import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class Aquecedor{
	private InetSocketAddress hostAddress = null;
	private SocketChannel client = null;
	
	public Aquecedor() throws IOException {
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		client.write(ByteBuffer.wrap("14".getBytes()));//Manda a mensagem de Identificacao: header + id
	}

	public SocketChannel getClient() {
		return client;
	}

	public void communicate(){
		int bytesRead = 0;
		String msgServer = null;
		while(client.isConnected()) {/*Se a conexao nao estiver feixada e quando o server repassar algo entra no while*/
			System.out.println("Aguardando mensagem da temperatura..");
			ByteBuffer newBuff = ByteBuffer.allocate(256);
			
			try {
				do {
					bytesRead = client.read(newBuff);
				}while(bytesRead <= 0);
				msgServer = new String(newBuff.array());
				
				if(msgServer.contentEquals("2")) {
					System.out.println("Aquecedor foi identificado pelo servidor");
				}else		
					System.out.println("Resposta do servidor:" + msgServer);
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException, InterruptedException{
		Aquecedor atuador = null;
		try{
			atuador = new Aquecedor();
			//Aciono uma thread que fecha o canal quando finalizar o programa
			Runtime.getRuntime().addShutdownHook(new ShutDownTask(atuador.getClient()));//Acionado quando da CTRL+C ***NO TERMINAL***
			atuador.communicate();
		}catch(Exception e) {
			System.out.println("Deu problema no ataudor");
		}
	}
	
	/*Classe que extende de Thread para finalizar a comunicacao com o servidor*/
	/* NAO FUNCIONA POR CAUSA DE UM BUG NO GANCHO DE DESLIGAMENTO DO ECLIPSE
	 * CLICKANDO NO BOTAO VERMELHO PRA FINALIZAR O PROGRAMA NO ECLIPSE ELE NAO FAZ O GANCHO DE DESLIGAMENTO CORRETO E POR ISSO NAO CONSIGO
	 * SABER QUANDO UM PROGRAMA FINALIZA */
	private static class ShutDownTask extends Thread {
		public SocketChannel client = null;
		public ShutDownTask(SocketChannel client) {
			this.client = client;
		}
		
		@Override
		public void run() {
			try {
				client.close();
				System.out.println("Aquecedor desligado");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}