package my;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DNSResolver
{

  // get DNS server address as array of byte
  public static byte[] Init()
  {
    List<?> nameservers = sun.net.dns.ResolverConfiguration.open().nameservers();
    if (!nameservers.isEmpty())
    {
      String delimiter = "[.]";
      String[] tokens = nameservers.get(0).toString().split(delimiter);
      byte[] server = new byte[4];
      for (int i = 0; i < 4; ++i)
        server[i] = (byte) Short.parseShort(tokens[i]);
      return server;
    }
    return null;
  }

  // resolve site name to an IP address
  public static void GetIpByName(String name, byte[] server, DNSData data)
  {
    int length = 17;
    int offset = 0;

    String delimiter = "[.]";
    String regex =
        "^(([a-zA-Z]|[a-zA-Z][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z]|[A-Za-z][A-Za-z0-9\\-]*[A-Za-z0-9])$";

    if (!name.matches(regex))
    {
      System.err.println("The provided string is not a valid URL !");
      return;
    }

    String[] tokens = name.split(delimiter); // split URL into tokens (separated by points)

    for (int i = 0; i < tokens.length; ++i)
      length += tokens[i].length() + 1;

    byte[] sendData = new byte[length];
    byte[] receiveData = new byte[512];

    try
    {
      DatagramSocket dnsSocket = new DatagramSocket();

      sendData[0] = 0; // byte 0 and 1
      sendData[1] = 1; // Identification
      sendData[2] = 1; // byte 2 and 3
      sendData[3] = 0; // Flags
      sendData[4] = 0; // byte 4 and 5
      sendData[5] = 1; // Total Questions
      sendData[6] = 0; // byte 6 and 7
      sendData[7] = 0; // Total Answers
      sendData[8] = 0; // byte 8 and 9
      sendData[9] = 0; // Total Authority Resource Records
      sendData[10] = 0; // byte 10 and 11
      sendData[11] = 0; // Total Additional Resource Records
      offset = 12; // total 12 bytes of the header are hard coded

      for (int i = 0; i < tokens.length; ++i) // the URL is encoded
      { // as a sequence of tokens
        sendData[offset++] = (byte) tokens[i].length(); // the length precedes
        for (int j = 0; j < tokens[i].length(); ++j)
          // the token itself
          sendData[offset++] = (byte) tokens[i].charAt(j);// www.site.net -> 3www4site3net
      }

      sendData[offset++] = 0; // end of the question
      sendData[offset++] = 0; // QType
      sendData[offset++] = 1; // QType 1 -> hostname
      sendData[offset++] = 0; // QClass
      sendData[offset++] = 1; // QClass 1 -> ipv4

      DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, InetAddress.getByAddress(server), 53);
      dnsSocket.send(sendPacket);

      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      dnsSocket.receive(receivePacket);

      if (receiveData[0] != sendData[0] || receiveData[1] != sendData[1]) // test Identification
        System.err.println("Request Id does not match the response !");
      else if ((receiveData[2] & 0xff) >> 7 != 1) // test message type
        System.err.println("Received a request instead of a response !");
      else if ((receiveData[3] & 0x0f) != 0) // test error code
        System.err.println("Request failed with error code : " + (receiveData[3] & 0x0f) + " !");
      else if ((receiveData[6] | receiveData[7]) == 0) // test answers count
        System.err.println("Server error. Response not found !");
      else
      {
        offset = length + receiveData[length + 1];
        int i = (receiveData[6] << 8) + receiveData[7];
        while (--i > -1)
        {
          DNSResponse dnsr =
              new DNSResponse((short) ((receiveData[offset - 10] << 8) + receiveData[offset - 9]), // class
                  (short) ((receiveData[offset - 8] << 8) + receiveData[offset - 7]), // type
                  (receiveData[offset - 6] << 24) + (receiveData[offset - 5] << 16)
                      + (receiveData[offset - 4] << 8) + receiveData[offset - 3], // ttl
                  Arrays.copyOfRange(receiveData, offset, offset
                      + ((receiveData[offset - 2] << 8) + receiveData[offset - 1]))); // data
          if (dnsr.Class() == 5 && dnsr.Type() == 1 && dnsr.PTR() != 0)
          {
            StringBuilder sb = new StringBuilder();

            int k = dnsr.PTR(), j = k + receiveData[k];
            while (0 != receiveData[k])
            {
              while (k < j)
              {
                sb.append((char) receiveData[++k]);
              }
              j += receiveData[++k] + 1;
              if (0 != receiveData[k])
                sb.append('.');
            }
            dnsr.DATA(sb.toString());
          }
          data.AddEntry(name, dnsr);
          offset += ((receiveData[offset - 2] << 8) + receiveData[offset - 1]) + 12;
        }
      }
    } catch (SocketException e)
    {
      e.printStackTrace();
    } catch (UnknownHostException e)
    {
      e.printStackTrace();
    } catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  // resolve a sire name to an IP address
  public static void GetNameByIp(String ip, byte[] server, DNSData data)
  {
    int length = 30;
    int offset = 0;

    String delimiter = "[.]";
    String regex = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    if (!ip.matches(regex))
    {
      System.err.println("The provided string is not a valid IP !");
      return;
    }

    String[] tokens = ip.split(delimiter);

    for (int i = 0; i < tokens.length; ++i)
      length += tokens[i].length() + 1;

    byte[] sendData = new byte[length];
    byte[] receiveData = new byte[512];

    try
    {
      DatagramSocket dnsSocket = new DatagramSocket();

      sendData[0] = 0; // byte 0 and 1
      sendData[1] = 1; // Identification
      sendData[2] = 1; // byte 2 and 3
      sendData[3] = 0; // Flags
      sendData[4] = 0; // byte 4 and 5
      sendData[5] = 1; // Total Questions
      sendData[6] = 0; // byte 6 and 7
      sendData[7] = 0; // Total Answers
      sendData[8] = 0; // byte 8 and 9
      sendData[9] = 0; // Total Authority Resource Records
      sendData[10] = 0; // byte 10 and 11
      sendData[11] = 0; // Total Additional Resource Records
      offset = 12; // total 12 bytes of the header are hard coded

      for (int i = tokens.length - 1; i > -1; --i) // the URL is encoded
      { // as a sequence of tokens
        sendData[offset++] = (byte) tokens[i].length(); // the length precedes
        for (int j = 0; j < tokens[i].length(); ++j)
          // the token itself
          sendData[offset++] = (byte) tokens[i].charAt(j); // 127.0.0.1 -> 3127101011
      }

      sendData[offset++] = 7; // inverse
      sendData[offset++] = 'i'; // dns
      sendData[offset++] = 'n'; // requests
      sendData[offset++] = '-'; // a followed
      sendData[offset++] = 'a'; // by
      sendData[offset++] = 'd'; // in-addr.arpa
      sendData[offset++] = 'd'; // encoded
      sendData[offset++] = 'r'; // as a
      sendData[offset++] = 4; // part
      sendData[offset++] = 'a'; // of the
      sendData[offset++] = 'r'; // url
      sendData[offset++] = 'p'; // and
      sendData[offset++] = 'a'; // ends
      sendData[offset++] = 0; // with 0
      sendData[offset++] = 0; // QType
      sendData[offset++] = 12; // QType 12 -> ip address
      sendData[offset++] = 0; // QClass
      sendData[offset++] = 1; // QClass 1 -> ipv4

      DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, InetAddress.getByAddress(server), 53);
      dnsSocket.send(sendPacket);

      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      dnsSocket.receive(receivePacket);

      if (receiveData[0] != sendData[0] || receiveData[1] != sendData[1]) // test Identification
        System.err.println("Request Id does not match the response !");
      else if ((receiveData[2] & 0xff) >> 7 != 1) // test message type
        System.err.println("Received a request instead of a response !");
      else if ((receiveData[3] & 0x0f) != 0) // test error code
        System.err.println("Request failed with error code : " + (receiveData[3] & 0x0f) + " !");
      else if ((receiveData[6] | receiveData[7]) == 0) // test answers count
        System.err.println("Server error. Response not found !");
      else
      {
        offset = length + receiveData[length + 1];
        int i = (receiveData[6] << 8) + receiveData[7];
        while (--i > -1)
        {
          DNSResponse dnsr =
              new DNSResponse((short) ((receiveData[offset - 10] << 8) + receiveData[offset - 9]), // class
                  (short) ((receiveData[offset - 8] << 8) + receiveData[offset - 7]), // type
                  (receiveData[offset - 6] << 24) + (receiveData[offset - 5] << 16)
                      + (receiveData[offset - 4] << 8) + receiveData[offset - 3], // ttl
                  Arrays.copyOfRange(receiveData, offset, offset
                      + ((receiveData[offset - 2] << 8) + receiveData[offset - 1]))); // data

          if (dnsr.Class() == 5 && dnsr.Type() == 1 && dnsr.PTR() != 0)
          {
            StringBuilder sb = new StringBuilder();

            int k = dnsr.PTR(), j = k + receiveData[k];
            while (0 != receiveData[k])
            {
              while (k < j)
              {
                sb.append((char) receiveData[++k]);
              }
              j += receiveData[++k] + 1;
              if (0 != receiveData[k])
                sb.append('.');
            }
            dnsr.DATA(sb.toString());
          }

          data.AddEntry(ip, dnsr);
          offset += ((receiveData[offset - 2] << 8) + receiveData[offset - 1]) + 12;
        }
      }
    } catch (SocketException e)
    {
      e.printStackTrace();
    } catch (UnknownHostException e)
    {
      e.printStackTrace();
    } catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  public static void main(String[] args)
  {
    try
    {
      File configFile = new File("config.properties");
      InputStream inputStream = new FileInputStream(configFile);
      Properties props = new Properties();
      props.load(inputStream);
      String inFile = props.getProperty("inFile", "dnsData.in");
      String outFile = props.getProperty("outFile", "dnsData.out");
      String siteName = props.getProperty("name", "www.google.com");
      String siteIp = props.getProperty("ip", "173.194.112.19");

      byte[] server = Init();

      if (null != server)
      {
        DNSData data = new DNSData();
        data.Load(inFile);

        if (null != data)
        {
          System.out.println("\tExisting data");
          data.Display();
        }

        GetIpByName(siteName, server, data);
        GetNameByIp(siteIp, server, data);

        if (null != data)
        {
          System.out.println("\tNew data");
          data.Display();
        }

        try
        {
          data.Save(outFile);
        } catch (FileNotFoundException e)
        {
          e.printStackTrace();
        } catch (IOException e)
        {
          e.printStackTrace();
        }

      } else
      {
        System.err.println("Could not find DNS server");
      }
    } catch (Exception e)
    {
      System.err.println(e);
    }
  }
}
