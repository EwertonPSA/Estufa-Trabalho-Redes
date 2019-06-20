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
	private String header;
	private Scanner teclado;
	
	/* Inicializa a comunicacao do sensor de temperatura com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informande que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public Cliente() throws IOException{
		teclado = new Scanner(System.in);
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
		int bytesRead = 0;
		String msg;
		String resposta;
		header = "3";

		while(true) {
			ByteBuffer bufferWrite = ByteBuffer.allocate(256);
			ByteBuffer buffRead = ByteBuffer.allocate(256);
			
			msg = header + idCliente + teclado.nextLine();
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
				return;
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException{
		try {
			Cliente sensor = new Cliente();
			sensor.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com gerenciador!");
		}
	}
}
