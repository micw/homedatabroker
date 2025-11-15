package de.wyraz.homedatabroker.source.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.wyraz.homedatabroker.source.AggregationSource.AggregatedMetricInput;

public class AggregationTypeTest {
	
	private AggregatedMetricInput createInput(String source, Number value, ZonedDateTime timestamp) {
		AggregatedMetricInput input = new AggregatedMetricInput(source);
		if (value != null) {
			input.update(value, timestamp);
		}
		return input;
	}
	
	@Test
	public void testSumWithAllValidInputs() {
		ZonedDateTime now = ZonedDateTime.now();
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now));
		inputs.add(createInput("input2", 20.3, now));
		inputs.add(createInput("input3", 5.2, now));
		
		Number result = AggregationType.sum.apply(inputs, null, false);
		
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(36.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithOneNullInput() {
		ZonedDateTime now = ZonedDateTime.now();
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now));
		inputs.add(createInput("input2", null, null)); // Kein Wert
		inputs.add(createInput("input3", 5.2, now));
		
		Number result = AggregationType.sum.apply(inputs, null, false);
		
		// Aktuelles Verhalten: Summe wird trotzdem berechnet (nur mit den vorhandenen Werten)
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(15.7, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithOneNullInputRequireAll() {
		ZonedDateTime now = ZonedDateTime.now();
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now));
		inputs.add(createInput("input2", null, null)); // Kein Wert
		inputs.add(createInput("input3", 5.2, now));
		
		Number result = AggregationType.sum.apply(inputs, null, true);
		
		// Mit requireAllInputs=true sollte null zurückgegeben werden
		assertThat(result).isNull();
	}
	
	@Test
	public void testSumWithAllNullInputs() {
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", null, null));
		inputs.add(createInput("input2", null, null));
		inputs.add(createInput("input3", null, null));
		
		Number result = AggregationType.sum.apply(inputs, null, false);
		
		// Aktuelles Verhalten: Gibt 0 zurück
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(0.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithAllNullInputsRequireAll() {
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", null, null));
		inputs.add(createInput("input2", null, null));
		inputs.add(createInput("input3", null, null));
		
		Number result = AggregationType.sum.apply(inputs, null, true);
		
		// Mit requireAllInputs=true sollte null zurückgegeben werden
		assertThat(result).isNull();
	}
	
	@Test
	public void testSumWithExpiredInputs() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oldTime = now.minusMinutes(5);
		ZonedDateTime expireTs = now.minusMinutes(1); // Expire nach 1 Minute
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now)); // Aktuell
		inputs.add(createInput("input2", 20.3, oldTime)); // Abgelaufen
		inputs.add(createInput("input3", 5.2, now)); // Aktuell
		
		Number result = AggregationType.sum.apply(inputs, expireTs, false);
		
		// Aktuelles Verhalten: Nur die nicht-abgelaufenen Werte werden summiert
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(15.7, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithExpiredInputsRequireAll() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oldTime = now.minusMinutes(5);
		ZonedDateTime expireTs = now.minusMinutes(1); // Expire nach 1 Minute
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now)); // Aktuell
		inputs.add(createInput("input2", 20.3, oldTime)); // Abgelaufen
		inputs.add(createInput("input3", 5.2, now)); // Aktuell
		
		Number result = AggregationType.sum.apply(inputs, expireTs, true);
		
		// Mit requireAllInputs=true sollte null zurückgegeben werden, da ein Input abgelaufen ist
		assertThat(result).isNull();
	}
	
	@Test
	public void testSumWithAllExpiredInputs() {
		ZonedDateTime oldTime = ZonedDateTime.now().minusMinutes(5);
		ZonedDateTime expireTs = ZonedDateTime.now().minusMinutes(1);
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, oldTime));
		inputs.add(createInput("input2", 20.3, oldTime));
		inputs.add(createInput("input3", 5.2, oldTime));
		
		Number result = AggregationType.sum.apply(inputs, expireTs, false);
		
		// Aktuelles Verhalten: Gibt 0 zurück wenn alle abgelaufen sind
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(0.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithAllExpiredInputsRequireAll() {
		ZonedDateTime oldTime = ZonedDateTime.now().minusMinutes(5);
		ZonedDateTime expireTs = ZonedDateTime.now().minusMinutes(1);
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, oldTime));
		inputs.add(createInput("input2", 20.3, oldTime));
		inputs.add(createInput("input3", 5.2, oldTime));
		
		Number result = AggregationType.sum.apply(inputs, expireTs, true);
		
		// Mit requireAllInputs=true sollte null zurückgegeben werden
		assertThat(result).isNull();
	}
	
	@Test
	public void testSumWithNoExpiration() {
		ZonedDateTime oldTime = ZonedDateTime.now().minusHours(10);
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, oldTime));
		inputs.add(createInput("input2", 20.3, oldTime));
		inputs.add(createInput("input3", 5.2, oldTime));
		
		Number result = AggregationType.sum.apply(inputs, null, false); // Keine Expiration
		
		// Ohne Expiration sollten alle Werte berücksichtigt werden
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(36.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithEmptyInputs() {
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		
		Number result = AggregationType.sum.apply(inputs, null, false);
		
		// Leere Input-Liste sollte 0 ergeben
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(0.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithEmptyInputsRequireAll() {
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		
		Number result = AggregationType.sum.apply(inputs, null, true);
		
		// Leere Input-Liste sollte auch mit requireAllInputs=true 0 ergeben (keine fehlenden Inputs)
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(0.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithNegativeValues() {
		ZonedDateTime now = ZonedDateTime.now();
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now));
		inputs.add(createInput("input2", -20.3, now));
		inputs.add(createInput("input3", 5.2, now));
		
		Number result = AggregationType.sum.apply(inputs, null, false);
		
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(-4.6, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithIntegerValues() {
		ZonedDateTime now = ZonedDateTime.now();
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10, now));
		inputs.add(createInput("input2", 20, now));
		inputs.add(createInput("input3", 5, now));
		
		Number result = AggregationType.sum.apply(inputs, null, false);
		
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(35.0, org.assertj.core.data.Offset.offset(0.01));
	}
	
	@Test
	public void testSumWithAllValidInputsRequireAll() {
		ZonedDateTime now = ZonedDateTime.now();
		
		List<AggregatedMetricInput> inputs = new ArrayList<>();
		inputs.add(createInput("input1", 10.5, now));
		inputs.add(createInput("input2", 20.3, now));
		inputs.add(createInput("input3", 5.2, now));
		
		Number result = AggregationType.sum.apply(inputs, null, true);
		
		// Wenn alle Inputs valide sind, sollte auch mit requireAllInputs=true die normale Summe zurückgegeben werden
		assertThat(result)
			.isNotNull()
			.isInstanceOf(BigDecimal.class);
		assertThat(result.doubleValue())
			.isEqualTo(36.0, org.assertj.core.data.Offset.offset(0.01));
	}
}
