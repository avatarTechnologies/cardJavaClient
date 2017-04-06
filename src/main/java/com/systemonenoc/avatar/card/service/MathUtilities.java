package com.systemonenoc.avatar.card.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

public class MathUtilities {
	public static BigDecimal fromStringToBigDecimal(String value) {
		if (value.isEmpty()) {
			return BigDecimal.ZERO;
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		String pattern = "###0.0#";
		DecimalFormat format = new DecimalFormat(pattern, symbols);
		format.setParseBigDecimal(true);
		
		try {
			return ((BigDecimal) format.parse(value)).setScale(2, RoundingMode.HALF_UP);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static int fromBigDecimalToInt(BigDecimal value) {
		return value.setScale(0, RoundingMode.HALF_UP).intValueExact();
	}
	
	public static BigDecimal fromIntToBigDecimal(int value) {
		return new BigDecimal(value);
	}
	
	public static BigDecimal applyVat(BigDecimal value, BigDecimal vatRate) {
		return value.multiply(vatRate).divide(BigDecimal.valueOf(100));
	}
	
	public static BigDecimal getProductTotalPrice(BigDecimal unitPrice, BigDecimal quantity, BigDecimal discount) {
		if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
			discount = BigDecimal.valueOf(100);
		}
		return quantity.multiply(unitPrice).multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100))))
				.setScale(2, RoundingMode.HALF_UP);
	}
}
