import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class SensorTemperatura extends Thread{
	public InetSocketAddress hostAddress = null;	//endereco IP local host
	public SocketChannel client = null;	//socket
	private String idEquipamento = "1";	//id do sensor
	private String header;	//header da mensagem
	
	/* Inicializa a comunicacao do sensor de temperatura com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informande que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public SensorTemperatura() throws IOException{
		byte msgServerByte[];
		ByteBuffer msgServer;
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);	//cria um host de acesso
		this.client = SocketChannel.open(hostAddress);	//conexao com o IP e a porta
		this.client.configureBlocking(false);	//configura socket como nao bloqueante
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";	//header da mensagem de identificacao
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		msgServer = ByteBuffer.allocate(256);
		int bytesRead = 0;

		do {
			bytesRead = client.read(msgServer);	//salva mensagem em msgServer
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		msgServerByte = msgServer.array();	//passa mensagem para array de bytes
		if(msgServerByte[0] == '2') {	//mensagem de confirmacao de identificacao
			System.out.println("Equipamento registrado");
		}else {
			throw new RuntimeException("Problema no registro do equipamento no servidor!");
		}
	}
	
	// sensor faz a leitura da temperatura da estufa
	private String readFileTemperatura() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(Temperatura.getArqTemperatura());	//arquivo que contem a temperatura
		BufferedReader buffRead = new BufferedReader(fr);	//leitor do arquivo
		int temperaturaInt = Integer.parseInt(buffRead.readLine());//Le como string, passa pra inteiro
		//Obtem o inteiro como representacao em string (com os bytes do inteiro como caracteres)
		String sequenciaNumero = intToChar(temperaturaInt);
		System.out.println("Leitura de temperatura:" + temperaturaInt + "°C");
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

	//envia as leituras de temperatura a cada segundo
	public void communicate() throws InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		String msgSensor;
		header = "3";	//header da mensagem de envio de leitura
		
		while(true) {
			TimeUnit.SECONDS.sleep(1);
			try {//cria a mensagem para enviar a leitura
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
