package com.cmiot.sip_demo.rtp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

import com.cmiot.sip_demo.client.Property;
import com.cmiot.sip_demo.g711.PCMA;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;

public class InitSession implements RTPAppIntf{
	private final static Logger log = Logger.getLogger( InitSession.class ); 
	
	private static int MAX_RTP_PKT_LENGTH = 160;
	
	public RTPSession rtpSession = null;  
    
    public InitSession(String toHost, int toPort) {  
        DatagramSocket rtpSocket = null;  
        DatagramSocket rtcpSocket = null;  
        
        try {  
            rtpSocket = new DatagramSocket(Property.rtpPort);  
            rtcpSocket = new DatagramSocket(Property.rtpPort + 1);  
        } catch (Exception e) {
        	log.error(e.getMessage());
        }  
          
        rtpSession = new RTPSession(rtpSocket, rtcpSocket);
        rtpSession.RTPSessionRegister(this,null,null);
        Participant p = new Participant(toHost, toPort, 0);
        rtpSession.addParticipant(p);
    }
    
    public void sendData(){
    	byte[] data = getBytes("C:\\Users\\Harold\\Desktop\\t.pcm");
    	List<byte[]> g711aBytes = getGroupBytes(data);
    	int seqNum = new Random().nextInt(100000);
    	long rtpTimestamp = System.currentTimeMillis();
    	for(byte[] g711aByte : g711aBytes){
    		rtpSession.payloadType(8);
    		rtpSession.sendData(g711aByte, rtpTimestamp, seqNum);
    		seqNum ++;
    		rtpTimestamp += 160;
    		try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    }
    
    
    /** 
     * 注释：字节数组到short的转换！ 
     * 
     * @param b 
     * @return 
     */ 
    public static short byteToShort(byte[] b) { 
        short s = 0; 
        short s0 = (short) (b[0] & 0xff);// 最低位 
        short s1 = (short) (b[1] & 0xff); 
        s1 <<= 8; 
        s = (short) (s0 | s1); 
        return s; 
    }
    
    private static List<byte[]> getGroupBytes(byte[] bytes){
		List<byte[]> groupBytes = new ArrayList<>();
		int groupBytesSize = bytes.length % MAX_RTP_PKT_LENGTH == 0 ? 
				bytes.length / MAX_RTP_PKT_LENGTH : (bytes.length / MAX_RTP_PKT_LENGTH + 1);
		for(int index = 0; index < groupBytesSize; index ++){
			byte[] groupByte = new byte[MAX_RTP_PKT_LENGTH];
			groupBytes.add(groupByte);
			for(int index1 = 0 ;index1 < MAX_RTP_PKT_LENGTH; index1++){
				if (index * MAX_RTP_PKT_LENGTH + index1 < bytes.length) {
					groupByte[index1] = bytes[index * MAX_RTP_PKT_LENGTH + index1];
				}
			}
		}
		
		/*List<byte[]> g711aData = new ArrayList<>();
		for(byte[] groupByte : groupBytes){
			short[] groupShort = new short[groupByte.length / 2];
			int shortIndex = 0;
			for(int index = 1; index < groupByte.length; index += 2){
				byte[] _bytes = new byte[2];
				_bytes[0] = groupByte[index - 1];
				_bytes[1] = groupByte[index];
				groupShort[shortIndex] = byteToShort(_bytes);
				shortIndex ++;
			}
			
			byte[] alaw = new byte[groupShort.length];
			PCMA.linear2alaw(groupShort, 0, alaw, groupShort.length);
			g711aData.add(alaw);
		}*/
		return groupBytes;
	}
    
    public static byte[] getBytes(String filePath) {
		byte[] buffer = null;
		FileInputStream fis = null;
		ByteArrayOutputStream bos = null;
		try {
			File file = new File(filePath);
			fis = new FileInputStream(file);
			bos = new ByteArrayOutputStream(1000);
			byte[] b = new byte[1000];
			int n;
			while ((n = fis.read(b)) != -1) {
				bos.write(b, 0, n);
			}
			fis.close();
			bos.close();
			buffer = bos.toByteArray();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return buffer;
	}

	@Override
	public void receiveData(DataFrame frame, Participant participant) {
		String s = new String(frame.getConcatenatedData());  
        System.out.println("���յ����: "+s+" , ������CNAME�� "  
                +participant.getCNAME()+"ͬ��Դ��ʶ��("+participant.getSSRC()+")");  
	}

	@Override
	public void userEvent(int type, Participant[] participant) {
		// TODO Auto-generated method stub
	}

	@Override
	public int frameSize(int payloadType) {
		return 1; 
	}
}
