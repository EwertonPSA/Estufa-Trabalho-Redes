import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Cliente {
	public InetSocketAddress hostAddress = null;
	public SocketChannel client = null;
	private String idCliente = "8";
	
	/* Inicializa a comunicacao do sensor de temperatura com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informande que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public Cliente() throws IOException{
		Scanner teclado = new Scanner(System.in);
		String header;
		
		ByteBuffer msgServer;
		
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";
		client.write(ByteBuffer.wrap((header + idCliente).getBytes()));//Manda a mensagem de Identificacao: header + id
		
		msgServer = ByteBuffer.allocate(256);		
		int bytesRead = 0;
		do {
			bytesRead = client.read(msgServer);
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		
		byte msgServerByte[] = msgServer.array();
		if(msgServerByte[0] == '2') {
			System.out.println("Cliente foi identificado pelo gerenciador!");
		}else {
			throw new RuntimeException("Problema no registro no gerenciador!");
		}
	}
	
	/* Pega um valor inteiro e passa pra um vetor de char
	 * Ele eh usado para obter a representacao correta do inteiro em 4 bytes
	 * No qual deve ser incluido no corpo da mensagem a ser enviada pro servidor*/
	private String intToChar(int temperaturaInt) {
		int aux = temperaturaInt;
		byte[] seqNumero = new byte[4];
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (byte) (aux>>(i*8) & 0xff);
		}
		String r = new String(seqNumero);
		return r;
	}
	
	
	private static Integer byteToInt(int position, byte[] arr) {
		int num = 0;
		for(int i = 3; i >= 0; i--) {
			num = num<<8;
			num = num + (int)(arr[i+position]&0xff);
		}
		return num;
	}

	public void communicate() throws InterruptedException {
		Scanner teclado = new Scanner(System.in);
		String header = "6";
		String tipoParametro;
		int minVal = 0, maxVal = 10;
		int bytesRead = 0;
		String msg;
		String resposta;

		while(true) {
			ByteBuffer bufferWrite = ByteBuffer.allocate(256);
			ByteBuffer buffRead = ByteBuffer.allocate(256);
			
			// Menu do usuario			
			System.out.println( 
				"   1 - Configurar limiares de temperatura\n" +
				"   2 - Configurar limiares de umidade\n" +
				"   3 - Configurar limiares de CO2\n" +
				"   4 - Checar temperatura\n" +
				"   5 - Checar nivel de umidade\n" +
				"   6 - Checar nivel de CO2\n" +
				"Insira o valor [1-6] correspondente ao comando: "
			);
			
			int comando;
			try {
				// Le comando e parametros
				comando = teclado.nextInt();
				if(comando >= 1 && comando <= 3) {
					System.out.println("Valor minimo: ");
					minVal = teclado.nextInt();				
					System.out.println("Valor maximo: ");
					maxVal = teclado.nextInt();				
					header = "6";
					if(maxVal < minVal) {
						System.out.println("Valores invalidos!");
						continue;
					}
					
				} else if(comando >= 4 && comando <= 6) {
					header = "7";
				} else {
					System.out.println("Valor invalido!");
					continue;
				}
			}catch(InputMismatchException e) {
				System.out.println("Valores de configuracoes invalidos!");
				teclado.next();
				continue;
			}
			
			// Monta a mensagem para o gerenciador
			tipoParametro = Integer.toString((((comando-1)%3)+1));
			
			
			if(header == "6") {
				msg = header + tipoParametro +  intToChar(minVal) + intToChar(maxVal);
			} else {
				msg = header + tipoParametro;
			}
			
			try {
				bufferWrite = ByteBuffer.wrap(msg.getBytes());
				client.write(bufferWrite);
				bufferWrite.clear();
				
				do {
					bytesRead = client.read(buffRead);
				}while(bytesRead <= 0);
				
				resposta = new String(buffRead.array(), 0, buffRead.position());
				byte[] arr = buffRead.array();
				System.out.println("Resposta do Gerenciador: ");
				
				// Interpreta a resposta de acordo com a mensagem enviada anteriormente
				// Obs: "header" corresponde ao header da mensagem enviada pelo CLIENTE
				if(header == "6") {
					System.out.println(resposta);
				} else if(header == "7") {
					if(arr[0] == '8') {
						char tipo = (char)arr[1];
						int valor = byteToInt(2, arr);
						switch(tipo) {
						case '1':
							System.out.println("Temperatura: " + valor + "°C");
							break;
						case '2':
							System.out.println("Umidade do Solo: " + valor + "%");
							break;
						case '3':
							System.out.println("Nivel de CO2: " + valor + " ppmv");
							break;
						}
					}
				}
				
				buffRead.clear();
			}catch(Exception e) {
				e.printStackTrace();
				System.out.println("Servidor desligado, desligando conexao!");
				try {
					client.close();
				} catch (IOException e1) {;}
				teclado.close();
				return;
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException{
		try {
			Cliente cliente = new Cliente();
			cliente.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com gerenciador!");
		}
	}
}
