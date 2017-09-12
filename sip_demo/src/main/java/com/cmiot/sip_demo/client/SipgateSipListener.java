package com.cmiot.sip_demo.client;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.cmiot.sip_demo.rtp.InitSession;

import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.SipProviderImpl;

public class SipgateSipListener implements SipListener
{
	private SipStack sipStack;
	private SipProvider sipProvider;
	private HeaderFactory headerFactory;
	private AddressFactory addressFactory;
	private MessageFactory messageFactory;
	private Dialog dialog;
	
	private ListeningPoint lp;
	private ServerTransaction inviteTid;
	
	private RequestProcessor requestProcessor;
	
	private final static Logger log = Logger.getLogger( SipgateSipListener.class ); 

	
	public SipgateSipListener() throws PeerUnavailableException
	{
		requestProcessor = new RequestProcessor();
	}
	
	public void init() throws Exception
	{
		SipFactory sipFactory = SipFactory.getInstance();

		sipStack = null;
		Properties properties = new Properties();
		properties.setProperty("javax.sip.IP_ADDRESS", Property.fromHost);
		//properties.setProperty("javax.sip.OUTBOUND_PROXY", credentials.getProxy() +  "/" + ListeningPoint.UDP);
		properties.setProperty("javax.sip.STACK_NAME", "Sip Test");
//		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
//		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "shootmedebug.txt");
//		properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "shootmelog.txt");

		// Create SipStack object
		sipStack = sipFactory.createSipStack(properties);
		headerFactory = sipFactory.createHeaderFactory();
		addressFactory = sipFactory.createAddressFactory();
		messageFactory = sipFactory.createMessageFactory();
		log.info("#####################sip log: listenin " + "host:" + Property.fromHost + " port:" + Property.fromPort);
		lp = sipStack.createListeningPoint(Property.fromHost, Property.fromPort, ListeningPoint.UDP);

		SipgateSipListener listener = this;
		ListeningPointImpl lpl = (ListeningPointImpl)lp;
		SipProviderImpl providerImpl = lpl.getProvider();
		log.info("#####################sip log: providerImpl " + providerImpl);
		if (providerImpl != null && providerImpl.getListeningPoint() != null) {
			log.info("#####################sip log:  SipProviderImpl host:"
					+ providerImpl.getListeningPoint().getIPAddress()
					+ " port:" + providerImpl.getListeningPoint().getPort());
		}

		sipProvider = sipStack.createSipProvider(lp);
		sipProvider.addSipListener(listener);
	}

	public void sendRequest(String userAgentHeaderString) {
		try {
			// create >From Header
			SipURI fromAddress = addressFactory.createSipURI(Property.fromName, Property.publicHost);
			fromAddress.setPort(Property.fromPort);
			Address fromNameAddress = addressFactory.createAddress(fromAddress);
			fromNameAddress.setDisplayName(Property.fromName);
			FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
					Integer.toString(new Random().nextInt(10000000)));

			// create To Header
			SipURI toAddress = addressFactory.createSipURI(Property.toName, Property.toHost);
			toAddress.setPort(Property.toPort);
			Address toNameAddress = addressFactory.createAddress(toAddress);
			toNameAddress.setDisplayName(Property.toName);
			ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

			// create Request URI
			SipURI requestURI = addressFactory.createSipURI(Property.toName, Property.toHost + ":" + Property.toPort);

			// Create ViaHeaders

			ArrayList viaHeaders = new ArrayList();
			String ipAddress = lp.getIPAddress();
			ViaHeader viaHeader = headerFactory.createViaHeader(Property.publicHost, lp.getPort(), lp.getTransport(), null);

			// add via headers
			viaHeaders.add(viaHeader);

			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

			// Create a new CallId header
			CallIdHeader callIdHeader = sipProvider.getNewCallId();

			// Create a new Cseq header
			CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

			// Create the request.
			Request request = messageFactory.createRequest(requestURI, Request.INVITE, callIdHeader, cSeqHeader,
					fromHeader, toHeader, viaHeaders, maxForwards);
			// Create contact headers

			SipURI contactUrl = addressFactory.createSipURI(Property.fromName, Property.publicHost);
			contactUrl.setPort(lp.getPort());
			contactUrl.setLrParam();

			// Create the contact name address.
			SipURI contactURI = addressFactory.createSipURI(Property.fromName, Property.publicHost);
			contactURI.setPort(sipProvider.getListeningPoint(lp.getTransport()).getPort());

			Address contactAddress = addressFactory.createAddress(contactURI);

			// Add the contact address.
			contactAddress.setDisplayName(Property.fromName);

			ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
			request.addHeader(contactHeader);

			// You can add extension headers of your own making
			// to the outgoing SIP request.
			// Add the extension header.
			Header extensionHeader = headerFactory.createHeader("My-Header", "my header value");
			request.addHeader(extensionHeader);

			String sdpData = 
					//SDP版本目前为0,没有子版本
					"v=0\r\n" 
					//<用户名>用户在发起主机上登录名,如果主机不支持用户标识的概念,则为”-”
                    //<会话id>一般为数字串,其分配由创建工具决定,建议用网络时间协议(NTP)时戳,以确保唯一性.
                    //<版本>该会话公告的版本,供公告代理服务器检测同一会话的若干个公告哪个是最新公告.基本要求是会话数据修改后该版本值递增,建议用NTP时戳
                    //<网络类型>为文本串”IN”
                    //<地址类型>”IP4”(可为域名或点分十进制)/”IP6”(域名或压缩文本地址形式)
                    //<地址>
					+ "o=" + Property.fromName + " 13760799956958020 13760799956958020" + " IN IP4  " + Property.publicHost + "\r\n"
					+ "s=mysession session\r\n"
					+ "c=IN IP4  "+ Property.publicHost +"\r\n"
					+ "t=0 0\r\n"
					//媒体级会话的开始
					+ "m=audio "+ Property.rtpPort + " RTP/AVP 8\r\n"		//1.m=是媒体级会话的开始处，audio：媒体类型 ； 6022：端口号    ；RTP/AVP：传输协议   ；0：rtp头中的payload格式
					+ "a=rtpmap:8 pcma/8000/1\r\n"
					+ "a=ptime:20\r\n";
			byte[] contents = sdpData.getBytes();

			request.setContent(contents, contentTypeHeader);
			// You can add as many extension headers as you
			// want.

			extensionHeader = headerFactory.createHeader("My-Other-Header", "my new header value ");
			request.addHeader(extensionHeader);

			Header callInfoHeader = headerFactory.createHeader("Call-Info", "<http://www.antd.nist.gov>");
			request.addHeader(callInfoHeader);

			if (userAgentHeaderString != null) {
				List<String> userAgents = new ArrayList<String>();
				userAgents.add(userAgentHeaderString);

				UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgents);
				request.addHeader(userAgentHeader);
			}
			// Create the client transaction.
			ClientTransaction inviteTid = sipProvider.getNewClientTransaction(request);
			/*Dialog d = null;
			try {
				d = sipProvider.getNewDialog(inviteTid);
			} catch (SipException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/

			// send the request out.
			inviteTid.sendRequest();

			dialog = inviteTid.getDialog();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	} 

	public void processRequest(RequestEvent requestReceivedEvent)
	{
		try {
			requestProcessor.process(requestReceivedEvent, dialog);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void processResponse(ResponseEvent responseReceivedEvent)
	{	
		log.debug("Got a response. Code: " + responseReceivedEvent.getResponse().getStatusCode() + " / CSeq: "
				+ responseReceivedEvent.getResponse().getHeader(CSeqHeader.NAME));
		
		Response response = (Response) responseReceivedEvent.getResponse();
		CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

		log.debug("Response received : Status Code = " + response.getStatusCode() + " " + cseq);

		if (response.getStatusCode() == Response.UNAUTHORIZED
				|| response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED)
		{
			log.debug("unauthorized!!!");
		}

		try
		{
			if (response.getStatusCode() == Response.OK)
			{
				if (cseq.getMethod().equals(Request.INVITE))
				{
					Request ackRequest = dialog.createAck(cseq.getSeqNumber());
					log.debug("Sending ACK");
					dialog.sendAck(ackRequest);
					String toHost = this.getIPAddress(dialog.getRemoteTarget().toString());
					String responseStr= response.toString();
					int startIndex = responseStr.indexOf("m=audio");
					int endIndex = responseStr.indexOf("a=recvonly");
					//Iterator parameterNames = response.getContentDisposition().getParameterNames();
					//log.info("content disposition start:");
					/*while (parameterNames.hasNext()) {
						String parameterName = (String) parameterNames.next();
						log.info(parameterName + ":" + response.getContentDisposition().getParameter(parameterName));
					}*/
					//log.info("content disposition end");
					
					log.info(responseStr.substring(startIndex, endIndex));
					String[] mInfo = responseStr.substring(startIndex, endIndex).split(" ");
					log.info(mInfo[1]);
					int toPort = Integer.parseInt(mInfo[1]);
					InitSession rtpSession= new InitSession(toHost, toPort);
					rtpSession.sendData();
					sendBye();
				} else if (cseq.getMethod().equals(Request.CANCEL)) {
                    if (dialog.getState() == DialogState.CONFIRMED) {
                        // oops cancel went in too late. Need to hang up the
                        // dialog.
                        log.debug("Sending BYE -- cancel went in too late !!");
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                    }
                }else if (cseq.getMethod().equals(Request.BYE)) {
					log.info("remote server response bye 200");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void sendBye(){
		log.debug("Sending BYE!!!");
        Request byeRequest;
		try {
			byeRequest = dialog.createRequest(Request.BYE);
			ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
	        dialog.sendRequest(ct);
		} catch (SipException e) {
			e.printStackTrace();
		}
	}
	
	public int parseHeader(String header) {
		int port;
		String strPort = header.substring((header.indexOf(":") + 1));
		strPort = strPort.trim();
		port = Integer.parseInt(strPort);
		System.out.println("Port Extracted as: " + port);
		return port;
	}
	
	public String getIPAddress(String sipAddress) {
		String ipAddress = null;
		int start, end;
		start = sipAddress.indexOf('@') + 1;
		end = sipAddress.lastIndexOf(':');
		ipAddress = sipAddress.substring(start, end);
		System.out.println("IP Extracted as: " + ipAddress);
		return ipAddress;
	}

	public void processTimeout(TimeoutEvent arg0)
	{
        log.debug("Process event recieved.");
	}

	public void processTransactionTerminated(TransactionTerminatedEvent arg0)
	{
        log.debug("Transaction terminated event recieved.");
	}

	public void processDialogTerminated(DialogTerminatedEvent arg0)
	{
        log.debug("Dialog terminated event recieved.");
	}

	public void processIOException(IOExceptionEvent arg0)
	{
        log.debug("IO Exception event recieved.");
	}
}
