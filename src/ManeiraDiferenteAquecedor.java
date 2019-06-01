

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

public class ManeiraDiferenteAquecedor extends Thread{
	public static void receiveMessage(SocketChannel client) throws IOException {
		int bytesRead = 0;
		ByteBuffer read = ByteBuffer.allocate(256);
		do {
			bytesRead = client.read(read);
		}while(bytesRead > 0 && read.hasRemaining());
		
		String msgServer = new String(read.array());
		System.out.println("Resposta do servidor:" + msgServer);
	}
	
	public static void connection(Selector selector, SocketChannel client) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
        if (client.isConnectionPending()){//Verifica se o canal esta disponivel pra comunicaçao
            client.finishConnect();//Finaliza a sequencia de conexao para o canal de comunicaçao no modo nao bloqueante
        }
        client.register(selector, SelectionKey.OP_WRITE);//Deixa o canal no modo de escrita pra envia mensagem de identificaçao
		buffer = ByteBuffer.wrap("14".getBytes());//header+id de identificacao
		client.write(buffer);//Manda a mensagem de Identificacao
		client.register(selector, SelectionKey.OP_READ);//Depois deixa o canal no modo de leitura para identificar ações do gerenciador
	}
	
	
	/* Quando um SocketChannel C realizar uma opera C.read(buffer) ele altera o canal para o modo writeble pro servidor e pode retornar valores maiores que 0 e (-1 ou 0)
	 * Os valores maiores signifcam que foi recebido lido algo no canal, valores -1 ou 0 significa que o canal nao leu nada, */
	public static void main(String[] argc) throws UnknownHostException, IOException, InterruptedException{
		Selector selector = Selector.open();
		InetSocketAddress hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		SocketChannel client = SocketChannel.open();
		client.configureBlocking(false);
		client.register( selector, SelectionKey.OP_CONNECT);
		client.connect(hostAddress);

		Set<SelectionKey> selectedKeys;
		while(true) {
			selector.select();
			selectedKeys = selector.selectedKeys();//Pega a lista de canais que fizeram algo no canal
			
			for(Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext();) {
				SelectionKey key = it.next();
				if(key.isConnectable()) {//Testa se o canal desta chave finalizou ou não concluiu sua operação de conexão com o soquet
					try{
						connection(selector, client);
					}catch(Exception e) {
						System.out.println("Problema no registro do canal");
					}
				}
				
				if(key.isReadable()) {//Algum canal se comunicou
					try{				
						receiveMessage(client);
					}catch(Exception e) {
						System.out.println("Problema de comunicação com cliente");
					}
				}
				it.remove();
			}
		}
	}
}