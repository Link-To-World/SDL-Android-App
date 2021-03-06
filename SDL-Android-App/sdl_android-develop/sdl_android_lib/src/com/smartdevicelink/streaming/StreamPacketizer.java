package com.smartdevicelink.streaming;

import java.io.IOException;
import java.io.InputStream;

import com.smartdevicelink.SdlConnection.SdlConnection;
import com.smartdevicelink.protocol.ProtocolMessage;
import com.smartdevicelink.protocol.enums.SessionType;

public class StreamPacketizer extends AbstractPacketizer implements Runnable{

	public final static String TAG = "StreamPacketizer";

	private Thread t = null;
	private final static int BUFF_READ_SIZE = 1000000;

	public SdlConnection sdlConnection = null;
    private Object mPauseLock;
    private boolean mPaused;

	public StreamPacketizer(IStreamListener streamListener, InputStream is, SessionType sType, byte rpcSessionID) throws IOException {
		super(streamListener, is, sType, rpcSessionID);
        mPauseLock = new Object();
        mPaused = false;
	}

	public void start() throws IOException {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {
		try {
			is.close();
		} catch (IOException ignore) {}
		t.interrupt();
		t = null;
	}

	public void run() {
		int length;

		try 
		{
			while (t != null && !t.isInterrupted()) 
			{
				synchronized(mPauseLock)
				{
					while (mPaused)
                    {
						try
                        {
							mPauseLock.wait();
                        }
                        catch (InterruptedException e) {}
                    }
                }

				length = is.read(buffer, 0, BUFF_READ_SIZE);
				
				if (length >= 0) {
					ProtocolMessage pm = new ProtocolMessage();
					pm.setSessionID(_rpcSessionID);
					pm.setSessionType(_session);
					pm.setFunctionID(0);
					pm.setCorrID(0);
					pm.setData(buffer, length);
					
			        _streamListener.sendStreamPacket(pm);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    @Override
	public void pause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    @Override
    public void resume() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }
}
