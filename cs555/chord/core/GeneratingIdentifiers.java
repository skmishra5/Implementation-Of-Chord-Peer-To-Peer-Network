package cs555.chord.core;

import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;

public class GeneratingIdentifiers {
	
	public static int generateNodeIdentifier()
	{
		Date dte=new Date();
		long timestamp = dte.getTime();
		String hashCode = null;
		try {
			hashCode = encrypt(Long.toHexString(timestamp));
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] temp = convertHexToBytes(hashCode);
		int test = byteArrayToInt(temp);
		int high =  (test >> 16);
		int low =  (test & 0xFFFF);
		
		if(low < 0)
		{
			generateNodeIdentifier();
		}
		else
		{
			return low;
		}
		
		return 0;
	}
	
	public static byte[] convertHexToBytes(String hexString) {
		int size = hexString.length();
		byte[] buf = new byte[size / 2];
		int j = 0;
		for (int i = 0; i < size; i++) {
			String a = hexString.substring(i, i + 2);
			int valA = Integer.parseInt(a, 16);
			i++;
			buf[j] = (byte) valA;
			j++;
		}
		return buf;
	}
	
	public static int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}
	
	 public static String encrypt(String x) throws Exception {

		 java.security.MessageDigest digest = null;
		 digest = java.security.MessageDigest.getInstance("MD5");
		 byte[] hashedBytes = digest.digest(x.getBytes("UTF-8"));
		 return convertByteArrayToHexString(hashedBytes);
	 }
	 
	 public static String convertByteArrayToHexString(byte[] arrayBytes) {
		 StringBuffer stringBuffer = new StringBuffer();
		 for (int i = 0; i < arrayBytes.length; i++) {
		     stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
		             .substring(1));
		 }
		 return stringBuffer.toString();
	 }
}
