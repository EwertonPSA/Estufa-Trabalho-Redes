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
	
	private byte[] readFileTemperatura() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(Temperatura.getArqTemperatura());
		BufferedReader buffRead = new BufferedReader(fr);
		String temperatura =  buffRead.readLine();//Le a linha e repassa para inteiro
		Integer temperaturaInt = Integer.parseInt(temperatura);
		System.out.println(temperaturaInt);
		byte[] sequenciaNumero = intToByte(temperaturaInt);
		System.out.println(sequenciaNumero);
		return sequenciaNumero;
	}
	
	private byte[] intToByte(Integer temperaturaInt) {
		int aux = temperaturaInt;
		byte[] seqNumero = new byte[4];
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (byte)(aux>>(i*8) & 0xff);
		}
		return seqNumero;
	}
	
	private Integer byteToInt(byte[] seqNumero) {
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
