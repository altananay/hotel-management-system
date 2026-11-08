package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.SupportedCurrency;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class TcmbFxParser {

    private static final DateTimeFormatter TCMB_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public TcmbRates parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new TcmbParseException("TCMB XML is empty.");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document =
                    factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            LocalDate rateDate = extractRateDate(document);
            Map<SupportedCurrency, BigDecimal> rates = new EnumMap<>(SupportedCurrency.class);
            NodeList currencies = document.getElementsByTagName("Currency");
            for (int index = 0; index < currencies.getLength(); index++) {
                Element currency = (Element) currencies.item(index);
                String code = currency.getAttribute("Kod");
                if (code == null || code.isBlank()) {
                    code = currency.getAttribute("CurrencyCode");
                }
                SupportedCurrency supported = currencyFrom(code);
                if (supported == null) {
                    continue;
                }
                String selling = childText(currency, "ForexSelling");
                if (selling == null || selling.isBlank()) {
                    continue;
                }
                rates.put(supported, parseAmount(selling));
            }
            return new TcmbRates(rateDate, Map.copyOf(rates));
        } catch (TcmbParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TcmbParseException("TCMB XML could not be parsed.", ex);
        }
    }

    private static LocalDate extractRateDate(Document document) {
        Element root = document.getDocumentElement();
        String date = root.getAttribute("Tarih");
        if (date == null || date.isBlank()) {
            NodeList tarihNodes = document.getElementsByTagName("Tarih");
            if (tarihNodes.getLength() > 0) {
                date = tarihNodes.item(0).getTextContent();
            }
        }
        if (date == null || date.isBlank()) {
            throw new TcmbParseException("TCMB XML does not contain a rate date.");
        }
        try {
            return LocalDate.parse(date.trim(), TCMB_DATE);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(date.trim());
            } catch (DateTimeParseException second) {
                throw new TcmbParseException("TCMB rate date is invalid: " + date, second);
            }
        }
    }

    private static String childText(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        if (children.getLength() == 0) {
            return null;
        }
        Node node = children.item(0);
        return node.getTextContent();
    }

    private static BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new TcmbParseException("TCMB FX value is invalid: " + value, ex);
        }
    }

    private static SupportedCurrency currencyFrom(String code) {
        if ("EUR".equalsIgnoreCase(code)) {
            return SupportedCurrency.EUR;
        }
        if ("USD".equalsIgnoreCase(code)) {
            return SupportedCurrency.USD;
        }
        return null;
    }
}
