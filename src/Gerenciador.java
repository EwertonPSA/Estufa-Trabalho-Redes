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
	private static SocketChannel sensorCO2 = null;
	private static SocketChannel aquecedor = null;
	private static SocketChannel resfriador = null;
	private static SocketChannel irrigador = null;
	private static SocketChannel injetorCO2 = null;
	private static SocketChannel cliente = null;
	
	private static Integer temperaturaLida = 0;
	private static Integer limiarSupTemperatura;
	private static Integer limiarInfTemperatura;
	private static boolean statusAquecedor;
	private static boolean statusResfriador;
	private static boolean statusSensorTemp;
	
	private static Integer umidadeSoloLida = 1;
	private static Integer limiarSupUmidade;
	private static Integer limiarInfUmidade;
	private static boolean statusSensorUmidade;
	private static boolean statusIrrigador;
	
	private static Integer co2Lido = 2;
	private static Integer limiarSupCO2;
	private static Integer limiarInfCO2;
	private static boolean statusSensorCO2;
	private static boolean statusInjetorCO2;
	
	private static ByteBuffer msgCliente;

	/*Simuladores*/
	private static Temperatura ambiente = null;
	private static UmidadeSolo solo = null;
	private static CO2 ar = null;
	
	//Cada endereço remoto esta associado a um equipamento, assim quando um canal pedir uma msg vou identifica-lo pelo endereço remoto
	static Map<SocketAddress, Integer> equipaments = null;
	
	private static String intToChar(int temperaturaInt) {
		int aux = temperaturaInt;
		char[] seqNumero = new char[4];
		String r = "";
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (char) ((int)aux>>(i*8) & (int)0xFF);
			r += String.valueOf(seqNumero[i]);
		}
		return r;
	}
	
	private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel channel = serverSocket.accept();//Abre um socket pro canal
        channel.configureBlocking(false);//configura ele como nao bloqueante
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);//Coloca um selector pra monitorar esse socket
    }
	
	public static void receive(SelectionKey key) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		SocketChannel channel = (SocketChannel) key.channel();
		byte[] arr;
		int byteReceive = 0;
		
		do {
			byteReceive = channel.read(buffer);
		}while(byteReceive <= 0);
		
		arr = buffer.array();
		
		byte header = arr[0];
		if(header == '1'){			
			SocketAddress clientAddress  = channel.getRemoteAddress();//Pego o endereço remoto do equipamento
			channel.read(buffer);//le e Repassa pro buffer o que foi enviado pelo cliente
			buffer.flip();//Vai pra posicao zero do buffer
			
			// Registra a SelectionKey associado a esse equiamento na Map
			equipaments.put(clientAddress, arr[1]-'0');
			
			switch(arr[1]) {/*Registra os canais de comunica, assim quando um SelectionKey de sensorTemperatura vier, por ex, ja repassarei para o aquecedor(se for necessario) */
				case '1':
					System.out.println("Sensor de Temperatura Registrado!");
					sensorTemperatura = channel;
					break;
				case '2':
					System.out.println("Sensor de Umidade Registrado!");
					sensorUmidade = channel;
					break;
				case '3':
					System.out.println("Sensor de CO2 Registrado!");
					sensorCO2 = channel;
					break;
				case '4':
					System.out.println("Aquecedor Registrado!");
					aquecedor = channel;
					break;
				case '5':
					System.out.println("Resfriador Registrado!");
					resfriador = channel;
					break;
				case '6':
					System.out.println("Irrigador Registrado!");
					irrigador = channel;
					break;
				case '7':
					System.out.println("Injetor Registrado!");
					injetorCO2 = channel;
					break;
				case '8':
					System.out.println("Cliente Registrado!");
					cliente = channel;
					break;
			}
			
			//Repassa mensagem 2 (confirmacao)
			String answer = "2";
			buffer = ByteBuffer.wrap(answer.getBytes());
	        channel.write(buffer);
	        
		} else if(header == '3'){	// Leitura dos recebida dos sensores
			// Identifica o sensor
			SocketAddress clientAddress  = channel.getRemoteAddress(); // Pego o endereço remoto do equipamento
			Integer id = equipaments.get(clientAddress);	/*Pega o id associado ao endereço remoto do equipamento*/
			switch(id) {
				case 1:
					temperaturaLida = byteToInt(2, arr);
					break;
				case 2:
					umidadeSoloLida = byteToInt(2, arr);
					break;
				case 3:
					co2Lido = byteToInt(2, arr);
					break;
			}
		} else if(header == '6') {	// Pedido de configuracao de limiares pelo cliente
			
			char tipoParametro = (char)arr[1];
			int minVal = byteToInt(2, arr);
			int maxVal = byteToInt(6, arr);
		
			String printString = "Novos limiares de ";
			
			if(tipoParametro == '1') {
				printString += "temperatura: ";
				limiarInfTemperatura = minVal;
				limiarSupTemperatura = maxVal;
			} else if(tipoParametro == '2') {
				printString += "umidade: ";
				limiarInfUmidade = minVal;
				limiarSupUmidade = maxVal;
			} else {
				printString += "CO2: ";
				limiarInfCO2 = minVal;
				limiarSupCO2 = maxVal;
			}
			
			printString += minVal + " a " + maxVal;
			System.out.println(printString);
			
			msgCliente = ByteBuffer.wrap(printString.getBytes());
			//cliente.write(buffer);
		} else if(header == '7') {
			String printString = "Enviando leitura de ";
			char tipoParametro = (char)arr[1];
			switch(tipoParametro) {
				case '1':
					printString += "temperatura: " + temperaturaLida.toString();
					msgCliente = ByteBuffer.wrap(("8" + tipoParametro + intToChar(temperaturaLida.intValue())).getBytes());
					break;
				case '2':
					printString += "umidade: " + umidadeSoloLida.toString();
					msgCliente = ByteBuffer.wrap(("8" + tipoParametro + intToChar(umidadeSoloLida.intValue())).getBytes());
					break;
				case '3':
					printString += "CO2: " + co2Lido.toString();
					msgCliente = ByteBuffer.wrap(("8" + tipoParametro + intToChar(co2Lido.intValue())).getBytes());
					break;	
			}
			System.out.println(printString);
		}
	}

	private static Integer byteToInt(int position, byte[] arr) {
		int num = 0;
		for(int i = 3; i >= 0; i--) {
			num = num<<8;
			num = num + (arr[i+position]&0xff);
			//System.out.println((int)(arr[i+position]&0xff));
		}
		//System.out.println(num);
		
		return num;
	}
	
	// Responde a uma solicitacao 
	public static void send(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		SocketAddress clientAddress  = channel.getRemoteAddress();	// Pego o endereço remoto do equipamento
		Integer idEquipaments = equipaments.get(clientAddress);	// Busca o id do equipamento associado ao enderço remoto
		
		//Caso o equipamento solicite a leitura mas nao tenha sido identificado
		if(idEquipaments == null) 
			return;
		
		switch(idEquipaments) {
			case 4:
				if(statusAquecedor == false && temperaturaLida < limiarInfTemperatura) {//Se atuador estiver desligado e temperaturaLida estiver a baixo do limiar
					System.out.println("Servidor informando ao aquecedor para ligar!");
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					aquecedor.write(msg);
					statusAquecedor = true;//Significa que foi enviado a msg para o atuador se ligar
				}else if(statusAquecedor == true && temperaturaLida >= limiarSupTemperatura) {//Se atuador estiver ligado
					System.out.println("Servidor informando ao aquecedor para desligar!");
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					aquecedor.write(msg);
					statusAquecedor = false;
				}
				break;
			case 5:
				if(statusResfriador == false && temperaturaLida > limiarSupTemperatura) {//Se atuador estiver desligado e temperaturaLida estiver a cima do limiar
					System.out.println("Servidor informando ao resfriador para ligar!");
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					resfriador.write(msg);
					statusResfriador = true;//Significa que foi enviado a msg para o atuador se ligar
				}else if(statusResfriador == true && temperaturaLida <= limiarInfTemperatura) {//Se atuador estiver ligado
					System.out.println("Servidor informando ao resfriador para desligar!");
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					resfriador.write(msg);
					statusResfriador = false;
				}
				break;
			case 6:
				if(statusIrrigador == false && umidadeSoloLida < limiarInfUmidade) {//Liga
					System.out.println("Servidor informando ao irrigador para ligar!");
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					irrigador.write(msg);
					statusIrrigador = true;
				}else if(statusIrrigador == true && umidadeSoloLida >= limiarSupUmidade) {//desliga
					System.out.println("Servidor informando ao Irrigador para desligar");
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					irrigador.write(msg);
					statusIrrigador = false;
				}
				
				break;
			case 7:
				if(statusInjetorCO2 == false && co2Lido < limiarInfCO2) {
					System.out.println("Servidor informando ao injetor para ligar!");
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					injetorCO2.write(msg);
					statusInjetorCO2 = true;
				}else if(statusInjetorCO2 == true && co2Lido >= limiarSupCO2) {
					System.out.println("Servidor informando ao injetor para desligar!");
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					injetorCO2.write(msg);
					statusInjetorCO2 = false;
				}
				break;
			case 8:
				if(msgCliente != null) {//Se houver mensagem a ser enviada
					cliente.write(msgCliente);
					msgCliente = null;
				}
				break;
		}
	}

	private static void setStatusDefaultEquipamentos() {
		statusAquecedor = false;
		statusResfriador = false;
		statusSensorTemp = false;
		statusSensorUmidade = false;
		statusIrrigador = false;
		statusSensorCO2 = false;
		statusInjetorCO2 = false;
		
		limiarInfCO2 = 300;
		limiarSupCO2 = 310;
		co2Lido = 300;
		CO2.setContribuicaoCO2(0);
		
		umidadeSoloLida = 40;
		limiarSupUmidade = 50;
		limiarInfUmidade = 40;
		UmidadeSolo.setContribuicaoUmidadeEquip(0);
		
		limiarSupTemperatura = 20;
		limiarInfTemperatura = 10;
		temperaturaLida = 0;
		ambiente.setContribuicaoTemperaturaEquip(0);//A contribuicao do equipamento eh inicializado com 0 pois os atuadores inicializam desligados

		
	}
	
	/* Caso o equipamento seja desconectado os status do equipamento sao resetados*/
	private static void resetStatusEquip(SocketChannel equip) {
		if(equip == aquecedor) {
			System.out.println("Aquecedor foi desconectado!");
			statusAquecedor = false;
			if(statusResfriador == false)//Se o resfriado nao estiver ligado
				ambiente.setContribuicaoTemperaturaEquip(0);
		}else if(equip == sensorTemperatura) {
			System.out.println("Sensor de Temperatura foi desconectado!");
		}else if(equip == resfriador) {
			System.out.println("Resfriador foi desconectado!");
			statusResfriador = false;
			if(statusAquecedor == false)//Se o aquecedor nao estiver ligado
				ambiente.setContribuicaoTemperaturaEquip(0);
		}else if(equip == sensorUmidade) {
			statusSensorUmidade = false;
			System.out.println("Sensor de Umidade foi desconectado!");
		}else if(equip == irrigador) {
			System.out.println("Irrigador foi desconectado!");
			statusIrrigador = false;
			UmidadeSolo.setContribuicaoUmidadeEquip(0);
		}else if(equip == injetorCO2) {
			statusInjetorCO2 = false;
			System.out.println("Injetor foi desconectado!");
			CO2.setContribuicaoCO2(0);
		}else if(equip == sensorCO2) {
			statusSensorCO2 = false;
			System.out.println("Sensor de CO2 foi desconectado!");
		}else if(equip == cliente) {
			System.out.println("Cliente foi desconectado!");
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
		solo = new UmidadeSolo();
		solo.start();
		ar = new CO2();
		ar.start();
		
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