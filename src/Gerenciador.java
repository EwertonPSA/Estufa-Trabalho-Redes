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
	
	private static Integer temperaturaLida;
	private static Integer limiarSupTemperatura;
	private static Integer limiarInfTemperatura;
	private static boolean statusAquecedor;
	private static boolean statusResfriador;
	private static boolean statusSensorTemp;
	
	private static Integer umidadeSoloLida;
	private static Integer limiarSupUmidade;
	private static Integer limiarInfUmidade;
	private static boolean statusSensorUmidade;
	private static boolean statusIrrigador;
	
	private static Integer co2Lido;
	private static Integer limiarSupCO2;
	private static Integer limiarInfCO2;
	private static boolean statusSensorCO2;
	private static boolean statusInjetorCO2;
	
	private static ByteBuffer msgCliente;

	/*Simuladores*/
	private static Temperatura ambiente = null;
	private static UmidadeSolo solo = null;
	private static CO2 ar = null;
	
	static Map<SocketAddress, Integer> equipaments = null;//Cada endereço remoto esta associado a um equipamento, assim quando um canal pedir uma msg vou identifica-lo pelo endereço remoto
	
	private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel channel = serverSocket.accept();//Abre um socket pro canal
        channel.configureBlocking(false);//configura ele como nao bloqueante
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);//Coloca um selector pra monitor esse socket
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
		if(arr[0] == '1'){
			
			SocketAddress clientAddress  = channel.getRemoteAddress();//Pego o endereço remoto do equipamento
			channel.read(buffer);//le e Repassa pro buffer o que foi enviado pelo cliente
			buffer.flip();//Vai pra posicao zero do buffer
			equipaments.put(clientAddress, arr[1]-'0');/*Registra a SelectionKey associado a esse equiamento na Map*/
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
			
			buffer = ByteBuffer.wrap("2".getBytes());//Repassa a string "2" em bytes e joga pro buffer
	        channel.write(buffer);//Envia a mensagem de confirmaçao pro cliente
		}else if(arr[0] == '3'){//Leitura dos sensores
			SocketAddress clientAddress  = channel.getRemoteAddress();//Pego o endereço remoto do equipamento
			Integer id = equipaments.get(clientAddress);/*Pega o id associado ao endereço remoto do equipamento*/
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
				case 8:
					msgCliente = buffer;
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
		SocketChannel channel = (SocketChannel) key.channel();
		SocketAddress clientAddress  = channel.getRemoteAddress();//Pego o endereço remoto do equipamento
		Integer idEquipaments = equipaments.get(clientAddress);//Busca o id do equipamento associado ao enderço remoto
		if(idEquipaments == null) return;//Caso o equipamento solicite a leitura mas nao tenha sido identificado
		switch(idEquipaments) {
			case 4:
				if(statusAquecedor == false && temperaturaLida < limiarInfTemperatura) {//Se atuador estiver desligado e temperaturaLida estiver a baixo do limiar
					System.out.println("Servidor informando ao aquecedor para ligar!");
					try {//Tenta enviar os dados para o aquecedor
						ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
						aquecedor.write(msg);
						statusAquecedor = true;//Significa que foi enviado a msg para o atuador se ligar
						
					}catch(Exception e) {/*Se der problema no envio da mensagem o status permanece desativado*/
						System.out.println("Aquecedor nao se encontra conectado");
					}
				}else if(statusAquecedor == true && temperaturaLida >= limiarSupTemperatura) {//Se atuador estiver ligado
					System.out.println("Servidor informando ao aquecedor para desligar!");
					try {//Tenta enviar os dados para o aquecedor
						ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
						aquecedor.write(msg);
						statusAquecedor = false;
					}catch(Exception e) {
						System.out.println("Aquecedor nao se encontra conectado");
					}
				}
				break;
			case 5:
				if(statusResfriador == false && temperaturaLida > limiarSupTemperatura) {//Se atuador estiver desligado e temperaturaLida estiver a cima do limiar
					System.out.println("Servidor informando ao resfriador para ligar!");
					try {//Tenta enviar os dados para o resfriador
						ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
						resfriador.write(msg);
						statusResfriador = true;//Significa que foi enviado a msg para o atuador se ligar
					}catch(Exception e) {/*Se der problema no envio da mensagem o status permanece desativado*/
						System.out.println("Resfriador nao se encontra conectado");
					}
				}else if(statusResfriador == true && temperaturaLida <= limiarInfTemperatura) {//Se atuador estiver ligado
					System.out.println("Servidor informando ao resfriador para desligar!");
					try {//Tenta enviar os dados para o resfriador
						ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
						resfriador.write(msg);
						statusResfriador = false;
					}catch(Exception e) {
						System.out.println("Resfriador nao se encontra conectado");
					}
				}
				break;
			case 6:
				if(statusIrrigador == false && umidadeSoloLida < limiarInfUmidade) {//Liga
					System.out.println("Servidor informando ao irrigador para ligar!");
					try {
						ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
						irrigador.write(msg);
						statusIrrigador = true;
					}catch(Exception e) {
						System.out.println("Irrigador nao se encontra conectado");
					}
				}else if(statusIrrigador == true && umidadeSoloLida >= limiarSupUmidade) {//desliga
					System.out.println("Servidor informando ao Irrigador para desligar");
					try {
						ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
						irrigador.write(msg);
						statusIrrigador = false;
					}catch(Exception e) {
						System.out.println("Irrigador nao se encontra conectado");
					}
				}
				
				break;
			case 7:
				if(statusInjetorCO2 == false && co2Lido < limiarInfCO2) {
					System.out.println("Servidor informando ao injetor para ligar!");
					try {
						ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
						injetorCO2.write(msg);
						statusInjetorCO2 = true;
					}catch(Exception e) {
						System.out.println("Irrigador nao se encontra conectado");
					}
				}else if(statusInjetorCO2 == true && co2Lido >= limiarSupCO2) {
					System.out.println("Servidor informando ao injetor para desligar!");
					try {
						ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
						injetorCO2.write(msg);
						statusInjetorCO2 = false;
					}catch(Exception e) {
						System.out.println("Irrigador nao se encontra conectado");
					}
				}
				break;
			case 8:

				if(msgCliente != null && msgCliente.position() != 0) {//Se houver mensagem a ser analisada
					/* Observacao importante na implementacao: ByteBuffer foi alocado com 256bytes, 
					 * Se for retornado um simples array pro construtor(new String(msgCliente.array())) do Objeto String ele repassa todos os bytes
					 * E preenche todo o restante dos 256bytes com vazio, a conversao de String para Integer tendo no final vazios
					 * Da problema de formatacao, assim foi alterado o construtor para lidar com isto*/
					String msg = new String(msgCliente.array(), 0, msgCliente.position());
					msgCliente = null;
					//System.out.println("Pedido do cliente:" + msg.length());
					Integer solicitacao = analisaPedidoCliente( msg.substring(2, msg.length()));
					
					ByteBuffer envio;
					switch(solicitacao) {
						case 1:
							envio = ByteBuffer.wrap("Comando efetuado!".getBytes());
							cliente.write(envio);
							break;
						case 2:
							envio = ByteBuffer.wrap("Comando efetuado!".getBytes());
							cliente.write(envio);
							break;
						case 3:
							envio = ByteBuffer.wrap("Comando efetuado!".getBytes());
							cliente.write(envio);
							break;
						case 4:
							envio = ByteBuffer.wrap("Comando efetuado!".getBytes());
							cliente.write(envio);
							break;
						case 5:
							envio = ByteBuffer.wrap(temperaturaLida.toString().getBytes());//Envia a temperatura
							cliente.write(envio);
							break;
						case 6:
							envio = ByteBuffer.wrap(co2Lido.toString().getBytes());
							cliente.write(envio);
							break;
						case -1:
							envio = ByteBuffer.wrap("Comando nao reconhecido!".getBytes());
							cliente.write(envio);
							break;
					}
				}
				break;
		}
	}
	
	private static boolean comparaPedido(String pedidoCliente, String pedidoDisponivel) {
		Integer tamPedido = pedidoCliente.length();
		Integer tamPedidoDisponivel = pedidoDisponivel.length();
		return (tamPedido >= tamPedidoDisponivel && pedidoCliente.substring( 0, tamPedidoDisponivel ).equalsIgnoreCase(pedidoDisponivel));
	}

	private static Integer analisaPedidoCliente(String msg) {
		String pedido1 = "alterar limite superior CO2:";
		String pedido2 = "alterar limite inferior CO2:";
		String pedido3 = "alterar limite superior temperatura:";
		String pedido4 = "alterar limite inferior temperatura:";
		String pedido5 = "consultar temperatura";
		String pedido6 = "consultar CO2";
		try{
			if(comparaPedido(msg, pedido1)) {
				System.out.println("Alterado limite superior do CO2!");
				limiarSupCO2 = Integer.parseInt(msg.substring(pedido1.length(), msg.length()));
				return 1;
			}else if(comparaPedido(msg, pedido2)) {
				System.out.println("Alterado limite inferior do CO2!");
				limiarInfCO2 = Integer.parseInt(msg.substring(pedido2.length(), msg.length()));
				return 2;
			}else if(comparaPedido(msg, pedido3)) {
				System.out.println("Alterado limite superior da temperatura!");
				limiarSupTemperatura = Integer.parseInt(msg.substring(pedido3.length(), msg.length()));
				return 3;
			}else if(comparaPedido(msg, pedido4)) {
				System.out.println("Alterado limite inferior da temperatura!");
				limiarInfTemperatura = Integer.parseInt(msg.substring(pedido3.length(), msg.length()));
				return 4;
			}else if(comparaPedido(msg, pedido5)) {
				return 5;
			}else if(comparaPedido(msg, pedido6)) {
				return 6;
			}
			
			return -1;
		}catch(NumberFormatException e) {
			return -1;//Informa ao cliente que a formatacao esta incorreta
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