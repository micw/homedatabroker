package de.wyraz.homedatabroker.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JSONHelper {
	
	protected static final ObjectMapper RELAXED_MAPPER=JsonMapper
			.builder()
			.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
			.build();
	
	/**
	 * Parses the input as JSON. Returns a flat list of key/value pairs.
	 * Structures are flattened as following:
	 * 
	 * 1. Simple keys
	 * 
	 * Input: {"key1":"value1","key2":"value2"}
	 * Result: key1=value1, key2=value2
	 * 
	 * 2. Objects
	 * 
	 * Input: {"parent":{"key1":"value1","key2":"value2"}}
	 * Result: parent.key1=value1, parent.key2=value2
	 * 
	 * 3. Arrays
	 * 
	 * Input: {"key":["value1",{"subkey":"value2"}]}
	 * Result: key.1=value1, key.2.subkey=value2
	 * 
	 * @param jsonData
	 * @return
	 */
	public static Map<String,String> parseAndFlattenJSON(byte[] jsonData) {
		try {
			Map<String,String> result=new TreeMap<>();
			flatten(result,null,RELAXED_MAPPER.readTree(jsonData));
			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static void flatten(Map<String,String> result, String prefix,JsonNode json) {
		if (json.isObject()) {
			for (Iterator<Entry<String, JsonNode>> i=json.fields();i.hasNext();) {
				Entry<String, JsonNode> e=i.next();
				flatten(result,(prefix==null)?e.getKey():(prefix+'.'+e.getKey()),e.getValue());
			}
			return;
		}
		if (json.isArray()) {
			
			int num=0;
			for (Iterator<JsonNode> i=json.elements();i.hasNext();) {
				JsonNode n=i.next();
				num++;
				flatten(result,(prefix==null)?Integer.toString(num):(prefix+'.'+num),n);
			}
			return;
		}
		
		if (json.isNull()) {
			return;
		}
		
		if (json.isValueNode()) {
			if (prefix==null) {
				prefix="VALUE";
			}
			result.put(prefix, json.asText());
			return;
		}
		
		// TODO: unhandled JSON?
		
		
	}

}
