package com.systemonenoc.avatar.card.service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import com.systemonenoc.avatar.core.smartcard.MathUtilities;

public class ByteUtilities {
	
	public static byte[] concat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
	public static byte[] toByteArray(BigDecimal value, int length) {
		return ByteBuffer.allocate(length).putInt(MathUtilities.fromBigDecimalToInt(value)).array();
	}
	
	public static byte[] toByteArray(int value, int length) {
		return ByteBuffer.allocate(length).putShort((short) value).array();
	}
	
	public static byte[] toByteArray(int value) {
		byte[] result = {};
		do {
			int units = value % 100;
			int dec = units / 10;
			int unit = units % 10;
			byte[] pair = { (byte) ((16 * dec + unit) & 0xFF) };
			result = concat(pair, result);
			value = value / 100;
		} while (value > 0);
		
		return result;
	}
	
	public static byte[] toDecimalCounter(BigDecimal value) {
		byte[] intValue = toByteArray(value.intValue());
		byte[] intPart = { (byte) intValue.length };
		intPart = concat(intPart, intValue);
		
		BigDecimal remainder = value.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100));
		byte[] decValue = toByteArray(MathUtilities.fromBigDecimalToInt(remainder));
		byte[] decPart = { (byte) decValue.length };
		decPart = concat(decPart, decValue);
		
		byte[] result = { (byte) (intPart.length + decPart.length) };
		result = concat(result, intPart);
		result = concat(result, decPart);
		return result;
	}
}
