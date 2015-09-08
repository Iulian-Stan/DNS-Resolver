package my;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DNSData
{
  private Map<String, Set<DNSResponse>> map = null;

  // load saved data from a file (object)
  @SuppressWarnings("unchecked")
  public void Load(String filePath)
  {
    try
    {
      ObjectInputStream ois =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
      map = (Map<String, Set<DNSResponse>>) ois.readObject();
      ois.close();
      if (map != null)
        for (Map.Entry<String, Set<DNSResponse>> me : map.entrySet())
        {
          for (DNSResponse dnsr : me.getValue())
          {
            if (dnsr.TTL() < (new Date()).getTime())
            {
              map.get((me.getKey())).remove(dnsr);
            }
          }
        }
    } catch (Exception e)
    {
      map = new HashMap<String, Set<DNSResponse>>();
    }
  }

  // save data to a file
  public void Save(String filePath) throws FileNotFoundException, IOException
  {
    if (!map.isEmpty())
    {
      ObjectOutputStream oos =
          new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
      oos.writeObject(map);
      oos.close();
    }
  }

  // display the data
  public void Display()
  {
    if (!map.isEmpty())
      for (Map.Entry<String, Set<DNSResponse>> me : map.entrySet())
      {
        System.out.println("Request for " + me.getKey());
        for (DNSResponse dnsr : me.getValue())
        {
          {
            System.out.println("\tType - " + dnsr.Type());
            System.out.println("\tClass - " + dnsr.Class());
            System.out.println("\tTTL - " + dnsr.TTL());
            System.out.println("\tData - " + dnsr.RDATA());
          }
        }
      }
  }

  // auxiliary function that adds records to the map
  public void AddEntry(String key, DNSResponse value)
  {
    if (map.containsKey(key))
      map.get(key).add(value);
    else
    {
      Set<DNSResponse> list = new HashSet<DNSResponse>();
      list.add(value);
      map.put(key, list);
    }
  }
}
