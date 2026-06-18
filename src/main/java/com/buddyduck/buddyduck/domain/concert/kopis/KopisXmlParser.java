package com.buddyduck.buddyduck.domain.concert.kopis;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
class KopisXmlParser {

	private static final DateTimeFormatter KOPIS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	List<KopisConcertListItem> parseConcertList(String xml) {
		return dbElements(xml).stream()
			.map(this::toListItem)
			.flatMap(Optional::stream)
			.toList();
	}

	Optional<KopisConcertDetail> parseConcertDetail(String xml) {
		return firstDbElement(xml).flatMap(this::toConcertDetail);
	}

	Optional<KopisFacilityDetail> parseFacilityDetail(String xml) {
		return firstDbElement(xml).flatMap(this::toFacilityDetail);
	}

	private Optional<KopisConcertListItem> toListItem(Element element) {
		String externalId = text(element, "mt20id");
		String title = text(element, "prfnm");
		if (!StringUtils.hasText(externalId) || !StringUtils.hasText(title)) {
			return Optional.empty();
		}
		return Optional.of(new KopisConcertListItem(externalId, title));
	}

	private Optional<KopisConcertDetail> toConcertDetail(Element element) {
		String externalId = text(element, "mt20id");
		String title = text(element, "prfnm");
		String venueName = text(element, "fcltynm");
		String facilityId = text(element, "mt10id");
		Optional<LocalDate> startDate = parseDate(text(element, "prfpdfrom"));
		Optional<LocalDate> endDate = parseDate(text(element, "prfpdto"));
		String posterUrl = text(element, "poster");
		String area = text(element, "area");
		String genre = text(element, "genrenm");
		String timeGuidance = text(element, "dtguidance");

		if (!StringUtils.hasText(externalId)
			|| !StringUtils.hasText(title)
			|| !StringUtils.hasText(venueName)
			|| !StringUtils.hasText(facilityId)
			|| startDate.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new KopisConcertDetail(
			externalId,
			title,
			venueName,
			facilityId,
			startDate.get(),
			endDate.orElse(startDate.get()),
			trimToNull(posterUrl),
			trimToNull(area),
			trimToNull(genre),
			trimToNull(timeGuidance)
		));
	}

	private Optional<KopisFacilityDetail> toFacilityDetail(Element element) {
		String venueName = text(element, "fcltynm");
		Optional<BigDecimal> lat = parseDecimal(text(element, "la"));
		Optional<BigDecimal> lng = parseDecimal(text(element, "lo"));
		if (!StringUtils.hasText(venueName) || lat.isEmpty() || lng.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new KopisFacilityDetail(venueName, lat.get(), lng.get()));
	}

	private List<Element> dbElements(String xml) {
		Document document = parse(xml);
		NodeList nodes = document.getElementsByTagName("db");
		List<Element> elements = new ArrayList<>();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element element) {
				elements.add(element);
			}
		}
		return elements;
	}

	private Optional<Element> firstDbElement(String xml) {
		return dbElements(xml).stream().findFirst();
	}

	private Document parse(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			disableExternalEntities(factory);
			return factory.newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalArgumentException("Invalid KOPIS XML response", exception);
		}
	}

	private void disableExternalEntities(DocumentBuilderFactory factory) throws ParserConfigurationException {
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
	}

	private String text(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		if (nodes.getLength() == 0 || nodes.item(0) == null) {
			return null;
		}
		return nodes.item(0).getTextContent();
	}

	private Optional<LocalDate> parseDate(String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDate.parse(value.trim(), KOPIS_DATE_FORMATTER));
		} catch (DateTimeParseException exception) {
			return Optional.empty();
		}
	}

	private Optional<BigDecimal> parseDecimal(String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new BigDecimal(value.trim()));
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
