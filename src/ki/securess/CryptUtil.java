package ki.securess;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CryptUtil
{
	private static final String KEY_TYPE = "RSA";
	
	private static final String CRYPT_TYPE = "RSA/ECB/PKCS1Padding";
	
	private static final String STRING_ENCODING = "UTF-8";
	
	public static KeyPair genKey(int keysize) throws NoSuchAlgorithmException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_TYPE);
		keyGen.initialize(keysize);
		return keyGen.generateKeyPair();
	}

	public static byte[] crypt(String message, PublicKey key) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException
	{
		Cipher cipher = Cipher.getInstance(CRYPT_TYPE);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(message.getBytes(STRING_ENCODING));
	}

	public static String decrypt(byte[] message, PrivateKey key) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException
	{
		Cipher cipher = Cipher.getInstance(CRYPT_TYPE);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return new String(cipher.doFinal(message), STRING_ENCODING);
	}
}
