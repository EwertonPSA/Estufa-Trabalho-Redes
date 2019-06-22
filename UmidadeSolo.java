import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UmidadeSolo extends Thread{
	private static String pathUmidade = "umidade.txt";/*arquivo que simula umidade*/
	private static String pathContribuicaoUmidade = "contribuicaoUmidade.txt";
	private static File arqUmidade = null;
	private static File arqContribuicaoUmidade = null;
	private static int contribuicaoUmidadeAmbiente = -1;
	private static int timeUpdadeSolo = 1;

	/* Metodo que retorna o arquivo de umidade para lida ou escrita*/
	public static File getArqUmidade() throws IOException {
		if(arqUmidade == null)
			createFileUmidade();
		return arqUmidade;
	}
	
	/* Metodo que retorna o arquivo de contribuicao de umidade para lida ou escrita*/
	public static File getArqContribuicaoUmidade() throws IOException {
		if(arqContribuicaoUmidade == null)
			createFileContribuicaoUmidade();
		return arqContribuicaoUmidade;
	}
	
	/* Esse metodo eh utilizado apenas pelos atuadores e gerenciador
	 * O gerenciador utiliza para resetar os status do equipamento(quando desligado ou iniciado) e os atuadores para simulacao
	 * Atraves dele o fator de contribuicao de umidade(que afeta a umidade do solo)
	 * Eh alterado pelo arquivo contribuicaoUmidade.txt
	 * Apenas a classe UmidadeSolo tem acesso para lida e escrita no arquivo*/
	public static void setContribuicaoUmidadeEquip(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoUmidade());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));
			buffWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Esse metodo realiza a cricao do arquivo de umidade(utilizado para simular a umidade) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileUmidade() throws IOException {
		Integer defaultUmidade = 30;
		arqUmidade = new File(pathUmidade);
		if(!arqUmidade.exists()) {
			arqUmidade.createNewFile();
			FileWriter fw = new FileWriter(getArqUmidade());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(defaultUmidade.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma umidade Inicial*/
			buffWrite.close();
		}else {
			FileReader fr = new FileReader(getArqUmidade());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio*/
				FileWriter fw = new FileWriter(arqUmidade);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(defaultUmidade.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma umidade Inicial*/
				buffWrite.close();
			}
		}
	}
	
	/* Esse metodo realiza a cricao do arquivo de contribuicao(utilizado para simular as contribuicoes dos atuadores na umidade) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoUmidade() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoUmidade = new File(pathContribuicaoUmidade);
		if(!arqContribuicaoUmidade.exists()) {
			arqContribuicaoUmidade.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoUmidade());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoUmidade());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoUmidade);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
		}
	}
	
	/* Esse metodo obtem a umidade atual lendo o arquivo umidade.txt
	 * E pega o fator de contribuicao do arquivo contribuicao.txt
	 * Tendo esses valores eh aplicado o fator de contribuicao do ambiente
	 * E retornado a umidade atual*/
	private int updateUmidade() throws FileNotFoundException, IOException{
		FileReader fr = new FileReader(getArqUmidade());
		BufferedReader buffRead = new BufferedReader(fr);
		Integer contribuicaoUmidadeEquip ;
		/*Lendo a umidade do arquivo*/
		Integer umidadeAtual = Integer.parseInt(buffRead.readLine());//Le a linha e repassa para inteiro
		//System.out.println("Lido no arquivo: " + umidadeAtual);		
		
		/*Lendo contribuicao dos equipamentos no arquivo*/
		fr = new FileReader(getArqContribuicaoUmidade());
		buffRead = new BufferedReader(fr);
		contribuicaoUmidadeEquip = Integer.parseInt(buffRead.readLine());//Atualiza a contribuicao do equipamento
		return umidadeAtual + contribuicaoUmidadeAmbiente + contribuicaoUmidadeEquip;
	}
	
	@Override
	public void run() {
		Integer umidadeSoloAtual;
		FileWriter fw = null;
		BufferedWriter buffWrite = null;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(timeUpdadeSolo);//Espera 2 segundos pra alterar os valores do solo
				umidadeSoloAtual = updateUmidade();
				fw = new FileWriter(getArqUmidade());
				buffWrite = new BufferedWriter(fw);
				buffWrite.append(umidadeSoloAtual.toString() + '\n');
				buffWrite.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				System.out.println("Problema na formatacao dos dados da umidade");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
}
