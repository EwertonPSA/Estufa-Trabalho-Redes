import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class SensorCO2 {
	public InetSocketAddress hostAddress = null;
	public SocketChannel client = null;
	private String idEquipamento = "3";
	private String header;
	
	/* Inicializa a comunicacao do sensor de co2 com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informande que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public SensorCO2() throws IOException{
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
	
	/*Retorna a mensagem como uma sequencia de string ja formatada para o envio no corpo da mensagem pro gerenciador*/
	private String readFileCO2() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(CO2.getArqCO2());
		BufferedReader buffRead = new BufferedReader(fr);
		int CO2Int = Integer.parseInt(buffRead.readLine());//Le como string, passa pra inteiro
		String sequenciaNumero = intToChar(CO2Int);//Obtem o inteiro como representacao em vetor de char
		System.out.println("Leitura de CO2:" + CO2Int + "ppm");
		return sequenciaNumero;
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

	public void communicate() throws InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		int byteRead = 0;
		String msgSensor;
		header = "3";
		
		while(true) {
			TimeUnit.SECONDS.sleep(1);
			try {/*Leitura do arquivo de co2*/
				msgSensor = header + idEquipamento + readFileCO2();//Header + id + co2
			}catch(Exception e) {
				System.out.println("Problema ao abrir o arquivo para leitura!");
				return;
			}
			
			try {/*Envia o co2 pro gerenciador*/
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
			SensorCO2 sensor = new SensorCO2();
			sensor.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com gerenciador!");
		}
	}

}
