import java.io.File;
import java.io.IOException;

public class Temperatura {
	private static String path = "temperatura.txt";/*arquivo que simula temperatura*/
	private static File arqTemperatura;
	
	public static String getPath() {
		return path;
	}

	public static File getArqTemperatura() throws IOException {
		if(arqTemperatura == null)
			createFile();
		return arqTemperatura;
	}

	public static void setArqTemperatura(File arqTemperatura) {
		Temperatura.arqTemperatura = arqTemperatura;
	}

	private static void createFile() throws IOException {
		arqTemperatura = new File(path);
	}
}

//temperatura vai ter o ambiente e vai sempre realizar alteracao na temperatura
//aquecedor vai só alterar o fator de contribuicao
//fator de contribuição do ambiente: 1
//fator de contribuição dos atuadores: 2
//ambiente será uma thread na classe da temperatura
