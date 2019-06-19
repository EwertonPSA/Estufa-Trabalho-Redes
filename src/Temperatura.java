import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Temperatura {
	private static String path = "temperatura.txt";/*arquivo que simula temperatura*/
	private static File arqTemperatura = null;
	private static Integer temperaturaInicial = 30;
	
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
		if(!arqTemperatura.exists())
			arqTemperatura.createNewFile();
		FileWriter fw = new FileWriter(arqTemperatura);
		BufferedWriter buffWrite = new BufferedWriter(fw);
		buffWrite.append(temperaturaInicial.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
		buffWrite.close();
	}
	
	public static void main(String[] argc) throws IOException {
		Integer temperatura = -1;
		//byte[] msg = ByteBuffer.allocate(4).putInt(temperatura).array();
		FileWriter fw = new FileWriter(Temperatura.getArqTemperatura());
		BufferedWriter buffWrite = new BufferedWriter(fw);
		buffWrite.append(temperatura.toString() + '\n');
		buffWrite.close();
		
		FileReader fr = new FileReader(Temperatura.getArqTemperatura());
		BufferedReader buffRead = new BufferedReader(fr);
		String msg = buffRead.readLine();
		System.out.println(Integer.parseInt(msg));
	}
}
