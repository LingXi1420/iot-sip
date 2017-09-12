package com.cmiot.sip_demo.client;

import com.cmiot.sip_demo.test.TTSUtil;

public class App
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			SipgateSipListener listener = new SipgateSipListener();
			listener.init();
			listener.sendRequest(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

}
