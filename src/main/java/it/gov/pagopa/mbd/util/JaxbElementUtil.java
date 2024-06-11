package it.gov.pagopa.mbd.util;

import it.gov.pagopa.mbd.exception.AppError;
import it.gov.pagopa.mbd.exception.AppException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JaxbElementUtil {

    public <T> T convertToBean(byte[] xml, Class<T> targetType) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml);
            JAXBContext context = JAXBContext.newInstance(targetType);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<T> jaxbElement = unmarshaller.unmarshal(new StreamSource(byteArrayInputStream),targetType);
            byteArrayInputStream.close();
            return jaxbElement.getValue();
        } catch (JAXBException | IOException e) {
            throw new AppException(e, AppError.PARSING_GENERIC_ERROR, e.getMessage());
        }
    }
    public <T> T convertToBean(String xml, Class<T> targetType) {
        return convertToBean(xml.getBytes(StandardCharsets.UTF_8),targetType);
    }

}
