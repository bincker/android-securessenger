package ki.securess.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Enumeration;

import ki.securess.Constants;
import ki.securess.CryptUtil;
import ki.securess.R;
import ki.securess.activity.ChatActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ChatService
{
	private PrivateKey privateKey;

	private PublicKey publicKey;

	private PublicKey sendingKey;

	File fpriv = new File(Environment.getExternalStorageDirectory() + "/" + Constants.PRIVKEY_LOCATION);

	File fpub = new File(Environment.getExternalStorageDirectory() + "/" + Constants.PUBKEY_LOCATION);

	ServerSocket serverSocket;

	Socket socket;

	InputStream socketInput;

	OutputStream socketOutput;

	Thread hostingThread;

	Thread connectingThread;

	Thread socketInputThread;
	
	String code;
	
	Handler message_handler;
	
	boolean isHosting;
	
	Context ctx;
		
	public ChatService(Context ctx, Handler handler, boolean isHosting, String code)
	{
		this.ctx = ctx;
		this.message_handler = handler;
		this.isHosting = isHosting;
		this.code = code;
	}
	
	public void start()
	{
		try
		{
			loadKey();
		}
		catch (Exception e)
		{
			message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.error_on_loading_key, 0));
		}
		
		if (isHosting)
		{
			hostingThread = new Thread(procHost);
			hostingThread.start();
		}
		else
		{
			connectingThread = new Thread(procConnect);
			connectingThread.start();
		}
	}
	
	public boolean sendMessage(String msg)
	{
		try
		{
			byte[] _message = CryptUtil.crypt(msg, sendingKey);
			int len = _message.length;
			int l1 = (0xFF000000 & len) >> 24;
			int l2 = (0x00FF0000 & len) >> 16;
			int l3 = (0x0000FF00 & len) >> 8;
			int l4 = (0x000000FF & len);
			
			socketOutput.write(SOCK_CODE_MESSAGE);
			socketOutput.write(l1);
			socketOutput.write(l2);
			socketOutput.write(l3);
			socketOutput.write(l4);
			socketOutput.write(_message, 0, len);
			
			return true;
		}
		catch (Exception e)
		{
			Log.e(this.getClass().getName(), e.getMessage(), e);
			return false;
		}
	}
	
	public void destroy()
	{
		try
		{
			if (socketOutput != null)
			{
				socketOutput.write(SOCK_DISCONNECT);
			}
		}
		catch (IOException e1)
		{
		}
		
		if (socketInputThread != null)
		{
			socketInputThread.interrupt();
			socketInputThread = null;
		}
		if (socket != null)
		{
			try
			{
				socket.close();
			}
			catch (Exception e)
			{
			}
		}
		if (serverSocket != null)
		{
			try
			{
				serverSocket.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	private static final int SOCK_CODE_REQUEST_PUBKEY = 1;
	
	private static final int SOCK_CODE_PUBKEY_ARRIVED = SOCK_CODE_REQUEST_PUBKEY + 1;

	private static final int SOCK_CODE_MESSAGE = SOCK_CODE_PUBKEY_ARRIVED + 1;
	
	private static final int SOCK_DISCONNECT = SOCK_CODE_MESSAGE + 1;

	Runnable procReadSocket = new Runnable()
	{
		@Override
		public void run()
		{
			int msgcode = 0;
			while (!Thread.interrupted())
			{
				try
				{
					msgcode = 0;
					msgcode = socketInput.read();
				}
				catch (IOException e)
				{
					if (e.getMessage().equals("Bad file number"))
					{
						// This should mean that connection has been closed
						destroy();
						message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_closed, 0));
						return;
					}
					else
					{
						Log.e(this.getClass().getName(), e.getMessage(), e);
					}
				}

				switch (msgcode)
				{
					case SOCK_CODE_REQUEST_PUBKEY:
					{
						try
						{
							message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_EXCHANGING_KEYS));
	
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ObjectOutputStream os = new ObjectOutputStream(baos);
							os.writeObject(publicKey);
							os.flush();
							byte[] pubkey = baos.toByteArray();
							int len = baos.size();
							int l1 = (0xFF000000 & len) >> 24;
							int l2 = (0x00FF0000 & len) >> 16;
							int l3 = (0x0000FF00 & len) >> 8;
							int l4 = (0x000000FF & len);
							os.close();
							socketOutput.write(SOCK_CODE_PUBKEY_ARRIVED);
							socketOutput.write(l1);
							socketOutput.write(l2);
							socketOutput.write(l3);
							socketOutput.write(l4);
							socketOutput.write(pubkey, 0, len);
							socketOutput.flush();
						}
						catch (IOException e)
						{
							Log.e(this.getClass().getName(), e.getMessage(), e);
							destroy();
							message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_closed, 0));
							return;
						}
					}
					case SOCK_CODE_PUBKEY_ARRIVED:
					{
						try
						{
							socketInput.read(); // WHY DOES IT NEEDS TO READ TWICE THE PROTOCOL CODE??
							int l1 = socketInput.read();
							int l2 = socketInput.read();
							int l3 = socketInput.read();
							int l4 = socketInput.read();
							int len = (l1 << 24)
									| (l2 << 16)
									| (l3 << 8)
									| (l4);
							byte[] bytes = new byte[len];
							if (len > 0)
							{
								socketInput.read(bytes, 0, len);
								ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
								sendingKey = (PublicKey) ois.readObject();
								ois.close();
								message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_PUBKEY_ARRIVED));
							}
						}
						catch (Exception e)
						{
							Log.e(this.getClass().getName(), e.getMessage(), e);
						}
						break;
					}
					case SOCK_CODE_MESSAGE:
					{
						try
						{
							int l1 = socketInput.read();
							int l2 = socketInput.read();
							int l3 = socketInput.read();
							int l4 = socketInput.read();
							int len = (l1 << 24)
									| (l2 << 16)
									| (l3 << 8)
									| (l4);
							byte[] bytes = new byte[len];
							if (len > 0)
							{
								socketInput.read(bytes, 0, len);
								String message = CryptUtil.decrypt(bytes, privateKey);

								Message m = Message.obtain(message_handler, ChatActivity.MSG_MESSAGE_ARRIVED);
								Bundle b = new Bundle();
								b.putString("message", message);
								m.setData(b);
								message_handler.sendMessage(m);
							}
						}
						catch (Exception e)
						{
							Log.e(this.getClass().getName(), e.getMessage(), e);
						}
						break;
					}
					case SOCK_DISCONNECT:
					{
						// This should mean that connection has been closed
						destroy();
						message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_closed, 0));
						return;
					}
					case -1:
					{
						// This should mean that connection has been closed
						destroy();
						message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_not_established, 0));
						return;
					}
				}

				try
				{
					Thread.sleep(Constants.SOCKET_READ_TIMEOUT);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	};
	
	private void socketConnected()
	{
		//
		// The socket (server or client) has just established connection
		// Start the looper for the socket reading.
		//
		try
		{
			socketInput = socket.getInputStream();
			socketOutput = socket.getOutputStream();
			if (socketInputThread != null)
			{
				socketInputThread.interrupt();
			}
			socketInputThread = new Thread(procReadSocket);
			socketInputThread.start();
			
			socketOutput.write(SOCK_CODE_REQUEST_PUBKEY);
		}
		catch (IOException e)
		{
			Log.e(this.getClass().getName(), e.getMessage(), e);
		}
	}
	
	Runnable procConnect = new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				socket = new Socket(code, Constants.HOST_PORT);
				socketConnected();
			}
			catch (ConnectException e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				destroy();
				message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_not_established, 0));
			}
			catch (UnknownHostException e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				destroy();
				message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_closed, 0));
			}
			catch (IOException e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				destroy();
				message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_closed, 0));
			}
			catch (Exception e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				destroy();
				message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_not_established, 0));
			}
		}
	};

	Runnable procHost = new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				Log.i(this.getClass().getName(), "Listening");
				String ip = getLocalIpAddress();
				if (ip == null)
				{
					message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.no_network, 0));
					return;
				}

				serverSocket = new ServerSocket(Constants.HOST_PORT);
				String addr = " " + ip;

				Log.i(this.getClass().getName(), addr);
				Bundle b = new Bundle();
				b.putString("code", addr);
				Message msg = Message.obtain(message_handler, ChatActivity.MSG_CODE_AVAILABLE);
				msg.setData(b);
				message_handler.sendMessage(msg);

				try
				{
					socket = serverSocket.accept();
					socketConnected();
				}
				catch (SocketException e)
				{
					if (!e.getMessage().toLowerCase().contains("interrupted"))
					{
						Log.e(this.getClass().getName(), e.getMessage(), e);
					}
				}
			}
			catch (IOException e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				destroy();
				message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_not_established, 0));
			}
			catch (Exception e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				destroy();
				message_handler.sendMessage(Message.obtain(message_handler, ChatActivity.MSG_ERROR_MESSAGE, R.string.connection_not_established, 0));
			}
		}
	};

	private void loadKey() throws StreamCorruptedException, FileNotFoundException, IOException, ClassNotFoundException
	{
		// Read keys from disk
		ObjectInputStream fispriv = new ObjectInputStream(new FileInputStream(fpriv));
		try
		{
			privateKey = (PrivateKey) fispriv.readObject();
		}
		finally
		{
			fispriv.close();
		}

		// Read keys from disk
		ObjectInputStream fispub = new ObjectInputStream(new FileInputStream(fpub));
		try
		{
			publicKey = (PublicKey) fispub.readObject();
		}
		finally
		{
			fispub.close();
		}
	}
	
	private String getLocalIpAddress()
	{
		try
		{
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress())
					{
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		}
		catch (SocketException ex)
		{
			Log.e(this.getClass().getName(), ex.toString());
		}
		return null;
	}
}
