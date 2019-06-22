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
	public InetSocketAddress hostAddress = null;	//endereco IP local host
	public SocketChannel client = null;	//socket
	private String idEquipamento = "3";	//id do sensor
	private String header;	//header das mensagens
	
	/* Inicializa a comunicacao do sensor de co2 com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informe que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public SensorCO2() throws IOException{
		byte msgServerByte[];
		ByteBuffer msgServer;
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);	//cria um host de acesso
		this.client = SocketChannel.open(hostAddress);	//conexao com o IP e a porta
		this.client.configureBlocking(false);	//configura socket como nao-bloqueante
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";	//header da mensagem de identificacao
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		msgServer = ByteBuffer.allocate(256);
		//aguarda resposta do gerenciador
		int bytesRead = 0;
		do {
			bytesRead = client.read(msgServer);
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		msgServerByte = msgServer.array();
		if(msgServerByte[0] == '2') {	//se for confirmacao da identificacao
			System.out.println("Equipamento registrado");
		}else {
			throw new RuntimeException("Problema no registro do equipamento no servidor!");
		}
	}
	
	//retorna a leitura do nivel de CO2
	private String readFileCO2() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(CO2.getArqCO2());	//arquivo do CO2
		BufferedReader buffRead = new BufferedReader(fr);	//leitor
		int CO2Int = Integer.parseInt(buffRead.readLine());//Le como string, passa pra inteiro
		String sequenciaNumero = intToChar(CO2Int);//Obtem o inteiro como representacao em string (bytes do inteiro como caracteres)
		System.out.println("Leitura de CO2:" + CO2Int + "ppm");
		buffRead.close();
		return sequenciaNumero;
	}
	
	/* Pega um valor inteiro e passa pra uma string com os bytes do inteiro como caracteres
	 * Ele eh usado para obter a representacao correta do inteiro em 4 bytes
	 * No qual deve ser incluido no corpo da mensagem a ser enviada pro servidor*/
	private String intToChar(int temperaturaInt) {
		int aux = temperaturaInt;
		byte[] seqNumero = new byte[4];		//salva os bytes do inteiro
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (byte) (aux>>(i*8) & 0xff);	//separa os bytes com bit shift e operacao and
		}
		String r = new String(seqNumero);	//transforma os bytes em string
		return r;
	}

	//envia a leitura do nivel de CO2 a cada segundo
	public void communicate() throws InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		String msgSensor;
		header = "3";	//header da mensagem de envio de leitura
		
		while(true) {
			TimeUnit.SECONDS.sleep(1);
			try {//monta a mensagem
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
