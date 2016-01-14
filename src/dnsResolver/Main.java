package dnsResolver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Main {
	private static Map<String, Set<DNSResponse>> map = new HashMap<String, Set<DNSResponse>>();;

	// load saved data from a file (object)
	@SuppressWarnings("unchecked")
	public static void Load(String filePath) {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
			map = (Map<String, Set<DNSResponse>>) ois.readObject();
			ois.close();
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		} catch (FileNotFoundException e) {
			System.err.println("Input file not found");
		} catch (IOException e) {
			System.err.println("Some IO exception on load");
		}
	}

	// save data to a file
	public static void Save(String filePath) {
		if (!map.isEmpty()) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(filePath)));
				oos.writeObject(map);
				oos.close();
			} catch (IOException e) {
				System.err.println("Some IO exception on save");
			}
		}
	}

	// display the data
	public static void Display(String header) {
		if (!map.isEmpty()) {
			System.out.println("\t\t" + header);
			for (Map.Entry<String, Set<DNSResponse>> me : map.entrySet()) {
				System.err.println("\tRequest for " + me.getKey());
				int count = 0;
				for (DNSResponse dnsr : me.getValue()) {
					{
						System.out.println("\t-----" + ++count + "-----");
						System.out.println("\tType - " + dnsr.Type());
						System.out.println("\tClass - " + dnsr.Class());
						System.out.println("\tTTL - " + dnsr.TTL());
						System.out.println("\tData - " + dnsr.RDATA());
						System.out.println();
					}
				}
			}
		}
	}

	// auxiliary function that adds records to the map
	public static void AddEntry(String key, DNSResponse value) {
		if (map.containsKey(key))
			map.get(key).add(value);
		else {
			Set<DNSResponse> list = new HashSet<DNSResponse>();
			list.add(value);
			map.put(key, list);
		}
	}

	public static void main(String[] args) {

		Properties props = new Properties();

		try {
			File configFile = new File("config.properties");
			InputStream inputStream = new FileInputStream(configFile);
			props.load(inputStream);

		} catch (FileNotFoundException e) {
			System.err.println("Config file not found");
		} catch (IOException e) {
			System.err.println("Some IO exception on config load");
		}

		String inFile = props.getProperty("inFile", "dnsData.in");
		String outFile = props.getProperty("outFile", "dnsData.out");
		String siteName = props.getProperty("name", "www.google.com");
		String siteIp = props.getProperty("ip", "173.194.112.19");

		Load(inFile);

		Display("Existing data");

		List<DNSResponse> ipList = DNSResolver.GetIpByHostName(siteName);
		if (null != ipList)
			for (DNSResponse response : ipList)
				AddEntry(siteName, response);

		List<DNSResponse> hostList = DNSResolver.GetHostNameByIp(siteIp);
		if (null != hostList)
			for (DNSResponse response : hostList)
				AddEntry(siteIp, response);

		Display("New data");

		Save(outFile);
	}
}
