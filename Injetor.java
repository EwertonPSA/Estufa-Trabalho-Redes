import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Injetor {
	private InetSocketAddress hostAddress = null;
	private SocketChannel client = null;
	private boolean statusRegistro;
	private String header;
	private String idEquipamento = "7";
	private CO2 ambiente;

	public Injetor() throws IOException {
		this.ambiente = new CO2();
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		this.statusRegistro = false;
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
			
			if(bytesRead == 1 && msgGerenciador[0] == '2') {
				System.out.println("Injetor foi identificado pelo servidor ");
				this.statusRegistro = true;
			}else if(bytesRead == 2) {
				/* Se um equipamento que foi cadastrado antes no Gerenciador e for conectado
				 * Novamente (desligo o processo e religo), pode ocorrer de a mensagem no canal vir muito rapido 
				 * E ser interpretado como uma unica mensagem(registro + sinal de ligar equipamento), assim o comando de ativacao do equipamento eh tratado nessa etapa,
				 * Pois o gerenciador soh informa uma vez para ligar o equipamento(como eh tcp ele tem certeza que chegou a msg)*/
				if(msgGerenciador[0] == '2') {/*Identificacao*/
					System.out.println("Injetor foi identificado pelo servidor ");
					this.statusRegistro = true;
				}
				
				if(this.statusRegistro == true && msgGerenciador[1] == '5') {//Comando de desativacao do equipamento
					System.out.println("Injetor desativado!");
					CO2.setContribuicaoCO2(0);
				}else if(this.statusRegistro == true && msgGerenciador[1] == '4') {//Comando de ativacao do equipamento
					System.out.println("Injetor ativado!");
					CO2.setContribuicaoCO2(2);
				}
			}else {
				throw new RuntimeException("Problema de registro com o servidor");
			}
		}catch(Exception e) {
			throw new RuntimeException("Problema no registro do equipamento");
		}
	}

	public SocketChannel getClient() {
		return client;
	}

	public void communicate(){
		int bytesRead = 0;
		byte[] msgGerenciador;
		while(client.isConnected()) {/*Enquanto a conexao nao estiver fechada*/
			ByteBuffer newBuff = ByteBuffer.allocate(256);
			try {
				System.out.println("Aguardando mensagem do Gerenciador..");
				do {
					bytesRead = client.read(newBuff);
				}while(bytesRead <= 0);
				msgGerenciador = newBuff.array();
				if(this.statusRegistro == true && msgGerenciador[0] == '5') {//Comando de desativacao do equipamento
					System.out.println("Injetor desativado!");
					CO2.setContribuicaoCO2(0);
				}else if(this.statusRegistro == true && msgGerenciador[0] == '4') {
					System.out.println("Injetor ativado!");
					CO2.setContribuicaoCO2(2);
				}
			} catch (IOException e) {
				System.out.println("Servidor foi desconectado, desligando equipamento!");
				return;
			}
		}
	}

	public static void main(String[] argc) throws UnknownHostException, IOException, InterruptedException{
		Injetor atuador = null;
		try{
			atuador = new Injetor();
			atuador.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com o Gerenciador!");
		}
	}
}
