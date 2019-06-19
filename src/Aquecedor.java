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

/* CORRIGIR: Nao precisa mais escrever no arquivo, basta ir na temperatura
 * E adicionar o fator de contribuicao*/
public class Aquecedor extends Thread{
	private InetSocketAddress hostAddress = null;
	private SocketChannel client = null;
	private boolean statusEquip;
	private boolean statusRegistro;
	private String header;
	private String idEquipamento = "4";

	public Aquecedor() throws IOException {
		this.setStatusEquip(false);//Equipamento inicia desligado
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		this.statusRegistro = false;
		this.statusEquip = false;
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		
		ByteBuffer newBuff = ByteBuffer.allocate(256);
		int bytesRead;
		byte[] msgGerenciador;
		try {
			System.out.println("Aguardando mensagem do Gerenciador..");
			do {
				bytesRead = client.read(newBuff);
			}while(bytesRead <= 0);
			msgGerenciador = newBuff.array();
			if(msgGerenciador[0] == '2') {
				System.out.println("Aquecedor foi identificado pelo servidor");
				this.statusRegistro = true;
				this.start();
			}else {
				throw new RuntimeException("Problema no registro do equipamento");
			}
		}catch(Exception e) {
			throw new RuntimeException("Problema no registro do equipamento");
		}
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
			FileReader fr = new FileReader(Temperatura.getArqTemperatura());
			BufferedReader buffRead = new BufferedReader(fr);
			Integer temperaturaAtual = Integer.parseInt(buffRead.readLine());//Le a linha e repassa para inteiro
			System.out.println("Lido no arquivo: " + temperaturaAtual);
			
			temperaturaAtual = trataTemperatura(temperaturaAtual);
			
			if(!this.isInterrupted()) {/*Se a thread nao for interrompida*/
				System.out.println("Escrevendo no arquivo: " + temperaturaAtual);
				FileWriter fw = new FileWriter(Temperatura.getArqTemperatura());
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(temperaturaAtual.toString() + '\n');
				buffWrite.close();
			}
		} catch (IOException e) {
			System.out.println("Problema de escrita no arquivo!");
			e.printStackTrace();
			return;
		}
	}


	public SocketChannel getClient() {
		return client;
	}
	
	@Override
	public void run() {
		while(!this.isInterrupted()) {
			System.out.println("aqui");
			try {
				TimeUnit.SECONDS.sleep(1);
				if(getStatusEquip())//Se o equipamento estiver ativo
					updateTemperatura();
			} catch (InterruptedException e1) {
				System.out.println("aqui2");
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
				
				if(this.statusRegistro == true && msgGerenciador[0] == '5') {//Comando de desativacao do equipamento
					System.out.println("Aquecedor desativado!");
					setStatusEquip(false);//desativa equipamento
				}else if(this.statusRegistro == true && msgGerenciador[0] == '4') {
					System.out.println("Aquecedor ativado!");
					setStatusEquip(true);//ativa equipamento
				}
			} catch (IOException e) {
				this.interrupt();
				System.out.println("Servidor foi desconectado, desligando equipamento!");
				return;
			}
		}
	}

	public static void main(String[] argc) throws UnknownHostException, IOException, InterruptedException{
		Aquecedor atuador = null;
		try{
			atuador = new Aquecedor();
			atuador.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com o Gerenciador!");
		}
	}
}