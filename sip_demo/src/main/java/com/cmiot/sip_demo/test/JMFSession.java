package com.cmiot.sip_demo.test;
/**
  * Filename		:		JMFSession.java
  * Status			:		Beta
  * Author			:		Sai Kiran S. Kovvuri
  * Mentor			:		Dr. Ala Alfuqaha, CS Dept., WMU
  * Requirements	:		Java Media Framework2.1.1e must be pre-installed
  * License			:		GNU/GPL, Public Domain
  * 
  * Description
  * This is a JMFSession that is used for the audio communication through RTP between any 2 communicating
  * parties usually after a Sip Session. It can also be used stand-alone for testing or other purposes.
  * 
  */


import java.io.*;
import java.util.*;
import java.net.*;
import javax.media.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.format.*;
import javax.media.control.TrackControl;
import javax.media.control.QualityControl;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;
import com.sun.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.format.FormatChangeEvent;

class  JMFSession
{
/*	public static void main(String[] args) 
	{
		RTPManager rtpManager = null;
		String remoteParty = "10.0.4.15";
		int localPort = 9090;
		int remotePort = 9595;
		rtpManager = RTPManager.newInstance();

		new JMFSession(rtpManager, remoteParty, localPort, remotePort);
	}*/

	public JMFSession(RTPManager rtpM, String remoteIP, int localPort, int remotePort)
	{
		InetAddress remoteAddr;
		SessionAddress localAddress, remoteAddress ;
		try
		{
			if (rtpM != null)
			{
				System.out.println("RtpM is not null; " + "remoteIP = " + remoteIP + "; LocalPort = " + localPort +
									"; remotePort = " + remotePort );
			}
			remoteAddr = InetAddress.getByName( remoteIP ) ;
			localAddress = new SessionAddress( InetAddress.getLocalHost(), localPort ) ;
			remoteAddress = new SessionAddress( remoteAddr, remotePort ) ;
			System.out.println("Session Addresses created!!!");

			// Now Initialze the Manager and then add the target to the RTP Manager
			rtpM.initialize(localAddress) ;
			System.out.println("RTPM initialized !!!");
			rtpM.addTarget(remoteAddress) ;
			System.out.println( "RTP Session created between : " + localAddress + " and " + remoteAddress ) ;

			//Create a Session ie., an RTP Stream Sender and an RTP Stream Receiver/Player
			new RTPServer(rtpM);	
			new RTPPlayer(rtpM);
		}catch (UnknownHostException e)
		{
			System.out.println("Unknown Host Exception caught as : " + e.getMessage());
		}catch (Exception e)
		{
			System.out.println("Other Exception caught as : " + e.getMessage());
		}
	}
}

class RTPServer{
	MediaLocator mediaLocator ;
	Vector deviceList;
	CaptureDeviceInfo di;
	Format audioFormat = null;

	Processor processor = null ;
	DataSource dataSource = null ;
	RTPManager rtpManager ;
	String ipAddress;
	int port;

	public RTPServer(RTPManager rtpM)
	{
		this.rtpManager = rtpM;
		initJMFSession();
	}

	public void initJMFSession(){
		//定位所需要用的捕获设备
		deviceList = CaptureDeviceManager.getDeviceList(new AudioFormat("linear", 44100, 16, 2));
		System.out.println("Device list is of size: " + Integer.toString(deviceList.size()));
		di = (CaptureDeviceInfo) deviceList.firstElement();
		//获取捕获设备的位置Medialocator
		mediaLocator =  di.getLocator();
		System.out.println("Media is: " + mediaLocator.toString());
		
		boolean processorOK = false ;
		boolean configureOK = false ; 

		processorOK = createProcessor() ;
		System.out.println( "processor OK ?  " + processorOK ) ;
		if( processorOK )
			configureOK = createSend() ;
		System.out.println( "configure OK ? " + configureOK ) ;
		if( configureOK ){
			processor.start() ;
			System.out.println( "Processor starting..." ) ;
		}
	}
			// Method to create a Processor and do error checking
	public synchronized boolean createProcessor()
	{
		// create the datasource
		try {
			//利用捕获设备位置创建数据源
			dataSource = javax.media.Manager.createDataSource( mediaLocator ) ;
			System.out.println( "DataSource created for Device" ) ;
		} catch ( Exception e ) {
			System.out.println( "Error: Couldn't create Datasource" ) ;
			System.exit( -1 ) ;
		}

		// Create the processor to process the DataSource
		try 
		{
			//如果想把捕获的数据发送到网络或者保存起来，就需要创建处理器Processor
			processor = javax.media.Manager.createProcessor( dataSource ) ;
			System.out.println( "Processor created" ) ;
		} catch ( NoProcessorException p ) {
			System.out.println( "Error: Couldn't create processor" ) ;
			System.exit( -1 ) ;
		} catch ( IOException i ) {
			System.out.println( "Error: Error reading Device" ) ;
			System.exit( -1 ) ;
		}

		// Run Processor methods to configure Processor
		processor.addControllerListener(new StateListener());
		
		int state = Processor.Configured ;
		processor.configure() ;
		System.err.println( "value of Configured state: " + state +"\nvalue of getState: " + processor.getState());
		System.err.println("----------------------------------------");
		while(state != processor.getState())
		{
			processor.configure();
		}
		
		System.out.println( "Processor Configured" ) ;
		System.err.println( "Value of Configured state: " + state +"\nValue of getState: " + processor.getState());
		System.err.println("========================================");

		TrackControl [] track = processor.getTrackControls() ;

		boolean encodingOK = false ;

		// Check to see if the datasource had tracks
		if ( ( track == null ) || ( track.length < 1 ) ) 
		{
			System.out.println( "Error: No tracks in Source" ) ;
			System.exit( -1 ) ;
		}

		// Set the content type of the output data to RTP data
		ContentDescriptor content = new ContentDescriptor( ContentDescriptor.RAW_RTP ) ;
		processor.setContentDescriptor( content ) ;

		Format supportedFormats[] ;

		for ( int i = 0 ; i < track.length ; i++ )
		{
			// Set format to the format of TrackControl i.e. ContentDescriptor
			Format format = track[i].getFormat() ;
			if ( track[i].isEnabled() )
			{
				supportedFormats = track[i].getSupportedFormats() ;			// Find formats that support RAW_RTP
				if ( supportedFormats.length > 0 )
				{
					// Encode the track with MPEG_RTP format
					if( track[i].setFormat( supportedFormats[i] ) == null )
					{ 
						track[i].setEnabled( false ) ; 
						encodingOK = false ;
					} else {
						encodingOK = true ;
					}
				}
			} // End if
		} // End for

		// Now, if encoding occured correctly, the processor is programmed and ready to be realized
		int realized = Controller.Realized ;
		if ( encodingOK )
		{
			processor.realize() ;
			System.err.println( "value of Realized state: " + realized + "\nvalue of getState: " + processor.getState());
			System.err.println("----------------------------------------");
			while(realized != processor.getState())
			{
				processor.realize();
			}
			
			System.out.println( "Processor Realized" ) ;
			System.err.println( "Value of Realized state: " + realized +"\nValue of getState: " + processor.getState());
			System.err.println("----------------------------------------");

		} else {
			System.out.println( "Error: Encoding to MPEG format failed" ) ;
			System.exit( -1 ) ;
		}

		// Final Steps of creating Processor... Link the datasource to the Processor
		System.out.println( "Getting DataSource now ..." ) ;
		dataSource = processor.getDataOutput() ;

		return true ;
	} //createProcessor() ends here

	// Use the RTPManager to pass the application data to the lower network layer		
	public synchronized boolean createSend()
	{
		// Make a buffered Push Source from our data source
	//	PushBufferDataSource bufferDatasource = ( PushBufferDataSource ) dataSource ;
		// Make a buffered stream for each track in the processor
	//	PushBufferStream bufferStream[] = bufferDatasource.getStreams() ;

		SendStream outputStream ;						// Create a SendStream that will carry the output.
		SourceDescription SDESList[] ;					// Create the needed SDES list to identify sources.

		try{
			// Create the sendStreams and link them to the targets just added to the RTPManager
			outputStream = rtpManager.createSendStream( dataSource, 0 ) ;
			outputStream.start() ;
		} catch (UnsupportedFormatException e ) {
			System.out.println( "Error: SendStream UnsupportedFormatException: " + e.getMessage() ) ;
			System.out.println( e.getMessage() ) ;
			System.exit( -1 ) ;
		  }catch (IOException e ) {
			System.out.println( "Error: SendStream IOException " ) ;
			System.out.println( e.getMessage() ) ;
			System.exit( -1 ) ;
		  }catch ( Exception e ) {
			System.out.println( "Error: SendStream could not be created" ) ;
			System.out.println( e.getMessage() ) ;
			System.exit( -1 ) ;
		  }
		return true ;
	} // createSend() Ends here

	/**
	 * Controller Listener Class to Listen for the Controller events
	 * This is an Inner Class
	 */
	class StateListener implements ControllerListener
	{
		public void controllerUpdate( ControllerEvent c )
		{
			if( c instanceof ControllerClosedEvent )
			{	if( processor != null ){
				processor.stop() ;
				processor.close() ;
				processor = null ;
					rtpManager.removeTargets( "Sessions are done" ) ;
					rtpManager.dispose() ;
			 }
			}
			if( c instanceof EndOfMediaEvent )
			{
				processor.stop() ;
				processor.close() ;
				processor = null ;
				rtpManager.removeTargets( "Sessions are done" ) ;
				rtpManager.dispose() ;
				System.out.println( "End of Media Stream" ) ;
			}
		}
	} 

} // RTPServer Class Ends

class RTPPlayer implements ReceiveStreamListener, SessionListener, ControllerListener
{
	RTPManager rtpManager = null ;
	Object dataSync = new Object() ;
	boolean dataReceived = false, completed = false ;
	static String address;
	static int port ;

	/**
	 * Constructor for the RTPPlayer Class
	 * @param	sock		Socket through which UDP Communications must take place.
	 * @param	srv			IP Address of the Server whose Audio needs to be played.
	 * @param	portNumber	Port on which client and server RTP Session needs to be created.
	 */
	public RTPPlayer(RTPManager rtpM)//, String serverIP, int portNumber)
	{
		this.rtpManager = rtpM;
		initJMFSession();
	}

	public void initJMFSession()
	{
		try
		{
			rtpManager.addSessionListener( this ) ;
			rtpManager.addReceiveStreamListener( this ) ;
			System.out.println( "Added receive stream" ) ;
		}catch ( Exception e ) {
			System.out.println( "Error: Could not create RTP Session because " + e.getMessage() ) ;
			System.exit(-1);
		 }

		// Wait for for a maximum of 30 secs.for the data to arrive
		long then = System.currentTimeMillis();
		long waitingPeriod = 30000;	

	    synchronized (dataSync) 
		{
			while (!dataReceived && ( System.currentTimeMillis() - then < waitingPeriod ) ) 
			{
			    if (!dataReceived)
				{
					System.err.println("  - Waiting for RTP data from server...");
					try{
						dataSync.wait(1000);
					}catch(InterruptedException e)
					 {
						System.out.println("Wait Interrupted... ");
						System.exit(-1);
					 }
				}
			}
		}

		if (!dataReceived) {
			System.err.println("No RTP data was received.");
			quit();
		}
	}
	
		public boolean finished()
		{
			return (completed == true? true : false) ;
		}

		public void quit()
		{
			rtpManager.removeTargets( "Session over" ) ;
			rtpManager.dispose() ;
			rtpManager = null ;
			completed = true;
		}

		// The SessionListener update method, it listens for new joining users
		public synchronized void update( SessionEvent event )
		{
			if( event instanceof NewParticipantEvent )
			{
				Participant participant = ( ( NewParticipantEvent) event ).getParticipant() ;
				System.out.println( "Participant " + participant.getCNAME() + " joined" ) ;
			}
		}

		public synchronized void update( ReceiveStreamEvent event )
		{
			RTPManager manager = ( RTPManager ) event.getSource() ;
			Participant participant = event.getParticipant() ;
			ReceiveStream stream = event.getReceiveStream() ;

			if( event instanceof RemotePayloadChangeEvent )
			{
				System.out.println( "Error: Payload Change" );
				System.exit( -1 ) ;

			// If new stream, create player for that stream and associate datasource
			} else if ( event instanceof NewReceiveStreamEvent )
			  {
				try {
					stream = ( ( NewReceiveStreamEvent ) event ).getReceiveStream() ;
					DataSource dataSource = stream.getDataSource() ;

					// Get Formats of the New Stream
					RTPControl control = ( RTPControl ) dataSource.getControl( "javax.media.rtp.RTPControl" ) ;
					if( control != null ) {
						System.out.println( "New RTP Stream: " + control.getFormat() ) ;
					} else {
						System.out.println( "New Stream of unknown format" ) ;
					}

					if( participant == null )
					{
						System.out.println( "User of RTP Session unknown" );
					} else {
						System.out.println( "User of RTP Session: " + participant.getCNAME() ) ;
					}

					// Now that we associated the DataSource with the Stream, the player to handle
					// the media can be created

					Player player = javax.media.Manager.createPlayer( dataSource ) ;
					if( player == null )
						return ;
					System.out.println( "player created and linked to datasource" ) ;
					// Add controllerListener to catch Controller changes
					player.addControllerListener( this ) ;
					player.realize() ;
					System.out.println( "player realized" ) ;

					// Notify create() that a new stream has arrived
					synchronized( dataSync )
					{
						dataReceived = true ;
						dataSync.notifyAll() ;
					}
				} catch ( Exception e ) {
					System.out.println( "NewReceiveException " +e.getMessage() ) ; }
			  }

			/** This event is when a stream that was previously unidentifed becomes identified with a
			 * participant.  When an RTCP packet arrives that has an SSRC that matches the one without
			 * a participant arrives, this event is generated
			 */
			 else if( event instanceof StreamMappedEvent )
				{
					if( stream != null && stream.getDataSource() != null )
					{
						DataSource dataSource = stream.getDataSource() ;
						// Find out formats
						RTPControl control = ( RTPControl ) dataSource.getControl( "javax.media.rtp.RTPControl" ) ;
						if( control != null )
						{
							System.out.println( "Previously unidentified stream now associated with participant" ) ;
							System.out.println( "with format " +control.getFormat() +" from user: " +participant.getCNAME() ) ;
						}
					}
			// If this is an instant of the server ending the session, receive the Bye event and quit
			} else if( event instanceof ByeEvent )
				{
					System.out.println( "Stream ended, goodbye - from: " +participant.getCNAME() ) ;
			}
		}

		public synchronized void controllerUpdate( ControllerEvent control )
		{
			Player player = ( Player ) control.getSourceController() ;
			// If player wasn't created successfully from controller, return

			if( player == null ) {
				System.out.println( "Player is null" ) ;
				return ; }

			if( control instanceof RealizeCompleteEvent )
			{
				player.start() ;
			}

			if( control instanceof ControllerErrorEvent )
			{
				player.removeControllerListener( this ) ;
			}
		}
} // RTPPlayer ends