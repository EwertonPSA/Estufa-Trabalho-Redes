import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class SensorTemperatura extends Thread{
	public InetSocketAddress hostAddress = null;
	public SocketChannel client = null;
	private String idEquipamento = "1";
	private String header;
	
	public SensorTemperatura() throws IOException{
		byte msgServerByte[];
		ByteBuffer msgServer;
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		msgServer = ByteBuffer.allocate(256);
		int bytesRead = 0;
		do {
			bytesRead = client.read(msgServer);
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		msgServerByte = msgServer.array();
		if(msgServerByte[0] == '2') {
			System.out.println("Equipamento registrado");
		}else {
			throw new RuntimeException("Problema no registro do equipamento no servidor!");
		}
	}
	
	private String readFileTemperatura() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(Temperatura.getArqTemperatura());
		BufferedReader buffRead = new BufferedReader(fr);
		String temperatura =  buffRead.readLine();//Le a linha e repassa para inteiro
		int temperaturaInt = Integer.parseInt(temperatura);
		char[] sequenciaNumero = intToChar(temperaturaInt);
		return String.valueOf(sequenciaNumero[0]) + String.valueOf(sequenciaNumero[1]) + String.valueOf(sequenciaNumero[2]) + String.valueOf(sequenciaNumero[3]);
	}
	
	private char[] intToChar(int temperaturaInt) {
		int aux = temperaturaInt;
		char[] seqNumero = new char[4];
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (char) ((int)aux>>(i*8) & (int)0xFF);
		}
		return seqNumero;
	}
	
	private Integer charToInt(char[] seqNumero) {
		int aux2 = 0;
		for(int i = 3; i >= 0; i--) {
			aux2 = aux2<<8;
			aux2 = aux2 + seqNumero[i];
		}
		return aux2;
	}

	public void communicate() throws InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		int byteRead = 0;
		String msgSensor;
		header = "3";
		
		while(true) {
			TimeUnit.SECONDS.sleep(1);
			try {/*Leitura do arquivo de temperatura*/
				msgSensor = header + idEquipamento + readFileTemperatura();//Header + id + temperatura
			}catch(Exception e) {
				System.out.println("Problema ao abrir o arquivo para leitura!");
				return;
			}
			
			try {/*Envia a temperatura pro gerenciador*/
				buffer = ByteBuffer.wrap(msgSensor.getBytes());
				client.write(buffer);
				buffer.clear();
			}catch(Exception e) {
				System.out.println("Servidor desligado, desligando sensor!");
				try {
					client.close();
				} catch (IOException e1) {;}
				return;
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException{
		try {
			SensorTemperatura sensor = new SensorTemperatura();
			sensor.communicate();
		}catch(Exception e) {;}
	}
}
