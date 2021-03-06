package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.messages.AttestationProtos.*;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.rat.NonceGenerator;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationConsumerHandler;

public class UnixSocketTest {
	
    static String SOCKET_PATH = "tpm2sim/socket/control.sock";
	private UnixSocketThread client;
	private Thread thread;
	private static UnixSocketResponsHandler handler;

    @Test
    public void testSocketConnection() throws Exception {
    	try {
			// client will be used to send messages
			client = new UnixSocketThread(SOCKET_PATH);
			thread = new Thread(client);
			//this.thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			System.out.println("could not write to/read from " + SOCKET_PATH);
			e.printStackTrace();
		}
    	// construct protobuf message to send to local tpm2d via unix socket
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(IdsAttestationType.BASIC)
				.setQualifyingData(NonceGenerator.generate())
				.setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
				.build();
		client.send(msg.toByteArray(), UnixSocketTest.handler);
		// and wait for response
		TpmToController response = TpmToController.parseFrom(UnixSocketTest.handler.waitForResponse());
		//System.out.println(response.toString());
		assertTrue(response.getCode().equals(TpmToController.Code.INTERNAL_ATTESTATION_RES));
    }
}
