package com.mariposa.QBO.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mariposa.QBO.config.OAuth2PlatformClientFactory;

@Controller
public class HomeController {
	@Autowired
	OAuth2PlatformClientFactory factory;

	@PutMapping(path = "/updateQuickbooksEssentials", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> updateQuickbooksEssentials(@RequestBody String jsonString) {
		return factory.updateQuickbooksEssentials(jsonString);
	}

}
