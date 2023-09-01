package de.wyraz.homedatabroker.source;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import de.wyraz.tibberpulse.sml.SMLDecoder;
import de.wyraz.tibberpulse.sml.SMLMeterData;
import jakarta.validation.constraints.NotEmpty;

public class TibberPulseHttpSource extends AbstractScheduledSource {
	
	protected boolean ignoreCrcErrors;
	
	@NotEmpty
	protected String pulseUrl;

	protected String pulseUsername;

	protected String pulsePassword;

	protected final CloseableHttpClient http = HttpClients.createDefault();

	@Override
	protected void schedule() {
		RequestBuilder get = RequestBuilder.get(pulseUrl);
		if (!StringUtils.isAnyBlank(pulseUsername, pulsePassword)) {
			get.addHeader("Authorization","Basic "+Base64.encodeBase64String((pulseUsername+":"+pulsePassword).getBytes()))		;
		}

		try (CloseableHttpResponse resp = http.execute(get.build())) {
			if (resp.getStatusLine().getStatusCode() != 200) {
				log.warn("Invalid response from tibber pulse gateway endpoint: {}",resp.getStatusLine());
				return;
			}
			byte[] payload;
			try {
				payload=EntityUtils.toByteArray(resp.getEntity());
				
				if (payload.length==0) {
					log.debug("Received no data");
					return;
				}
				
			} catch (Exception ex) {
				log.warn("Unable to extract payload from response",ex);
				return;
			}
			
			SMLMeterData data;
			try {
				data=SMLDecoder.decode(payload, !ignoreCrcErrors);
			} catch (Exception ex) {
				log.warn("Unable to parse SML from response",ex);
				return;
			}
			
			if (data!=null) {
				for (SMLMeterData.Reading r: data.getReadings()) {
					publishMetric(StringUtils.firstNonBlank(r.getName(),r.getObisCode()), r.getValue(), r.getUnit());
				}
			}
			
		} catch (Exception ex) {
			log.warn("Unable to fetch data from tibber pulse bridge",ex);
			return;
		}

	}

}
