package de.fhg.ids.comm.unixsocket;

import java.math.BigInteger;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;

public class UnixSocketResponsHandler {
	private Logger LOG = LoggerFactory.getLogger(UnixSocketResponsHandler.class);
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized byte[] waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
		int length = new BigInteger(pop(this.rsp, 4, true)).intValue();
		return slice(slice(this.rsp, 4, true), length, false);
	}
	
	static byte[] slice(byte[] original, int length, boolean fromLeft) {
		if(fromLeft) {
			return Arrays.copyOfRange(original, length, original.length);
		}
		else {
			return Arrays.copyOfRange(original, original.length - length, original.length);
		}
	}
	
	static byte[] pop(byte[] original, int length, boolean fromLeft) {
		if(fromLeft) {
			return Arrays.copyOfRange(original, 0, length);
		}
		else {
			return Arrays.copyOfRange(original, original.length - length, length);
		}
	}
}

