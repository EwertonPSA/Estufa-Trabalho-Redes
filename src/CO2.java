import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CO2 extends Thread{
	private static String pathCO2 = "co2.txt";/*arquivo que simula temperatura*/
	private static String pathContribuicaoCO2 = "contribuicaoCO2.txt";
	private static File arqCO2 = null;
	private static File arqContribuicaoCO2 = null;
	private static int contribuicaoCO2Ambiente = -1;
	private static int limiarInfCO2Ambiente = 300;
	private static int timeUpdate = 1;

	/* Metodo que retorna o arquivo de temperatura para lida ou escrita*/
	public static File getArqCO2() throws IOException {
		if(arqCO2 == null)
			createFileCO2();
		return arqCO2;
	}
	
	/* Metodo que retorna o arquivo de contribuicao para lida ou escrita*/
	public static File getArqContribuicaoCO2() throws IOException {
		if(arqContribuicaoCO2 == null)
			createFileContribuicaoCO2();
		return arqContribuicaoCO2;
	}
	
	/* Esse metodo eh utilizado apenas pelos atuadores e gerenciador
	 * O gerenciador utiliza para resetar os status do equipamento(quando desligado ou iniciado) e os atuadores para simulacao
	 * Atraves dele o fator de contribuicao(que afeta a temperatura ambiente)
	 * Eh alterado pelo arquivo contribuicao.txt
	 * Apenas a classe Temperatura tem acesso para lida e escrita no arquivo*/
	public static void setContribuicaoCO2(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoCO2());
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
	private static void createFileCO2() throws IOException {
		Integer defaultCO2 = 320;
		arqCO2 = new File(pathCO2);
		if(!arqCO2.exists()) {
			arqCO2.createNewFile();
			FileWriter fw = new FileWriter(getArqCO2());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(defaultCO2.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
			buffWrite.close();
		}else {
			FileReader fr = new FileReader(getArqCO2());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio*/
				FileWriter fw = new FileWriter(arqCO2);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(defaultCO2.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
				buffWrite.close();
			}
		}
	}
	
	/* Esse metodo realiza a cricao do arquivo de contribuicao(utilizado para simular as contribuicoes dos atuadores na temperatura) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoCO2() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoCO2 = new File(pathContribuicaoCO2);
		if(!arqContribuicaoCO2.exists()) {
			arqContribuicaoCO2.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoCO2());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoCO2());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoCO2);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
		}
	}
	
	/* Esse metodo obtem a temperatura atual lendo o arquivo temperatura.txt
	 * E pega o fator de contribuicao do arquivo contribuicao.txt
	 * Tendo esses valores eh aplicado o fator de contribuicao do ambiente
	 * E retornado a temperatura atual*/
	private int updateCO2() throws FileNotFoundException, IOException{
		FileReader fr = new FileReader(getArqCO2());
		BufferedReader buffRead = new BufferedReader(fr);
		Integer contribuicaoCO2Equip;
		Integer contribuicaoAmbienteCO2;
		/*Lendo a temperatura do arquivo*/
		Integer temperaturaAtual = Integer.parseInt(buffRead.readLine());//Le a linha e repassa para inteiro
		//System.out.println("Lido no arquivo: " + temperaturaAtual);
		if(temperaturaAtual <= limiarInfCO2Ambiente)
			contribuicaoAmbienteCO2 = 0;
		else 
			contribuicaoAmbienteCO2 = contribuicaoCO2Ambiente;
		
		/*Lendo contribuicao dos equipamentos no arquivo*/
		fr = new FileReader(getArqContribuicaoCO2());
		buffRead = new BufferedReader(fr);
		contribuicaoCO2Equip = Integer.parseInt(buffRead.readLine());//Atualiza a contribuicao do equipamento
		return temperaturaAtual + contribuicaoAmbienteCO2 + contribuicaoCO2Equip;
	}
	
	@Override
	public void run() {
		Integer co2Atual;
		FileWriter fw = null;
		BufferedWriter buffWrite = null;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(timeUpdate);
				co2Atual = updateCO2();
				fw = new FileWriter(getArqCO2());
				buffWrite = new BufferedWriter(fw);
				buffWrite.append(co2Atual.toString() + '\n');
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
