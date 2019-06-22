import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Temperatura extends Thread{
	private static String pathTemperatura = "temperatura.txt";/*arquivo que simula temperatura*/
	private static String pathContribuicaoAquecedor = "contribuicaoAquecedor.txt";
	private static String pathContribuicaoResfriador = "contribuicaoResfriador.txt";
	private static String pathTemperaturaAmbiente = "temperaturaAmbiente.txt";
	private static File arqTemperatura = null;
	private static File arqContribuicaoAquecedor = null;
	private static File arqContribuicaoResfriador = null;
	private static File arqTemperaturaAmbiente = null;
	private static int contribuicaoTemperaturaAmbiente = 0;
	private static int TemperaturaAmbiente = 0;

	/* Metodo que retorna o arquivo de temperatura para lida ou escrita*/
	public static File getArqTemperatura() throws IOException {
		if(arqTemperatura == null)
			createFileTemperatura();
		return arqTemperatura;
	}
	
	/* Metodo que retorna o arquivo de contribuicao para lida ou escrita*/
	public static File getArqContribuicaoAquecedor() throws IOException {
		if(arqContribuicaoAquecedor == null)
			createFileContribuicaoAquecedor();
		return arqContribuicaoAquecedor;
	}
	
	public static File getArqContribuicaoResfriador() throws IOException {
		if(arqContribuicaoResfriador == null)
			createFileContribuicaoResfriador();
		return arqContribuicaoResfriador;
	}
	
	public static File getArqTemperaturaAmbiente() throws IOException {
		if(arqTemperaturaAmbiente == null)
			createFileTemperaturaAmbiente();
		return arqTemperaturaAmbiente;
	}
	
	/* Esse metodo eh utilizado apenas pelos atuadores e gerenciador
	 * O gerenciador utiliza para resetar os status do equipamento(quando desligado ou iniciado) e os atuadores para simulacao
	 * Atraves dele o fator de contribuicao(que afeta a temperatura ambiente)
	 * Eh alterado pelo arquivo contribuicao.txt
	 * Apenas a classe Temperatura tem acesso para leitura e escrita no arquivo*/
	public static void setContribuicaoAquecedor(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoAquecedor());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));
			buffWrite.close();
			//System.out.println("Alterado contribuicao " + alteracao);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void setContribuicaoResfriador(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoResfriador());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));
			buffWrite.close();
			//System.out.println("Alterado contribuicao " + alteracao);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Esse metodo realiza a cricao do arquivo de temperatura(utilizado para simular a temperatura) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileTemperatura() throws IOException {
		Integer defaultTemperatura = 0;
		arqTemperatura = new File(pathTemperatura);
		if(!arqTemperatura.exists()) {
			arqTemperatura.createNewFile();
			FileWriter fw = new FileWriter(getArqTemperatura());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(defaultTemperatura.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
			buffWrite.close();
		}else {
			FileReader fr = new FileReader(getArqTemperatura());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio*/
				FileWriter fw = new FileWriter(arqTemperatura);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(defaultTemperatura.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
				buffWrite.close();
			}
		}
	}
	
	/* Esse metodo realiza a cricao do arquivo de contribuicao(utilizado para simular as contribuicoes dos atuadores na temperatura) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoAquecedor() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoAquecedor = new File(pathContribuicaoAquecedor);
		if(!arqContribuicaoAquecedor.exists()) {
			arqContribuicaoAquecedor.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoAquecedor());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoAquecedor());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoAquecedor);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
		}
	}
	
	private static void createFileContribuicaoResfriador() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoResfriador = new File(pathContribuicaoResfriador);
		if(!arqContribuicaoResfriador.exists()) {
			arqContribuicaoResfriador.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoResfriador());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoResfriador());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoResfriador);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
		}
	}
	
	private static void createFileTemperaturaAmbiente() throws IOException{
		Integer TemperaturaAmbienteDefault = 0;
		arqTemperaturaAmbiente = new File(pathTemperaturaAmbiente);
		if(!arqTemperaturaAmbiente.exists()) {
			arqTemperaturaAmbiente.createNewFile();
			FileWriter fw = new FileWriter(getArqTemperaturaAmbiente());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(TemperaturaAmbienteDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqTemperaturaAmbiente());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqTemperaturaAmbiente);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(TemperaturaAmbienteDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
		}
	}
	
	/* Esse metodo obtem a temperatura atual lendo o arquivo temperatura.txt
	 * E pega o fator de contribuicao do arquivo contribuicao.txt
	 * Tendo esses valores eh aplicado o fator de contribuicao do ambiente
	 * E retornado a temperatura atual*/
	private int updateTemperatura() throws FileNotFoundException, IOException{
		FileReader fr = new FileReader(getArqTemperatura());
		BufferedReader buffRead = new BufferedReader(fr);
		Integer contribuicaoTemperaturaEquip;
		/*Lendo a temperatura do arquivo*/
		Integer temperaturaAtual = Integer.parseInt(buffRead.readLine());//Le a linha e repassa para inteiro
		//System.out.println("Lido no arquivo: " + temperaturaAtual);
		
		fr = new FileReader(getArqTemperaturaAmbiente());
		buffRead = new BufferedReader(fr);
		TemperaturaAmbiente = Integer.parseInt(buffRead.readLine());
		
		
		if(temperaturaAtual < TemperaturaAmbiente)
			contribuicaoTemperaturaAmbiente = 1;
		else if(temperaturaAtual > TemperaturaAmbiente)
			contribuicaoTemperaturaAmbiente = -1;
		else
			contribuicaoTemperaturaAmbiente = 0;
		
		
		/*Lendo contribuicao dos equipamentos no arquivo*/
		fr = new FileReader(getArqContribuicaoAquecedor());
		buffRead = new BufferedReader(fr);
		Integer contribuicaoAquecedor = Integer.parseInt(buffRead.readLine());//Atualiza a contribuicao do equipamento
		
		fr = new FileReader(getArqContribuicaoResfriador());
		buffRead = new BufferedReader(fr);
		Integer contribuicaoResfriador = Integer.parseInt(buffRead.readLine());//Atualiza a contribuicao do equipamento
		
		return temperaturaAtual + contribuicaoTemperaturaAmbiente + contribuicaoAquecedor + contribuicaoResfriador;
		
		
	}
	
	@Override
	public void run() {
		Integer temperaturaAtual;
		FileWriter fw = null;
		BufferedWriter buffWrite = null;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(1);
				temperaturaAtual = updateTemperatura();
				//System.out.println("Escrevendo no arquivo: " + temperaturaAtual);
				fw = new FileWriter(getArqTemperatura());
				buffWrite = new BufferedWriter(fw);
				buffWrite.append(temperaturaAtual.toString() + '\n');
				buffWrite.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				System.out.println("Problema na formatacao dos dados da temperatura");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
}
