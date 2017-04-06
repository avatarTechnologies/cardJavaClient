package com.systemonenoc.avatar.card.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class CardService {
	
	private static final Logger logger = LoggerFactory.getLogger(CardService.class);
	
	static {
		try {
			fixPlatformPaths();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static final byte[] SELECT = { 0x00, (byte) 0xA4, 0x04, 0x00, (byte) 0x09, 0x41, 0x76, 0x61, (byte) 0x74,
			0x61, (byte) 0x72, 0x00, 0x00, 0x00 };
	public static final byte[] PIN = { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36 };
	public static final byte[] VERIFY_PIN = { (byte) 0xB0, 0x42, 0x00, 0x00, 0x06 };
	public static final byte[] GET_COUNTERS = { (byte) 0xB0, 0x76, 0x00, 0x00, 0x08 };
	public static final byte[] START_AUDIT = { (byte) 0xB0, 0x78, 0x00, 0x00, 0x08 };
	public static final byte[] VERIFY_AUDIT = { (byte) 0xB0, 0x79, 0x00, 0x00 };
	
	public static final byte[] SIGN_INVOICE_T = { (byte) 0xB0, 0x39, 0x00, 0x04, 0x00, 0x00 };
	
	public static final String PROTOCOL = "T=1";
	
	public static CardChannel getCardChannel() throws CardException {
		logger.debug("getCardChannel");
		
		TerminalFactory factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals = factory.terminals().list();
		CardTerminal terminal = terminals.get(0);
		Card card = terminal.connect(PROTOCOL);
		CardChannel channel = card.getBasicChannel();
		return channel;
	}
	
	public static Card getCard() throws CardException {
		logger.debug("getCard");
		
		TerminalFactory factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals = factory.terminals().list();
		logger.debug("Terminals {}", terminals);
		CardTerminal terminal = terminals.get(0);
		Card card = terminal.connect(PROTOCOL);
		logger.debug("card {}", card);
		return card;
	}
	
	public static boolean select(CardChannel channel) throws CardException {
		logger.debug("select");
		ResponseAPDU r = channel.transmit(new CommandAPDU(CardService.SELECT));
		return true;
	}
	
	public static byte[] verifyPin(CardChannel channel) throws CardException {
		logger.debug("verifyPin");
		
		byte[] combined = combine(CardService.VERIFY_PIN, CardService.PIN);
		ResponseAPDU r = channel.transmit(new CommandAPDU(combined));
		return Arrays.copyOfRange(r.getBytes(), 0, 8);
	}
	
	public static byte[] getCounters() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		return getCounters(channel, nonce);
	}
	
	public static byte[] getCounters(CardChannel channel, byte[] nonce) throws CardException {
		logger.debug("getCounters");
		
		byte[] combined = combine(CardService.GET_COUNTERS, nonce);
		ResponseAPDU r = channel.transmit(new CommandAPDU(combined));
		channel.getCard().disconnect(true);
		return Arrays.copyOf(r.getBytes(), r.getBytes().length - 2);
	}
	
	public static byte[] signInvoiceT(String tType, BigDecimal hb, BigDecimal mb, BigDecimal lb, BigDecimal zb,
			BigDecimal ht, BigDecimal mt, BigDecimal lt, BigDecimal zt, byte[] signature)
			throws CardException, IOException {
		logger.debug("signInvoiceT");
		
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		
		byte saleType = tType.equals("Sale") ? (byte) 0x00 : (byte) 0x01;
		byte[] request = { saleType, // Transaction type
				0x00 // Transaction mode - Normal/Proforma/Training
		};
		
		byte[] baseAmountCounter = ByteUtilities.toDecimalCounter(hb.add(mb).add(lb).add(zb));
		byte[] taxAmountCounter = ByteUtilities.toDecimalCounter(ht.add(mt).add(lt).add(zt));
		request = ByteUtilities.concat(request,
				new byte[] { (byte) (baseAmountCounter.length + taxAmountCounter.length) });
		request = ByteUtilities.concat(request, baseAmountCounter);
		request = ByteUtilities.concat(request, taxAmountCounter);
		byte[] digest = signature;
		request = ByteUtilities.concat(request, digest);
		
		byte[] data = { 0x01, // Cypher mode
				0x05, // Cypher Direction
				0x01 // Data location
		}; // Extended data
		data = ByteUtilities.concat(data, ByteUtilities.toByteArray(request.length, 2));
		data = ByteUtilities.concat(data, request);
		data = ByteUtilities.concat(data, nonce);
		byte[] result = ByteUtilities
				.concat(ByteUtilities.concat(CardService.SIGN_INVOICE_T, new byte[] { (byte) data.length }), data);
		return signInvoiceT(channel, result);
	}
	
	public static byte[] signInvoiceT(CardChannel channel, byte[] request) throws CardException {
		logger.debug("signInvoiceT {}", request);
		
		ResponseAPDU response = channel.transmit(new CommandAPDU(request));
		channel.getCard().disconnect(true);
		return Arrays.copyOf(response.getBytes(), response.getBytes().length - 2);
	}
	
	public static byte[] startAudit() throws CardException {
		logger.debug("startAudit");
		
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		return startAudit(channel, nonce);
	}
	
	public static void verifyAuditShort(byte[] signature) throws CardException {
		logger.debug("startAudit {}", signature.length);
		
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		
		byte[] comm1 = { (byte) 0xB0, (byte) 0x82,
				// False
				0x00,
				// 235
				(byte) 0xeb,
				// 245
				(byte) 0xf5,
				// offset 0
				0x00, 0x00 };
		byte[] command = ByteUtilities.concat(comm1,
				ByteUtilities.concat(Arrays.copyOfRange(signature, 0, 235), nonce));
		logger.debug("responseAPDU {}", Hex.encodeHexString(command));
		
		ResponseAPDU responseAPDU = channel.transmit(new CommandAPDU(command));
		logger.debug("responseAPDU {}", responseAPDU);
		
		byte[] comm2 = { (byte) 0xB0, (byte) 0x82,
				// True
				0x01,
				// 21
				(byte) 0x15,
				// 31
				(byte) 0x1f,
				// 235 offset
				0x00, (byte) 0xeb };
		command = ByteUtilities.concat(comm2, ByteUtilities.concat(Arrays.copyOfRange(signature, 235, 256), nonce));
		responseAPDU = channel.transmit(new CommandAPDU(command));
		logger.debug("responseAPDU {}", Hex.encodeHexString(command));
		logger.debug("responseAPDU {}", responseAPDU);
	}
	
	public static byte[] startAudit(CardChannel channel, byte[] nonce) throws CardException {
		logger.debug("startAudit {}", nonce);
		
		byte[] maxArr = { (byte) 0xB0, 0x78, 0x00, 0x00, 0x00, 0x00, 0x08 };
		byte[] combined = combine(maxArr, nonce);
		byte[] max2 = { (byte) 0xfa, (byte) 0xff };
		byte[] combined2 = combine(combined, max2);
		
		ResponseAPDU r = channel.transmit(new CommandAPDU(combined));
		logger.debug("startAudit {}", r);
		channel.getCard().disconnect(true);
		return Arrays.copyOf(r.getBytes(), r.getBytes().length - 2);
	}
	
	public static byte[] verifyAudit(byte[] token) throws CardException {
		logger.debug("verifyAudit");
		
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		return verifyAudit(channel, nonce, token);
	}
	
	public static byte[] verifyAudit(CardChannel channel, byte[] nonce, byte[] token) throws CardException {
		logger.debug("verifyAudit {}", nonce);
		
		byte[] comm = { (byte) 0xB0, 0x79, 0x00, 0x00 };
		byte[] variable = ByteUtilities.concat(new byte[] { 0x00 },
				short2ByteArr((short) (nonce.length + token.length)));
		
		byte[] combinedData = combine(token, nonce);
		byte[] combined2 = ByteUtilities.concat(ByteUtilities.concat(comm, variable), combinedData);
		
		ResponseAPDU response = channel.transmit(new CommandAPDU(combined2));
		logger.debug("verifyAudit {}", response);
		channel.getCard().disconnect(true);
		return response.getBytes();
	}
	
	public static byte[] signData(byte[] data) throws CardException {
		logger.debug("signData");
		
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		return signData(channel, nonce, data);
	}
	
	public static byte[] signData(CardChannel channel, byte[] nonce, byte[] data) throws CardException {
		logger.debug("signData {}", nonce);
		
		byte[] comm = { (byte) 0xB0, 0x33, 0x00, 0x00 };
		byte[] variable = ByteUtilities.concat(new byte[] { 0x00 },
				short2ByteArr((short) (nonce.length + data.length)));
		byte[] combinedData = combine(data, nonce);
		byte[] combined2 = ByteUtilities.concat(ByteUtilities.concat(comm, variable), combinedData);
		
		ResponseAPDU response = channel.transmit(new CommandAPDU(combined2));
		channel.getCard().disconnect(true);
		return Arrays.copyOfRange(response.getBytes(), 2, response.getBytes().length - 2);
	}
	
	public static byte[] short2ByteArr(short value) {
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(value);
		return buffer.array();
	}
	
	public static byte[] combine(byte[] a, byte[] b) {
		byte[] combined = new byte[a.length + b.length];
		System.arraycopy(a, 0, combined, 0, a.length);
		System.arraycopy(b, 0, combined, a.length, b.length);
		return combined;
	}
	
	/**
	 * El código de respuesta ISO7816 va en los dos últimos bytes
	 * 
	 * @param bytes
	 * @return
	 */
	public static short bytesToShort(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.put(bytes[bytes.length - 2]);
		bb.put(bytes[bytes.length - 1]);
		return bb.getShort(0);
	}
	
	static final String SUN_CLASS = "sun.security.smartcardio.SunPCSC";
	static final String JNA_CLASS = "jnasmartcardio.Smartcardio";
	
	static final String LIB_PROP = "sun.security.smartcardio.library";
	
	private static final String debian64_path = "/usr/lib/x86_64-linux-gnu/libpcsclite.so.1";
	private static final String ubuntu_path = "/lib/libpcsclite.so.1";
	private static final String ubuntu32_path = "/lib/i386-linux-gnu/libpcsclite.so.1";
	private static final String ubuntu64_path = "/lib/x86_64-linux-gnu/libpcsclite.so.1";
	private static final String freebsd_path = "/usr/local/lib/libpcsclite.so";
	private static final String fedora64_path = "/usr/lib64/libpcsclite.so.1";
	private static final String raspbian_path = "/usr/lib/arm-linux-gnueabihf/libpcsclite.so.1";
	
	/**
	 * https://github.com/martinpaljak/apdu4j/blob/master/src/apdu4j/TerminalManager.java
	 * Locates PC/SC shared library on the system and automagically sets system
	 * properties so that SunPCSC could find the smart card service. Call this
	 * before acquiring your TerminalFactory.
	 * 
	 * @throws Exception
	 */
	public static void fixPlatformPaths() throws Exception {
		if (System.getProperty(LIB_PROP) == null) {
			// Set necessary parameters for seamless PC/SC access.
			// http://ludovicrousseau.blogspot.com.es/2013/03/oracle-javaxsmartcardio-failures.html
			if (System.getProperty("os.name").equalsIgnoreCase("Linux")) {
				// Only try loading 64b paths if JVM can use them.
				if (System.getProperty("os.arch").contains("64")) {
					if (new File(debian64_path).exists()) {
						System.setProperty(LIB_PROP, debian64_path);
					} else if (new File(fedora64_path).exists()) {
						System.setProperty(LIB_PROP, fedora64_path);
					} else if (new File(ubuntu64_path).exists()) {
						System.setProperty(LIB_PROP, ubuntu64_path);
					}
				} else if (new File(ubuntu_path).exists()) {
					System.setProperty(LIB_PROP, ubuntu_path);
				} else if (new File(ubuntu32_path).exists()) {
					System.setProperty(LIB_PROP, ubuntu32_path);
				} else if (new File(raspbian_path).exists()) {
					System.setProperty(LIB_PROP, raspbian_path);
				} else {
					// XXX: dlopen() works properly on Debian OpenJDK 7
					// System.err.println("Hint: pcsc-lite probably missing.");
					throw new Exception("Hint: pcsc-lite probably missing.");
				}
			} else if (System.getProperty("os.name").equalsIgnoreCase("FreeBSD")) {
				if (new File(freebsd_path).exists()) {
					System.setProperty(LIB_PROP, freebsd_path);
				} else {
					System.err.println("Hint: pcsc-lite is missing. pkg install devel/libccid");
				}
			}
		} else {
			throw new Exception("LIB_PROP NOT FOUND. Supported platforms Linux/FreeBSD.");
		}
	}
}
