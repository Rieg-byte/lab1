import java.io.*;
import java.net.*;
import java.util.LinkedList;

/**
 * Этот класс реализует основную функциональность нашего приложения
 * которое сканирует сайт и ищет новые ссылки
 */
public class Crawler {
	// Ссылки, которые надо найти. Хранит пары URL, глубина
	private static LinkedList <URLDepthPair> findLink = new LinkedList <URLDepthPair>();
	// Ссылки, которые уже были просмотрены
	private static LinkedList <URLDepthPair> viewedLink = new LinkedList <URLDepthPair>();
	
	//Метод выводит результат
	public static void showResult(LinkedList<URLDepthPair> viewedLink) {
		System.out.println("");
		for(URLDepthPair c : viewedLink) {
			String adjust = "";
			for(int i = 0; i < c.getDepth(); i++) {
				adjust = adjust.concat("  ");
			}
			System.out.println(adjust+c.getDepth() + "\tLink : "+c.getURL());
		}
	}
	
	
	//Метод для составления запроса
	public static void request(PrintWriter out,URLDepthPair pair) throws MalformedURLException {
		String request = "GET " + pair.getPath() + " HTTP/1.1\r\nHost:" + pair.getHost() + "\r\nConnection: Close\r\n";
		out.println(request);
		// записывает данные с буффера
		out.flush();
	}
	
	//Сканируем URL
	public static void Process(int maxDepth) throws IOException {
		// Пока список не пустой
		while(!findLink.isEmpty()) {
			// Удаляем первую ссылку для обработки
			URLDepthPair currentPair = findLink.removeFirst();
			// Проверяем, что глубина текущей пары меньше максимальной глубины
			if(currentPair.getDepth() < maxDepth) {
				Socket my_socket;
				try {
					// Создаем новый сокет из полученной строки с именем хоста и номера порта
					my_socket = new Socket(currentPair.getHost(), 80);
				} catch (UnknownHostException e) {
					System.out.println("Could not resolve URL: "+currentPair.getURL()+" at depth "+currentPair.getDepth());
					continue;
				}
				// Устанавливаем время ожидания сокета,
				// чтобы сокет знал, сколько нужно ждать передачи с другой стороны
				my_socket.setSoTimeout(1000);
				try {
					System.out.println("Now scanning: "+currentPair.getURL()+" at depth "+currentPair.getDepth());
					// Сокет получает даннные с другой стороны соединения
					BufferedReader in = new BufferedReader(new InputStreamReader(my_socket.getInputStream()));
					// Сокет отправляет даннные на другую сторону соединения
					PrintWriter out = new PrintWriter(my_socket.getOutputStream(), true);
					// Вызываем запрос
					request(out, currentPair);
					String line;
					// читаем строку
					while ((line = in.readLine()) != null) {
						if (line.indexOf(currentPair.getURLPrefix()) != -1 && line.indexOf('"') != -1) {
							// Формируем нашу ссылку
							StringBuilder currentLink = new StringBuilder();
							int i = line.indexOf(currentPair.getURLPrefix());
							while (line.charAt(i) != '"' && line.charAt(i) != ' ') {
								if (line.charAt(i) == '<') {
									currentLink.deleteCharAt(currentLink.length() - 1);
									break;
								}
								else {
									currentLink.append(line.charAt(i));
									i++;
								}
							}
							System.out.println(" > Found new link: "+currentLink.toString());
							//  проверка адреса с помощью check
							URLDepthPair newPair = new URLDepthPair(currentLink.toString(), currentPair.getDepth() + 1);
							if (currentPair.check(findLink, newPair) && currentPair.check(viewedLink, newPair) && !currentPair.getURL().equals(newPair.getURL()))
								findLink.add(newPair);
						}
					}
					// Закрываем сокет
					my_socket.close();
				} catch (SocketTimeoutException e) {
					my_socket.close();
				}
			}
			// добавляем пару в просмотренные
			viewedLink.add(currentPair);
		}
		//
		showResult(viewedLink);
	}

	/**
	 * Главный метод
	 */
	public static void main(String[] args) {
		//http://crawler-test.com/
		try {
			findLink.add(new URLDepthPair("http://crawler-test.com/", 0));
			Process(Integer.parseInt("3"));
		} catch (Exception e) {
			System.out.println("Error!\n"+e);
			System.out.println("Usage: java crawler <site> <depth>");
		}
	}
}
