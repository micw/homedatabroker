package de.wyraz.homedatabroker.source;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import de.wyraz.homedatabroker.source.ModBusIPSource.ModBusRegFormat;

public class ModBusRegFormatTest {
	
	@Test
	public void testDecodeInt16() {
		assertThat(ModBusRegFormat.int16.decode(new byte[] {(byte)0xFF,(byte)0xFF}, 0))
			.isEqualTo(-1);
		assertThat(ModBusRegFormat.int16.decode(new byte[] {(byte)0x80,(byte)0x00}, 0))
			.isEqualTo(-32768);
		assertThat(ModBusRegFormat.int16.decode(new byte[] {(byte)0x7F,(byte)0xFF}, 0))
			.isEqualTo(32767);
		assertThat(ModBusRegFormat.int16.decode(new byte[] {(byte)0x00,(byte)0x01}, 0))
			.isEqualTo(1);
	}
	@Test
	public void testDecodeUint16() {
		assertThat(ModBusRegFormat.uint16.decode(new byte[] {(byte)0xFF,(byte)0xFF}, 0))
			.isEqualTo(65535);
		assertThat(ModBusRegFormat.uint16.decode(new byte[] {(byte)0x80,(byte)0x00}, 0))
			.isEqualTo(32768);
		assertThat(ModBusRegFormat.uint16.decode(new byte[] {(byte)0x7F,(byte)0xFF}, 0))
			.isEqualTo(32767);
		assertThat(ModBusRegFormat.uint16.decode(new byte[] {(byte)0x00,(byte)0x01}, 0))
			.isEqualTo(1);
	}

	@Test
	public void testDecodeInt32() {
		assertThat(ModBusRegFormat.int32.decode(new byte[] {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0))
			.isEqualTo(-1);
		assertThat(ModBusRegFormat.int32.decode(new byte[] {(byte)0x80,(byte)0x00,(byte)0x00,(byte)0x00}, 0))
			.isEqualTo(-2147483648);
		assertThat(ModBusRegFormat.int32.decode(new byte[] {(byte)0x7F,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0))
			.isEqualTo(2147483647);
		assertThat(ModBusRegFormat.int32.decode(new byte[] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01}, 0))
			.isEqualTo(1);
	}

	@Test
	public void testDecodeUint32() {
		assertThat(ModBusRegFormat.uint32.decode(new byte[] {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0))
			.isEqualTo(4294967295L);
		assertThat(ModBusRegFormat.uint32.decode(new byte[] {(byte)0x80,(byte)0x00,(byte)0x00,(byte)0x00}, 0))
			.isEqualTo(2147483648L);
		assertThat(ModBusRegFormat.uint32.decode(new byte[] {(byte)0x7F,(byte)0xFF,(byte)0xFF,(byte)0xFF}, 0))
			.isEqualTo(2147483647L);
		assertThat(ModBusRegFormat.uint32.decode(new byte[] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01}, 0))
			.isEqualTo(1L);
	}
	
}
