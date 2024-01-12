package de.wyraz.homedatabroker.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthcheckController {
	
	@RequestMapping(path = "/healthcheck")
	public ResponseEntity<String> checkHealth() {
		return ResponseEntity.ok("OK");
	}

}
