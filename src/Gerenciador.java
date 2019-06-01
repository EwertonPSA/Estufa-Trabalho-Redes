import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
//vou ter que criar um objeto do servidor e repassar pros servidores do cliente
//Ai usa o syncronized pra nao dar problema de concorrencia
//As threads dos clientes devem chamar o método e esse método passa pros demais
import java.util.Set;

public class Gerenciador{//Server master, com ele consigo conectar varios clientes e tambem desconecta-los
	static SocketChannel sensorTemperatura = null;
	static SocketChannel sensorUmidade = null;
	static SocketChannel sensorC02 = null;
	static SocketChannel aquecedor = null;
	static SocketChannel resfriador = null;
	static SocketChannel irrigador = null;
	static SocketChannel injetorC02 = null;
	static ByteBuffer msgSensorTemperatura;
	
	static Map<SocketAddress, Integer> equipaments = null;//Cada endereço remoto associo a um equipamento, assim quando um canal enviar uma msg vou identifica-lo pelo endereço remoto
	
	private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();//Abre um socket pro canal
        client.configureBlocking(false);//configura ele como nao bloqueante
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);//Coloca um selector pra monitor esse socket
    }
	
	public static void receive(SelectionKey key, ByteBuffer buffer) throws IOException {
		SocketChannel client = (SocketChannel) key.channel();
		SocketAddress clientAddress  = client.getRemoteAddress();//Pego o endereço remoto do equipamento
		Integer idEquipaments = equipaments.get(clientAddress);//Busca o id do equipamento associado ao enderço remoto
		if(idEquipaments == null){//Se o selector(canal de escuta de um socket) nao se encontra no Map eh pq o equipamento esta enviando a mensagem de identificacao]
			
			client.read(buffer);//le e Repassa pro buffer o que foi enviado pelo cliente
			buffer.flip();
			byte[] arr = buffer.array();
			System.out.println("Id do equipamento conectado: " + (arr[1] - '0'));
			
			equipaments.put(clientAddress, arr[1]-'0');/*Registra a SelectionKey associado a esse equiamento na Map*/
			switch(arr[1]) {/*Registra os canais de comunica, assim quando um SelectionKey de sensorTemperatura vier, por ex, ja repassarei para o aquecedor(se for necessario) */
				case '1':
					sensorTemperatura = client;
					break;
				case '2':
					sensorUmidade = client;
					break;
				case '3':
					sensorC02 = client;
					break;
				case '4':
					aquecedor = client;
					break;
				case '5':
					resfriador = client;
					break;
				case '6':
					irrigador = client;
					break;
				case '7':
					injetorC02 = client;
					break;
			}
			
			buffer = ByteBuffer.wrap("2".getBytes());//Repassa a string "2" em bytes e joga pro buffer
	        client.write(buffer);//Envia a mensagem de confirmaçao pro cliente
	        buffer.clear();
		}else {
			Integer id = equipaments.get(clientAddress);/*Pega o id associado ao endereço remoto do equipamento*/
			int byteRead;
			switch(id) {
				case 1:	
					do {
						byteRead = client.read(msgSensorTemperatura);
					}while(byteRead > 0);
					String msgServer = new String(msgSensorTemperatura.array());
					System.out.println("Resposta do servidor:" + msgServer);
					break;
				case 2:
					
					break;
				case 3:
					
					System.out.println();
					break;
				case 4:
					System.out.println(client.isConnected());
					client.read(buffer);
					buffer.flip();
					
					aquecedor.write(buffer);
					buffer.clear();
					System.out.println("Atuador de aquecimento conectado!");
					break;
				case 5:
					
					System.out.println();
					break;
				case 6:
					
					System.out.println();
					break;
				case 7:
					
					System.out.println();
					break;
			}
		}
	}
	
	public static void send(SelectionKey key, ByteBuffer buffer) throws IOException {
		SocketChannel client = (SocketChannel) key.channel();
		SocketAddress clientAddress  = client.getRemoteAddress();//Pego o endereço remoto do equipamento
		Integer idEquipaments = equipaments.get(clientAddress);//Busca o id do equipamento associado ao enderço remoto
		
		if(idEquipaments == null) return;//Caso o equipamento nao tenha sido identificado
		
		Integer id = equipaments.get(clientAddress);/*Pega o id associado ao endereço remoto do equipamento*/
		
		switch(id) {
			case 1:	
				break;
			case 2:
				
				break;
			case 3:
				break;
			case 4:
				if(msgSensorTemperatura.position() != 0) {
					String msgServer = new String(msgSensorTemperatura.array());
					System.out.println("Enviando: " + msgServer);
					
					msgSensorTemperatura.flip();
					aquecedor.write(msgSensorTemperatura);
					msgSensorTemperatura.clear();
				}
				break;
			case 5:
				
				System.out.println();
				break;
			case 6:

				System.out.println();
				break;
			case 7:
				
				System.out.println();
				break;
		}
	}
	
	public static void main(String[] argc) throws IOException{
		ByteBuffer buffer = ByteBuffer.allocate(256);
		msgSensorTemperatura = ByteBuffer.allocate(256);
		Selector selector  = Selector.open();
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		
		InetSocketAddress hostAddress = new InetSocketAddress("127.0.0.1", 9545);// ip localhost e porta qualquer
		serverSocket.bind(hostAddress);
		serverSocket.configureBlocking(false);
		
		serverSocket.register(selector, SelectionKey.OP_ACCEPT);// Coloca o selector para administrar a escuta dos canais 
		equipaments = new HashMap<SocketAddress, Integer>();
		
		Set<SelectionKey> selectedKeys;
		while(true) {
			selector.select();
			selectedKeys = selector.selectedKeys();//Pega a lista de canais que fizeram algo no canal
			
			for(Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext();) {
				SelectionKey key = it.next();
				
				if(key.isAcceptable()) {//Canal que deseja se registrar no servidor
					try {
						register(selector, serverSocket);
					}catch(Exception e) {
						
						System.out.println("Problema no registro do cliente");
					}
				}
				
				if(key.isReadable()) {//Algum canal se comunicou
					try{
						receive(key, buffer);
					}catch(Exception e) {
						e.printStackTrace();
						System.out.println("Problema de comunicação com cliente");
					}
				}
				
				if(key.isWritable()) {
					try {
						send(key, buffer);
					}catch(Exception e) {
						e.printStackTrace();
						System.out.println("Problema de comunicação com cliente");
					}
				}
				it.remove();
			}
		}
	}
}