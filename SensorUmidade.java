import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class SensorUmidade {
	public InetSocketAddress hostAddress = null;
	public SocketChannel client = null;
	private String idEquipamento = "2";
	private String header;
	
	/* Inicializa a comunicacao do sensor de umidade com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informande que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public SensorUmidade() throws IOException{
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
	private String readFileUmidade() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(UmidadeSolo.getArqUmidade());
		BufferedReader buffRead = new BufferedReader(fr);
		int umidadeInt = Integer.parseInt(buffRead.readLine());//Le como string, passa pra inteiro
		String sequenciaNumero = intToChar(umidadeInt);//Obtem o inteiro como representacao em vetor de char
		System.out.println("Leitura de Umidade:" + umidadeInt + "%");
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
			try {/*Leitura do arquivo de umidade*/
				msgSensor = header + idEquipamento + readFileUmidade();//Header + id + umidade
			}catch(Exception e) {
				System.out.println("Problema ao abrir o arquivo para leitura!");
				return;
			}
			
			try {/*Envia a umidade pro gerenciador*/
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
			SensorUmidade sensor = new SensorUmidade();
			sensor.communicate();
		}catch(Exception e) {;}
	}
}
