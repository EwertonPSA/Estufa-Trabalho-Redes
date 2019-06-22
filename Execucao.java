import java.io.IOException;
import java.util.*;

public class Execucao{
	public static void main(String args[]) throws IOException {/*Classe que cuida de executar os jar dos programas na pasta Estufa pegando o diretorio atual(fora da pasta)*/
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\Gerenciador.jar & exit\"");
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\Aquecedor.jar & exit\""); 
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\Resfriador.jar & exit\""); 
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\Irrigador.jar & exit\""); 
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\Injetor.jar & exit\"");
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\SensorCO2.jar & exit\""); 
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\SensorTemperatura.jar & exit\""); 
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\SensorUmidade.jar & exit\""); 
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"java -jar " + System.getProperty("user.dir") + "\\Estufa\\Cliente.jar & exit\"");
	}
}