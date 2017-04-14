package vo;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KeepAlive implements Serializable {

	private static final long serialVersionUID = 1L;
	private String ipAddress;
	private String hostname;
	
	public KeepAlive() {
		try {
			InetAddress address = InetAddress.getLocalHost();
			ipAddress = address.getHostAddress();
			hostname = address.getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
				+ " " + ipAddress + " " + hostname;
	}
	
}
