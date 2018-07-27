package com.cmiot.sip_demo.test;
/**
  * Filename		:		SipUA.java
  * Status			:		Alpha
  * Reference		:		http://www-x.antd.nist.gov/proj/iptel/src/nist-sip/jain-sip/docs/api/index.html
  * Author			:		Sai Kiran S. Kovvuri
  * Mentor			:		Dr. Ala Alfuqaha, CS Dept., WMU
  * Requirements	:		A Sip Proxy/Registrar Server must be pre-installed
  * License			:		GNU/GPL, Public Domain
  * 
  * Description
  *
  * This is a Sip User Agent (Sip client) that sends a REGISTER message to a Registrar server and then
  * opetionally sends an INVITE message to the client whose IP address has to be specified in remoteIP.
  * Open the comments in the processResponse event in order to INVITE the other sip client at remoteIP.
  * Optionally requires JMFSession class for Audio communication. It can be exended to Video Conference 
  * with a few more lines of code.
  */

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import java.util.*;
import java.net.InetAddress;
import java.text.ParseException;

import javax.media.rtp.*;

public class SipUA implements SipListener {
	private SipFactory sipFactory = null;
	private AddressFactory addressFactory = null;
	private HeaderFactory headerFactory = null;
	private MessageFactory messageFactory = null;
	private SipStack sipStack = null;
	private SipProvider sipProvider = null;
	private ListeningPoint listeningPoint = null;

	// Variables to Create Requests like Register, Invite ...
	CallIdHeader callIdHeader = null;
	CSeqHeader cSeqHeader = null;
	ViaHeader viaHeader = null;
	ArrayList viaHeaders = null;
	ContentTypeHeader contentTypeHeader = null;
	MaxForwardsHeader maxForwards = null;
	ContactHeader contactHeader = null;
	FromHeader fromHeader = null;
	ToHeader toHeader = null;

	int sipPort = 5060;
	String remoteIP = "10.100.56.118";
	String registrarIP = "10.100.57.139";

	// Variables to establish a JMF Session
	RTPManager rtpManager = null;
	String remoteParty = null;
	int localJMFPort = 9090, remoteJMFPort;// = 9595;

	public static void main(String[] args) {
		SipUA sipUA = new SipUA();
		sipUA.sendMessages(); // Register and Invite
	}

	public SipUA() {
		configure();
	}

	private void configure() {
		rtpManager = RTPManager.newInstance();
		// Obtain an instance of the singleton SipFactory
		sipFactory = SipFactory.getInstance();

		// Set path name of SipFactory to reference implementation
		// (not needed - default path name is gov.nist)
		sipFactory.setPathName("gov.nist");

		// Create a Properties file to pass to sipStack ...
		Properties stackProps = new Properties();
		String localHost = null;
		try {
			localHost = InetAddress.getLocalHost().getHostAddress();

			// Set Properties for sipStack
			stackProps.put("javax.sip.IP_ADDRESS", "10.100.57.139");
			stackProps.put("javax.sip.STACK_NAME", "Reference Implementation SIP stack");

			// Create SipStack object
			sipStack = (SipStack) sipFactory.createSipStack(stackProps);
			System.out.println(
					"The SIP :: " + sipStack.getStackName() + " :: is configured for IP :: " + sipStack.getIPAddress());
		} catch (java.net.UnknownHostException e) {
			System.out.println("Unknown Host Exception");
			System.out.println(e.getMessage());
		} catch (PeerUnavailableException e) {
			// could not find SipStack Implementor class,
			// gov.nist.javax.sip.SipStackImpl, in the CLASSPATH
			System.err.println("Peer Class Could not be Found");
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (SipException e) {
			// could not create SipStack for some other reason
			System.err.println("Other Sip Exception");
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try {
			// Default Port = ListeningPoint.PORT_5060 (currently used by OnDo
			// Server on this Machine)
			listeningPoint = sipStack.createListeningPoint(sipPort, ListeningPoint.UDP);
			System.out.println("Created Listening Point on... " + Integer.toString(listeningPoint.getPort()));
			// Create SipProvider based on the ListeningPoint
			sipProvider = sipStack.createSipProvider(listeningPoint);
			// Register this application as a SipListener of the SipProvider
			System.out.println("SipProvider created and Sip Listerner (this) added !!! ");
			sipProvider.addSipListener(this);

		} catch (ObjectInUseException e) {
			System.err.println("Another SipProvider is already using the ListeningPoint.");
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (TransportNotSupportedException e) {
			System.err.println("Transport Not Supported Exception");
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (InvalidArgumentException e) {
			System.err.println("Invalid Argument Exception");
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (TooManyListenersException e) {
			// A limit has been reached on the number of Listeners allowed per
			// provider
			System.err.println("OOPS!! only one Listener may be registered on the SipProvider concurrently");
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		System.out.println("\n\nDebug :: Sip Stack Configured !!!\n\n");
	} // End of configure();

	public void sendMessages() {
		try {
			// Create AddressFactory
			addressFactory = sipFactory.createAddressFactory();
			// Create HeaderFactory
			headerFactory = sipFactory.createHeaderFactory();
			// Create MessageFactory
			messageFactory = sipFactory.createMessageFactory();
		} catch (PeerUnavailableException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		System.out.println("Address, Header & Message Factories Created! ");
		// From Address
		SipURI fromAddress = null;
		Address fromNameAddress = null;
		// To Address
		SipURI toAddress = null;
		Address toNameAddress = null;

		Request register = null;
		try {
			// create From Header
			fromAddress = (SipURI) addressFactory.createSipURI("Sam", sipProvider.getSipStack().getIPAddress());
			fromAddress.setPort(sipProvider.getListeningPoint().getPort());
			fromNameAddress = addressFactory.createAddress("SaiKiran", fromAddress);
			fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345"); // Tag
																					// ->
																					// some
																					// random
																					// number
			System.out.println("From Address Constructed! ");

			// create To Header
			toAddress = (SipURI) addressFactory.createSipURI("Sam", sipProvider.getSipStack().getIPAddress() + ":5060");
			toAddress.setHost(registrarIP);
			toAddress.setPort(5060);
			toNameAddress = addressFactory.createAddress(null, toAddress);
			toHeader = headerFactory.createToHeader(toNameAddress, "56789");
			System.out.println("To Address Constructed! ");

			/**
			 * When the UAC creates a request, it MUST insert a Via into that
			 * request. The protocol name and protocol version in the header
			 * field MUST be SIP and 2.0, respectively. The Via header field
			 * value MUST contain a branch parameter. This parameter is used to
			 * identify the transaction created by that request. This parameter
			 * is used by both the client and the server.
			 *
			 * Ref:-
			 * http://www-x.antd.nist.gov/proj/iptel/src/nist-sip/jain-sip/docs/
			 * api/index.html javax.sip.header -> ViaHeader
			 */
			// Create ViaHeaders
			viaHeader = headerFactory.createViaHeader(sipProvider.getSipStack().getIPAddress(),
					(int) sipProvider.getListeningPoint().getPort(), sipProvider.getListeningPoint().getTransport(),
					null);
			viaHeader.setProtocol("SIP");
			System.out.println("DEBUG :: The Transaction Values of Via are:  ");
			System.out.println("\n\n Branch= " + viaHeader.getBranch() + " Host= " + viaHeader.getHost() + " MAddr= "
					+ viaHeader.getMAddr() + "\nPort= " + Integer.toString(viaHeader.getPort()) + " Protocol= "
					+ viaHeader.getProtocol() + " Received= " + viaHeader.getReceived() + " Transport= "
					+ viaHeader.getTransport() + "\n\n");

			viaHeaders = new ArrayList();
			viaHeaders.add(viaHeader);
			System.out.println("Via Header Constructed! ");

			// Create ContentTypeHeader
			contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
			System.out.println("Content-Type Header Constructed! ");

			// Create Max. Forwards Header
			maxForwards = headerFactory.createMaxForwardsHeader(70); // Some
																		// random
																		// Value
			System.out.println("Max Forwards Header Constructed! ");

			// Create ContactHeader
			contactHeader = headerFactory.createContactHeader(fromNameAddress);

			// create Request URI
			SipURI requestURI = null;
			requestURI = (SipURI) toAddress.clone();
			requestURI.setTransportParam(sipProvider.getListeningPoint().getTransport());

			// Create and send REGISTER Request
			callIdHeader = sipProvider.getNewCallId();
			System.out.println("This Call ID is : " + callIdHeader.getCallId());

			cSeqHeader = headerFactory.createCSeqHeader(1, Request.REGISTER);
			register = messageFactory.createRequest(requestURI, Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
					toHeader, viaHeaders, maxForwards, contentTypeHeader, "REGISTER");
			register.addHeader(contactHeader); // *** Without contact header the
												// Server returns a 403
												// Forbidden Message

			// sipProvider.sendRequest(register); // --> Stateless Request

			/**
			 * Before an application can send a new request it must first
			 * request a new client transaction to handle that Request. This
			 * method is called by the application to create the new client
			 * transaction befores it sends the Request on that transaction.
			 * This methods returns a new unique client transaction that can be
			 * passed to send Requests 'Statefully'.
			 */
			ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(register);
			clientTransaction.sendRequest();
		} catch (ParseException e) {
			// Implementation was unable to parse a value
			System.err.println("Error occured at : " + "[" + e.getErrorOffset() + "]");
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (InvalidArgumentException e) {
			System.err.println("Invalid Argument Exception");
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (SipException e) {
			// Another exception occurred
			System.err.println("**ERR** Other Exception in sendMessages() **ERR**");
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	} // End of sendMessges()

	public String getIPAddress(String sipAddress) {
		String ipAddress = null;
		int start, end;
		start = sipAddress.indexOf('@') + 1;
		end = sipAddress.lastIndexOf(':');
		ipAddress = sipAddress.substring(start, end);
		System.out.println("IP Extracted as: " + ipAddress);
		return ipAddress;
	}

	public int parseHeader(String header) {
		int port;
		String strPort = header.substring((header.indexOf(":") + 1));
		strPort = strPort.trim();
		port = Integer.parseInt(strPort);
		System.out.println("Port Extracted as: " + port);
		return port;
	}

	// -------------------- Events --------------------------
	// Process transaction timeout
	public void processTimeout(javax.sip.TimeoutEvent transactionTimeOutEvent) {
		if (transactionTimeOutEvent.isServerTransaction())
			System.out.println("Server transaction timed out");
		else
			System.out.println("Client transaction timed out");
	}

	// Process Request received
	public void processRequest(RequestEvent requestReceivedEvent) {
		// receivedEvent.getRequest() returns a 'Request' object and
		// .getMethod() returns String like Invite,Register,...
		Request req = (Request) requestReceivedEvent.getRequest();
		String request = (String) req.getMethod();
		System.out.println("Request Message received with Method " + request);
		ServerTransaction serverTransaction = requestReceivedEvent.getServerTransaction();
		if (serverTransaction == null) {
			try {
				serverTransaction = sipProvider.getNewServerTransaction(req);
			} catch (TransactionAlreadyExistsException ex) {
				System.out.println("Transaction Already Exists Exception");
				return;
			} catch (TransactionUnavailableException ex) {
				return;
			}
		}
		Dialog dialog = serverTransaction.getDialog();

		if (Request.INVITE.equals(request)) {
			System.out.println("Someone'z inviting U!!!");
			remoteJMFPort = (int) parseHeader(req.getHeader("AudioPort").toString());
			System.out.println("Request Message has AudioPort: " + remoteJMFPort);
			try {
				Response ringing = messageFactory.createResponse(Response.RINGING, req);
				ringing.addHeader(contactHeader);
				ToHeader to = (ToHeader) ringing.getHeader(ToHeader.NAME);
				int toTag = (int) System.currentTimeMillis();
				to.setTag(Integer.toString(toTag)); // Time Stamp
				serverTransaction.sendResponse(ringing);

				Response okResponse = messageFactory.createResponse(Response.OK, req);// callIdHeader,
																						// cSeqHeader,
																						// fromHeader,
																						// toHeader,
																						// viaHeaders,
																						// maxForwards
																						// );
				Header audioPort = headerFactory.createHeader("AudioPort", Integer.toString(localJMFPort));
				okResponse.addHeader(audioPort);
				okResponse.addHeader(contactHeader);
				serverTransaction.sendResponse(okResponse);
			} catch (ParseException e) {
				System.out.println("Parse Exception!!!\n" + e.getMessage());
				System.exit(-1);
			} catch (SipException e) {
				System.out.println("Sip Exception!!!\n" + e.getMessage());
				System.exit(-1);
			} catch (InvalidArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		else if ((Request.ACK.equals(request)) && (serverTransaction != null)) {
			if (dialog != null) {
				remoteParty = this.getIPAddress(dialog.getRemoteTarget().toString());
				/*
				 * System.out.println("Local Party = " + dialog.getLocalParty()
				 * + " Remote Party = " + dialog.getRemoteParty() +
				 * " Remote Target = " + remoteTarget + "\n" +
				 * "Dialog State  = " + dialog.getState());
				 */

				// If ACK'ed try to create a JMFSession ...
				new JMFSession(rtpManager, remoteParty, localJMFPort, remoteJMFPort);
			}
		}

		else if (Request.CANCEL.equals(request) || Request.BYE.equals(request)) {
			try {
				Response okResponse = messageFactory.createResponse(Response.OK, req);// callIdHeader,
																						// cSeqHeader,
																						// fromHeader,
																						// toHeader,
																						// viaHeaders,
																						// maxForwards
																						// );
				okResponse.addHeader(contactHeader);
				serverTransaction.sendResponse(okResponse);
			} catch (ParseException e) {
				System.out.println("Parse Exception!!!\n" + e.getMessage());
				System.exit(-1);
			} catch (SipException e) {
				System.out.println("Sip Exception!!!\n" + e.getMessage());
				System.exit(-1);
			} catch (InvalidArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// Process Response received
	public void processResponse(ResponseEvent responseReceivedEvent) {
		Response response = responseReceivedEvent.getResponse();
		int statusCode = (int) response.getStatusCode();
		System.out.println("ResponseEvent Received !!! Status Code = " + Integer.toString(statusCode)
				+ "; Reason Phrase = " + responseReceivedEvent.getResponse().getReasonPhrase());

		// This line throws an <XML> formatted <message> on output stream
		// System.out
		ClientTransaction clientTransaction = responseReceivedEvent.getClientTransaction();
		String method = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();

		if ((clientTransaction == null) && (method.equals(Request.INVITE)) && (statusCode == Response.OK)) {
			try {
				System.err.println("IGNORING A TRANSACTIONLESS RESPONSE ... ");
				SipURI toPeerSip = (SipURI) addressFactory.createSipURI("Sam",
						sipProvider.getSipStack().getIPAddress() + Integer.toString(sipPort));
				toPeerSip.setHost(remoteIP);
				toPeerSip.setPort(sipPort);
				Address topeerAddress = addressFactory.createAddress(null, toPeerSip);
				ToHeader toPeerHeader = headerFactory.createToHeader(topeerAddress, null);

				callIdHeader = sipProvider.getNewCallId();
				cSeqHeader = headerFactory.createCSeqHeader(1, Request.ACK);

				SipURI ackURI = addressFactory.createSipURI("sai",
						sipProvider.getSipStack().getIPAddress() + Integer.toString(sipPort));
				ackURI.setTransportParam(sipProvider.getListeningPoint().getTransport());
				ackURI.setPort(sipPort);

				Request ackOK = messageFactory.createRequest(ackURI, Request.ACK, callIdHeader, cSeqHeader, fromHeader,
						toPeerHeader, viaHeaders, maxForwards, contentTypeHeader, "ACK");
				ackOK.addHeader(contactHeader);
				ClientTransaction ackTransaction = sipProvider.getNewClientTransaction(ackOK);
				ackTransaction.sendRequest();
				// sipProvider.sendRequest(ackOK); // Send Statelessly

				return;

			} catch (InvalidArgumentException e) {
				System.err.println("Invalid Argument Exception :: " + e.getMessage());
			} catch (TransactionUnavailableException e) {
				System.err.println("Transaction Unavailable Exception :: " + e.getMessage());
			} catch (ParseException e) {
				System.err.println("Parse Exception :: " + e.getMessage());
			} catch (SipException e) {
				System.err.println("Sip Exception :: " + e.getMessage());
			}
		}

		Dialog dialog = clientTransaction.getDialog();

		if (statusCode == Response.TRYING || statusCode == Response.RINGING) {
			return; // do Nothing
		} else if ((statusCode == Response.OK)) {
			if (method.equals(Request.REGISTER)) {
				System.out.println("Response OK Received for " + method);
				SipURI toPeerAddress = null;
				/*
				 * try { // If OK then INVITE other UAC toPeerAddress = (SipURI)
				 * addressFactory.createSipURI("Sai",
				 * sipProvider.getSipStack().getIPAddress() + ":6060"); /* If
				 * host / port is not set correctly, we might receive a Sip
				 * Exception as follows:
				 * " Could not resolve next Hop or listening point Unavailable "
				 */

				/*
				 * toPeerAddress.setHost(remoteIP); // 'Host' is the Domain Name
				 * as Registered with the Registrar
				 * toPeerAddress.setPort(sipPort); Address toNameAddress =
				 * addressFactory.createAddress(toPeerAddress); toHeader =
				 * headerFactory.createToHeader(toNameAddress, null);
				 * System.out.println("Peer's To Address Constructed as " +
				 * toNameAddress.toString());
				 * 
				 * callIdHeader = sipProvider.getNewCallId(); cSeqHeader =
				 * headerFactory.createCSeqHeader(1, Request.INVITE);
				 * 
				 * SipURI inviteURI = (SipURI)toPeerAddress.clone();
				 * inviteURI.setTransportParam(sipProvider.getListeningPoint().
				 * getTransport());
				 * 
				 * Request invite = messageFactory.createRequest(inviteURI,
				 * Request.INVITE, callIdHeader, cSeqHeader, fromHeader,
				 * toHeader, viaHeaders, maxForwards, contentTypeHeader,
				 * "INVITE"); Header audioPort =
				 * headerFactory.createHeader("AudioPort",
				 * Integer.toString(localJMFPort)); invite.addHeader(audioPort);
				 * invite.addHeader(contactHeader);
				 * 
				 * ClientTransaction inviteTransaction =
				 * sipProvider.getNewClientTransaction(invite); /** Comment the
				 * line below if this UAC shouldnt INVITE other User Agent
				 * Client (UAC); This can be useful when the other UAC has to
				 * call this UAC
				 */
				// inviteTransaction.sendRequest();

				/*
				 * }catch (ParseException e) { System.out.println(
				 * "Parse Exception :\n" + e.getMessage()); System.exit(-1);
				 * }catch (InvalidArgumentException e) { System.out.println(
				 * "Invalid Argument Exception :\n" + e.getMessage());
				 * System.exit(-1); }catch (SipException e) {
				 * System.out.println("Other Sip Exception :\n" +
				 * e.getMessage()); System.exit(-1); }
				 */
			}

			// If response is a 200 OK response for a sent INVITE, try to send
			// an ACK
			else if (method.equals(Request.INVITE)) {
				try {
					System.out.println("Response OK Received for " + method);
					remoteJMFPort = (int) parseHeader(response.getHeader("AudioPort").toString());
					System.out.println("Respose Message has AudioPort: " + remoteJMFPort);

					Request ackOK = (Request) dialog.createRequest(Request.ACK);
					ackOK.addHeader(contactHeader);
					ackOK.addHeader(contentTypeHeader);
					dialog.sendAck(ackOK);
					remoteParty = this.getIPAddress(dialog.getRemoteTarget().toString());
					// If OK received try to create a JMFSession ...
					new JMFSession(rtpManager, remoteParty, localJMFPort, remoteJMFPort);
				} catch (SipException e) {
					System.out.println("Sip Exception :\n" + e.getMessage());
					System.exit(-1);
				}
			}

			else {
				System.out.println("\n\n *********** Other Events *********");
				System.out.println(" Code = " + statusCode + ";  " + responseReceivedEvent.toString());
			}
		} // IF (response == OK) Ends here
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processIOException(IOExceptionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
		// TODO Auto-generated method stub

	}
}// End of class SipUA