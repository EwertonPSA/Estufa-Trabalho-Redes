import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.concurrent.TimeUnit;

public class Aquecedor extends Thread{
	private InetSocketAddress hostAddress = null;
	private SocketChannel client = null;
	private String path = "temperatura.txt";/*arquivo que simula temperatura*/
	private File arqTemperatura;
	
	/* Cria o arquivo que simula temperatura*/
	private void createFile() throws IOException {
		arqTemperatura = new File(path);
	}
	

	private Integer trataTemperatura(Integer temperaturaAtual) {
		return temperaturaAtual+1;
	}
	
	/*Faz a alteracao da temperatura */
	public void updateTemperatura() {
		
		if(arqTemperatura == null) {
			try{
				createFile();
			}catch(Exception e) {
				System.out.println("Problema ao criar arquivo de temperatura!");
				return;
			}
		}
		
		try {
			FileReader fr = new FileReader(arqTemperatura);
			BufferedReader buffRead = new BufferedReader(fr);
			Integer temperaturaAtual = Integer.parseInt(buffRead.readLine());//Le a linha e repassa para inteiro
			System.out.println("Lido no arquivo: " + temperaturaAtual);
			
			temperaturaAtual = trataTemperatura(temperaturaAtual);
			
			if(!this.isInterrupted()) {/*Se a thread nao for interrompida*/
				System.out.println("Escrevendo no arquivo: " + temperaturaAtual);
				FileWriter fw = new FileWriter(arqTemperatura);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(temperaturaAtual.toString() + '\n');
				buffWrite.close();
			}
		} catch (IOException e) {
			System.out.println("Problema de escrita no arquivo!");
			return;
		}
	}



	public Aquecedor() throws IOException {
		try {
			createFile();
		}catch(Exception e) {
			System.out.println("Problema ao criar arquivo de temperatura!");
			return;
		}
		
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
	
	/* A Thread eh executada toda vez que o gerenciador informar
	 * Para o equipamento estar ligado
	 * A Thread cuida apenas de escrever dados no arquivo para
	 * Simular a temperatura
	 * A Thread eh interrompida toda vez que o gerenciador
	 * Pedir pro equipamento ser desligado*/
	@Override
	public void run() {
		System.out.println("Atuador ligado!");
		while(!this.isInterrupted()) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
				/*Trata interrupcao da thread dentro do sleep*/
				return;
			}
			updateTemperatura();
		}
	}
	
	private void offEquip() {
		this.interrupt();
		System.out.println("Atuador desligado!");
	}

	public void communicate(){
		int bytesRead = 0;
		byte[] msgGerenciador;
		while(client.isConnected()) {/*Enquanto a conexao nao estiver feixada*/
			ByteBuffer newBuff = ByteBuffer.allocate(256);

			try {
				System.out.println("Aguardando mensagem do Gerenciador..");
				do {
					bytesRead = client.read(newBuff);
				}while(bytesRead <= 0);
				msgGerenciador = newBuff.array();
				//msgServer = new String(newBuff.array());
				
				if(msgGerenciador[0] == '2') {
					System.out.println("Aquecedor foi identificado pelo servidor");
				}else if(msgGerenciador[0] == '5') {//Comando de desativacao do equipamento
					offEquip();
				}else if(msgGerenciador[0] == '4') {
					this.start();
				}
			} catch (IOException e) {
				System.out.println("Servidor foi desconectado, desligando equipamento!");
				return;
			}
		}
	}

	public static void main(String[] argc) throws UnknownHostException, IOException, InterruptedException{
		Aquecedor atuador = null;
		try{
			atuador = new Aquecedor();
			atuador.start();
			Scanner in = new Scanner(System.in);
			in.next();
			atuador.offEquip();
			atuador.start();
			in.next();
		}catch(Exception e) {
			System.out.println("Deu problema no atuador");
			e.printStackTrace();
		}
	}
}