package com.mariposa.QBO.resource;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.ipp.data.CompanyInfo;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.mariposa.QBO.config.OAuth2PlatformClientFactory;

import jakarta.servlet.http.HttpSession;

@Controller
public class CompanyInfoController {
	@Autowired
	OAuth2PlatformClientFactory factory;
	private static final Logger logger = LoggerFactory.getLogger(CompanyInfoController.class);
	private static final String failureMsg = "Failed";

	/**
	 * Sample QBO API call using OAuth2 tokens
	 * 
	 * @param session
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getCompanyInfo")
	public String callQBOCompanyInfo(HttpSession session) {
		session = factory.updateSession(session);
		String realmId = (String) session.getAttribute("realmId");
		if (StringUtils.isEmpty(realmId)) {
			return new JSONObject()
					.put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!")
					.toString();
		}
		String accessToken = (String) session.getAttribute("access_token");
		DataService service;
		QueryResult queryResult = null;
		try {
			String sql = "select * from companyinfo";
			service = factory.getDataService(realmId, accessToken);
			queryResult = service.executeQuery(sql);
		} catch (FMSException e) {
			logger.error("error extracting query result from quickbooks api : " + e.getMessage());
		}
		return processResponse(failureMsg, queryResult);
	}

	private String processResponse(String failureMsg, QueryResult queryResult) {
		if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
			CompanyInfo companyInfo = (CompanyInfo) queryResult.getEntities().get(0);
			logger.info("Companyinfo -> CompanyName: " + companyInfo.getCompanyName());
			ObjectMapper mapper = new ObjectMapper();
			try {
				String jsonInString = mapper.writeValueAsString(companyInfo);
				return jsonInString;
			} catch (JsonProcessingException e) {
				logger.error("Exception while getting company info ", e);
				return new JSONObject().put("response", failureMsg).toString();
			}

		}
		return failureMsg;
	}

}
