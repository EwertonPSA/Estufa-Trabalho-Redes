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

public class Gerenciador{
	private static SocketChannel sensorTemperatura = null;
	private static SocketChannel sensorUmidade = null;
	private static SocketChannel sensorC02 = null;
	private static SocketChannel aquecedor = null;
	private static SocketChannel resfriador = null;
	private static SocketChannel irrigador = null;
	private static SocketChannel injetorC02 = null;
	private static Integer temperaturaLida;
	private static boolean statusAtuadorTemp;
	private static boolean statusSensorTemp;
	private static Integer limiarSupTemperatura;
	private static Integer limiarInfTemperatura;
	private static Temperatura ambiente = null;
	static Map<SocketAddress, Integer> equipaments = null;//Cada endereço remoto esta associado a um equipamento, assim quando um canal pedir uma msg vou identifica-lo pelo endereço remoto
	
	private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();//Abre um socket pro canal
        client.configureBlocking(false);//configura ele como nao bloqueante
        client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);//Coloca um selector pra monitor esse socket
    }
	
	public static void receive(SelectionKey key) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		SocketChannel client = (SocketChannel) key.channel();
		byte[] arr;
		int byteReceive = 0;
		do {
			byteReceive = client.read(buffer);
		}while(byteReceive <= 0);

		arr = buffer.array();
		if(arr[0] == '1'){
			System.out.println("Id do equipamento conectado: " + (arr[1] - '0'));
			SocketAddress clientAddress  = client.getRemoteAddress();//Pego o endereço remoto do equipamento
			client.read(buffer);//le e Repassa pro buffer o que foi enviado pelo cliente
			buffer.flip();//Vai pra posicao zero do buffer
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
					statusAtuadorTemp = false;
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
		}else if(arr[0] == '3'){//Leitura dos sensores
			SocketAddress clientAddress  = client.getRemoteAddress();//Pego o endereço remoto do equipamento
			Integer id = equipaments.get(clientAddress);/*Pega o id associado ao endereço remoto do equipamento*/

			switch(id) {
				case 1:
					temperaturaLida = byteToInt(2, arr);
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5:
					break;
				case 6:
					break;
				case 7:
					break;
			}
		}
	}

	private static Integer byteToInt(int position, byte[] arr) {
		int num = 0;
		for(int i = 3; i >= 0; i--) {
			num = num<<8;
			num = num + (int)(arr[i+position]&0xff);
		}
		return num;
	}
	
	public static void send(SelectionKey key) throws IOException {
		SocketChannel client = (SocketChannel) key.channel();
		SocketAddress clientAddress  = client.getRemoteAddress();//Pego o endereço remoto do equipamento
		Integer idEquipaments = equipaments.get(clientAddress);//Busca o id do equipamento associado ao enderço remoto
		if(idEquipaments == null) return;//Caso o equipamento solicite a leitura mas nao tenha sido identificado
		switch(idEquipaments) {
			case 1:	
				break;
			case 2:
				
				break;
			case 3:
				break;
			case 4:
				if(statusAtuadorTemp == false && temperaturaLida <= limiarInfTemperatura) {//Se atuador estiver desligado e temperaturaLida estiver a baixo do limiar
					System.out.println("Servidor informando ao atuador para ligar!");
					try {//Tenta enviar os dados para o aquecedor
						ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
						aquecedor.write(msg);
						statusAtuadorTemp = true;//Significa que foi enviado a msg para o atuador se conectar
						
					}catch(Exception e) {/*Se der problema no envio da mensagem o status permanece desativado*/
						System.out.println("Aquecedor nao se encontra conectado");
					}
				}else if(statusAtuadorTemp == true && temperaturaLida > limiarSupTemperatura) {//Se atuador estiver ligado
					System.out.println("Servidor informando ao atuador para desligar!");
					try {//Tenta enviar os dados para o aquecedor
						ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
						aquecedor.write(msg);
						statusAtuadorTemp = false;
					}catch(Exception e) {
						System.out.println("Aquecedor nao se encontra conectado");
					}
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
	
	private static boolean temperaturaMedia() {
		return temperaturaLida < limiarSupTemperatura && temperaturaLida > (limiarSupTemperatura + limiarInfTemperatura);
	}

	private static void setStatusDefaultEquipamentos() {
		statusAtuadorTemp = false;
		statusSensorTemp = false;
		limiarSupTemperatura = 269;
		limiarInfTemperatura = 220;
		temperaturaLida = 200;
		ambiente.setContribuicaoTemperaturaEquip(0);//A contribuicao do equipamento eh inicializado com 0 pois os atuadores inicializam desligados
		
	}
	
	/* Caso o equipamento seja desconectado os status do equipamento sao resetados*/
	private static void resetStatusEquip(SocketChannel equip) {
		if(equip == aquecedor) {
			System.out.println("Aquecedor foi desconectado!");
			statusAtuadorTemp = false;
			ambiente.setContribuicaoTemperaturaEquip(0);
		}else if(equip == sensorTemperatura) {
			System.out.println("Sensor de Temperatura foi desconectado!");
		}	
	}
	
	/* Administra os canais de comunicacao e inicializa a simulacao da temperatura*/
	public static void main(String[] argc) throws IOException{
		Selector selector  = Selector.open();
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		
		InetSocketAddress hostAddress = new InetSocketAddress("127.0.0.1", 9545);// ip localhost e porta qualquer
		try {
			serverSocket.bind(hostAddress);
		}catch(Exception e) {/*Se ja tiver um gerenciador em execucao ou ip e porta tiver sendo usada*/
			System.out.println("O localhost com a porta 9545 ja esta em uso!");
			return;
		}
		serverSocket.configureBlocking(false);
		
		serverSocket.register(selector, SelectionKey.OP_ACCEPT);// Coloca o selector para administrar a escuta dos canais 
		equipaments = new HashMap<SocketAddress, Integer>();
		
		
		ambiente = new Temperatura();
		ambiente.start();//Inicializa a simulacao da temperatura Ambiente
		setStatusDefaultEquipamentos();
		
		
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
						System.out.println("Problema no registro do socket do cliente");
					}
				}
				
				if(key.isValid() && key.isReadable()) {//Algum canal se comunicou
					try{
						receive(key);
					}catch(Exception e) {
						/* Se o Equipamento desligar e for captado algo no canal
						 * Vai acontecer problema de leitura, aqui trato a desconexao com o canal do equipamento*/
						SocketChannel equip = (SocketChannel) key.channel();
						resetStatusEquip(equip);
						equip.close();
					}
				}
				
				if(key.isValid() && key.isWritable()) {
					try {
						send(key);
					}catch(Exception e) {
						/* Se o Equipamento desligar antes de receber os dados no canal
						 * Vai acontecer problema de envio, aqui trato a desconexao do canal do equipamento*/
						SocketChannel equip = (SocketChannel) key.channel();
						resetStatusEquip(equip);
						equip.close();
					}
				}
				it.remove();
			}
		}
	}
}