package com.systemonenoc.avatar.card;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import com.systemonenoc.avatar.card.service.CardService;

@SuppressWarnings("restriction")
@Ignore
public class TestCard {
	
	private static final Logger logger = LoggerFactory.getLogger(TestCard.class);
	
	static final String LIB_PROP = "sun.security.smartcardio.library";
	private static final String ubuntu64_path = "/lib/x86_64-linux-gnu/libpcsclite.so.1";
	
	CardService cardService;
	
	@Test
	public void getCardChannel() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		Assert.notNull(channel);
	}
	
	@Test
	public void select() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		Assert.isTrue(CardService.select(channel));
	}
	
	@Test
	public void verifyPin() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		Assert.notNull(nonce);
	}
	
	@Test
	public void getCounters() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		byte[] counters = CardService.getCounters(channel, nonce);
		Assert.notNull(counters);
	}
	
	@Test
	public void startAudit() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		byte[] auditData = CardService.startAudit(channel, nonce);
		Assert.notNull(auditData);
	}
	
	@Test
	public void signData() throws CardException {
		CardChannel channel = CardService.getCardChannel();
		CardService.select(channel);
		byte[] nonce = CardService.verifyPin(channel);
		
		byte[] data = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00 };
		byte[] response = CardService.signData(channel, nonce, data);
		Assert.notNull(response);
	}
	
	String tokenSign = "LJpCU1uDsr9uWUMNcTolP0RtC7jJMlTsBpuGNEtsBn8ECd7ICfkq+fNQWUTaz+WiSHz6TKEAECJBXy/D+BjlcotOCuKjKfyCpfiXScZBT5IkJqhwfmDI5q46NBmZea7SZOqG0tqZ25Klsj8i4xT4AmzVsACtpAc5sXZKMC3XLjcGXKuehjvWpPZMaP0X9Nr4GAgLEdbt4LjP1Cig73UjY1upflboDWVERzW8i8reMEs87sTQErF8j1F6x/r5btelAvwinIsxduJfrHKEI+ru4lwS/TuBhA3p6JRosC5H/W/Cdix3XELFQobHf5pAmoN6aF6+Diwrl+wPmc37QLdcRw==";
	
	@Test
	public void verifyAuditShort() throws CardException {
		CardService.verifyAuditShort(Base64Utils.decodeFromString(tokenSign));
	}
	
}
