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
	private Temperatura temperatura = null;
	private boolean statusEquip;
	private String header;
	private String idEquipamento = "4";

	public Aquecedor() throws IOException {
		try {
			temperatura = new Temperatura();
		}catch(Exception e) {
			System.out.println("Problema ao criar arquivo de temperatura!");
			return;
		}
		this.setStatusEquip(false);//Equipamento inicia desligado
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
	}
	
	private boolean getStatusEquip() {
		return statusEquip;
	}
	
	private void setStatusEquip(boolean statusEquip) {
		this.statusEquip = statusEquip;
	}

	private Integer trataTemperatura(Integer temperaturaAtual) {
		return temperaturaAtual+1;
	}
	
	/*Faz a alteracao da temperatura */
	public void updateTemperatura() {
		try {/*Le o arquivo, pega a temperatura atual e aumenta*/
			FileReader fr = new FileReader(temperatura.getArqTemperatura());
			BufferedReader buffRead = new BufferedReader(fr);
			Integer temperaturaAtual = Integer.parseInt(buffRead.readLine());//Le a linha e repassa para inteiro
			System.out.println("Lido no arquivo: " + temperaturaAtual);
			
			temperaturaAtual = trataTemperatura(temperaturaAtual);
			
			if(!this.isInterrupted()) {/*Se a thread nao for interrompida*/
				System.out.println("Escrevendo no arquivo: " + temperaturaAtual);
				FileWriter fw = new FileWriter(temperatura.getArqTemperatura());
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(temperaturaAtual.toString() + '\n');
				buffWrite.close();
			}
		} catch (IOException e) {
			System.out.println("Problema de escrita no arquivo!");
			return;
		}
	}


	public SocketChannel getClient() {
		return client;
	}
	
	@Override
	public void run() {
		System.out.println("Atuador ligado!");
		while(!this.isInterrupted()) {
			try {
				TimeUnit.SECONDS.sleep(1);
				if(getStatusEquip())//Se o equipamento estiver ativo
					updateTemperatura();
			} catch (InterruptedException e1) {
				return;
			}
		}
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
					setStatusEquip(false);//desativa equipamento
				}else if(msgGerenciador[0] == '4') {
					setStatusEquip(true);//ativa equipamento
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
			atuador.setStatusEquip(true);
			in.next();
			atuador.setStatusEquip(false);
			in.next();
			atuador.setStatusEquip(true);
			in.next();
			//atuador.communicate();
		}catch(Exception e) {
			System.out.println("Deu problema no atuador");
			e.printStackTrace();
		}
	}
}