import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
		byte msgServerByte[];
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
		msgServerByte = msgServer.array();
		if(msgServerByte[0] == '2') {
			System.out.println("Cliente foi identificado pelo gerenciador!");
		}else {
			throw new RuntimeException("Problema no registro no gerenciador!");
		}
	}
	
	/* Pega um valor inteiro e passa pra um vetor de char
	 * Ele eh usado para obter a representacao correta do inteiro em 4 bytes
	 * No qual deve ser incluido no corpo da mensagem a ser enviada pro servidor*/
	private char[] intToChar(int temperaturaInt) {
		int aux = temperaturaInt;
		char[] seqNumero = new char[4];
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (char) ((int)aux>>(i*8) & (int)0xFF);
		}
		return seqNumero;
	}

	public void communicate() throws InterruptedException {
		Scanner teclado = new Scanner(System.in);
		String header = "6";
		char tipoParametro;
		int minVal = 0, maxVal = 10;
		int bytesRead = 0;
		String msg;
		String resposta;

		while(true) {
			ByteBuffer bufferWrite = ByteBuffer.allocate(256);
			ByteBuffer buffRead = ByteBuffer.allocate(256);
			
			// Menu do usuario			
			System.out.println( "   1 - Configurar limiares de temperatura\n" +
							"   2 - Configurar limiares de CO2\n" +
							"   3 - Confugurar limiares de umidade\n" +
							"   4 - Checar temperatura\n" +
							"   5 - Checar nivel de CO2\n" +
							"   6 - Checar nivel de umidade\n" +
							"Insira o valor [1-6] correspondente ao comando: ");
			
			// Le comando e parametros
			int comando = teclado.nextInt();
			if(comando >= 1 && comando <= 3) {
				System.out.println("Valor minimo: ");
				minVal = teclado.nextInt();
				
				System.out.println("Valor maximo: ");
				maxVal = teclado.nextInt();
				
				header = "6";
			} else if(comando >= 4 && comando <= 6) {
				header = "7";
			} else {
				System.out.println("Valor invalido!\n");
				continue;
			}
			
			// Monta a mensagem para o gerenciador
			tipoParametro = (char)((comando%3)+1);			
			if(header == "6") {
				msg = header + tipoParametro + intToChar(minVal).toString() + intToChar(maxVal).toString();
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
				System.out.println("Resposta do Gerenciador: " + resposta );
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
			
			teclado.close();
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
