package com.mariposa.QBO.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.AccountBasedExpenseLineDetail;
import com.intuit.ipp.data.AccountClassificationEnum;
import com.intuit.ipp.data.AccountSubTypeEnum;
import com.intuit.ipp.data.Currency;
import com.intuit.ipp.data.CurrencyCode;
import com.intuit.ipp.data.EmailAddress;
import com.intuit.ipp.data.IntuitEntity;
import com.intuit.ipp.data.Line;
import com.intuit.ipp.data.LineDetailTypeEnum;
import com.intuit.ipp.data.PhysicalAddress;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.data.TelephoneNumber;
import com.intuit.ipp.data.Vendor;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.services.ReportService;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.data.PlatformResponse;
import com.intuit.oauth2.exception.ConnectionException;
import com.intuit.oauth2.exception.OAuthException;

import jakarta.servlet.http.HttpSession;

@Service
@PropertySource(value = "classpath:/application.properties", ignoreResourceNotFound = true)
public class OAuth2PlatformClientFactory {
	@Autowired
	private org.springframework.core.env.Environment env;
	private OAuth2PlatformClient client;
	private OAuth2Config oauth2Config;
	private String accessToken = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiZGlyIn0."
			+ ".XL8ft9x2il7-kT-B49T9kg.ETOQuSdboXgqP7WL9Tpi0TALqVGtQK2ghQzkxMPFmpT"
			+ "ZUheDHsPg1ikS9wQa_aLexI99b3DPE9VVzD1904sbVSGTQ1mZckQu-g36Mqes11Wwh3"
			+ "HJMlS20EGgy9RC34UvyMxWyv7RLIoZThVe3oSHz3mjIs75A21yvo7iQY21xoc2rtbdi"
			+ "YzxRycO8031VORJPhhvuC2RhptGs5vZDUmDKwDIKzY4-AclgIbkIIRhN8ckts-JTKL7"
			+ "P832XWdZmW0t1tZG_h53GJh1RlsJ3q5giHVIdVXZ6zs4laVJMFowjjVztJgTgs8JXjf"
			+ "ZonfVSKQEAlwkucgHS2zhrE9MLe0_YNSP_CFDXbmQeB0zNrCjmZPu_FK0bpMGJKEhoI"
			+ "qG3onMdEAUnWAQu-Tp-Lqxr4fq9eFjqmxOpB6FDuKwVyzxa4G9rITiPr8Nz09GsghZz"
			+ "C-3dki0YykSKrOwfOtR0WhQon5J4te70AuhEQlAppNa-WvRibajn2SwsDXlKtvd64D5"
			+ "Y0gJ-fZVPRFkg6Ls8LgfGeIPGn4dK5LwEa9yuNtVZmxBl3dr77dO5fGWdlgwguVFFi7"
			+ "H5ByFxOLiKtZGa8Q9DB2WttI5HEymbhFG4uU1_f5mW2bOEe7wSI1-wrYE5ESlOjm8-3"
			+ "FjFaBCiETic6WSz7PyEDneiKcc9xJoYt_A87CZ6aR1uFqPEEauh46t5Lgr9TyzmlrJ5"
			+ "_nqZZ-4wX7VOdRDQcbuL2Hx4cAP_A9yqZE7REg7T0VCWQzvLneLIIEFyclJKMvTaHl9"
			+ "_akxyqJCVIpcpRu08vCrmN7bq6iTPlJop67maI5l4mlNMjoQjSNMEr1wdWy52yqo-k1"
			+ "l2HhJn5rnjaiTFLF1MOwfPMkPZPVnow2v8OC3Dp4uqB5zxrGk._P2npto7yyUaqoMYP" + "QIZ1A";
	private String refreshToken = "AB11682701153Df5m6jKmBk4zPCP163kvl2UwWzGPfSwAad5SN";
	private String realmId = "4620816365249144170";
	private Logger logger = LoggerFactory.getLogger(getClass());

	public ResponseEntity<Object> updateQuickbooksEssentials(String jsonString) {
		logger.info("updating quickbooks tokens");
		Map<String, Object> response = new HashMap<>();
		String log = null;
		if (StringUtils.isEmpty(jsonString)) {
			log = "error extracting quickbooks essentials : empty string";
			logger.error(log);
			response.put("message", log);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		JSONObject json = new JSONObject(jsonString);
		try {
			String realmId = json.getString("realmId");
			setRealmId(realmId);
			String accessToken = json.getString("accessToken");
			setAccessToken(accessToken);
			String refreshToken = json.getString("refreshToken");
			setRefreshToken(refreshToken);
		} catch (JSONException e) {
			log = "error extracting quickbooks essentials : " + e.getMessage();
			logger.error(log);
			response.put("message", log);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		log = "success in updating quickbooks essentials";
		logger.info(log);
		Map<String, Object> data = new HashMap<>();
		data.put("realmId", getRealmId());
		data.put("access_token", getAccessToken());
		data.put("refresh_token", getRefreshToken());
		response.put("data", data);
		response.put("message", log);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public HttpSession updateSession(HttpSession session) {
		logger.info("updating session with quickbooks essentials");
		session.setAttribute("realmId", getRealmId());
		session.setAttribute("refresh_token", getRefreshToken());

		BearerTokenResponse bearerTokenResponse;
		try {
			bearerTokenResponse = getOAuth2PlatformClient()
					.refreshToken(session.getAttribute("refresh_token").toString());
			session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
			session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());
		} catch (OAuthException e) {
			logger.error("error refreshing quickbooks token : " + e.getMessage());
		}
		return session;
	}

	public HttpSession refreshTokens(HttpSession session, String refreshToken) {
		logger.info("refreshing tokens");
		BearerTokenResponse bearerTokenResponse;
		try {
			bearerTokenResponse = getOAuth2PlatformClient().refreshToken(refreshToken);
			session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
			session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());
		} catch (OAuthException e) {
			logger.error("error refreshing quickbooks token : " + e.getMessage());
		}
		return session;
	}

	public String revokeToken(HttpSession session) {
		try {
			OAuth2PlatformClient client = getOAuth2PlatformClient();
			String refreshToken = (String) session.getAttribute("refresh_token");
			PlatformResponse response = client.revokeToken(refreshToken);
			logger.info("raw result for revoke token request= " + response.getStatus());
			return new JSONObject().put("response", "Revoke successful").toString();
		} catch (ConnectionException ex) {
			logger.error("ConnectionException while calling refreshToken ", ex);
			logger.error("intuit_tid: " + ex.getIntuit_tid());
			logger.error("More info: " + ex.getResponseContent());
			return new JSONObject().put("response", ex.getResponseContent()).toString();
		} catch (Exception ex) {
			logger.error("Exception while calling revokeToken ", ex);
			return new JSONObject().put("response", "failed").toString();
		}
	}

	public OAuth2PlatformClient getOAuth2PlatformClient() {
		client = new OAuth2PlatformClient(getOAuth2Config());
		return client;
	}

	public OAuth2Config getOAuth2Config() {
		oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(env.getProperty("OAuth2AppClientId"),
				env.getProperty("OAuth2AppClientSecret")) // set client id, secret
				.callDiscoveryAPI(Environment.SANDBOX) // call discovery API to populate urls
				.buildConfig();
		return oauth2Config;
	}

	public DataService getDataService(String realmId, String accessToken) throws FMSException {
		Context context = prepareContext(realmId, accessToken);
		return new DataService(context);
	}

	private Context prepareContext(String realmId, String accessToken) throws FMSException {
		String url = getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";
		Config.setProperty(Config.BASE_URL_QBO, url);
		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
		Context context = new Context(oauth, ServiceType.QBO, realmId);
		return context;
	}

	public ReportService getReportService(String realmId, String accessToken) throws FMSException {
		Context context = prepareContext(realmId, accessToken);
		return new ReportService(context);
	}

	/**
	 * This method is an overloaded with duplication of the QBOServiceHelper to pass
	 * in a different minor version. This is due to an invoice update failing with
	 * the discount line for minor version "23" (SDK default), hence overriding with
	 * the minor version "4"
	 *
	 * @param realmId
	 * @param accessToken
	 * @return
	 * @throws FMSException
	 */
	public DataService getDataService(String realmId, String accessToken, String minorVersion) throws FMSException {
		Context context = prepareContext(realmId, accessToken);
		context.setMinorVersion(minorVersion);
		return new DataService(context);
	}

	/**
	 * Creates reference type for an entity
	 * 
	 * @param entity - IntuitEntity object inherited by each entity
	 * @return
	 */
	public ReferenceType createRef(IntuitEntity entity) {
		ReferenceType referenceType = new ReferenceType();
		referenceType.setValue(entity.getId());
		return referenceType;
	}

	public Vendor createVendor(String displayName, String emailAddress, String countryCode, String mobileNumber,
			String city, String country, String postalCode, String line1, String line2, String line3, String line4,
			String line5) {
		Vendor vendor = new Vendor();
		vendor.setDisplayName(displayName);
		vendor.setPrimaryEmailAddr(createEmailAddress(emailAddress));
		vendor.setPrimaryPhone(createTelePhoneNumber(countryCode, mobileNumber));
		vendor.setCompanyName(displayName);
		vendor.setBillAddr(createPhysicalAddress(city, country, postalCode, line1, line2, line3, line4, line5));
		return vendor;
	}

	public TelephoneNumber createTelePhoneNumber(String countryCode, String mobileNumber) {
		TelephoneNumber phoneNumber = new TelephoneNumber();
		phoneNumber.setCountryCode(countryCode);
		phoneNumber.setFreeFormNumber(mobileNumber);
		return phoneNumber;
	}

	public EmailAddress createEmailAddress(String emailAddress) {
		EmailAddress email = new EmailAddress();
		email.setAddress(emailAddress);
		return email;
	}

	public PhysicalAddress createPhysicalAddress(String city, String country, String postalCode, String line1,
			String line2, String line3, String line4, String line5) {
		PhysicalAddress address = new PhysicalAddress();
		if (!StringUtils.isEmpty(line1))
			address.setLine1(line1);
		if (!StringUtils.isEmpty(line2))
			address.setLine2(line2);
		if (!StringUtils.isEmpty(line3))
			address.setLine3(line3);
		if (!StringUtils.isEmpty(line4))
			address.setLine4(line4);
		if (!StringUtils.isEmpty(line5))
			address.setLine5(line5);
		address.setCity(city);
		address.setCountry(country);
		address.setPostalCode(postalCode);
		return address;
	}

	public Line createLine(BigDecimal amount, LineDetailTypeEnum detailtype) {
		Line line = new Line();
		line.setAmount(amount);
		line.setDetailType(detailtype);
		return line;
	}

	public Account createAccount(String name, AccountSubTypeEnum accountSubType,
			AccountClassificationEnum accountClassificationEnum) {
		final String DOMAIN = "QBO";
		final String CURRENCY_NAME = "United States Dollar";
		final CurrencyCode CURRENCY_CODE = CurrencyCode.USD;
		Account account = new Account();

		account.setName(name);
		account.setFullyQualifiedName(name);
		account.setDomain(DOMAIN);

		Currency currency = new Currency();
		currency.setName(CURRENCY_NAME);
		currency.setCode(CURRENCY_CODE);
		account.setCurrencyRef(createRef(currency));

		account.setSubAccount(false);
		account.setClassification(accountClassificationEnum);
		account.setAccountSubType(accountSubType.value());
		return account;
	}

	/**
	 * Queries data from QuickBooks
	 * 
	 * @param session
	 * @param sql
	 * @return
	 */
	public List<? extends IEntity> queryData(HttpSession session, String sql) {
		session = updateSession(session);
		String realmId = (String) session.getAttribute("realmId");
		if (StringUtils.isEmpty(realmId)) {
			logger.error("Relam id is null ");
		}
		String accessToken = (String) session.getAttribute("access_token");

		DataService service;
		QueryResult queryResult = null;
		try {
			service = getDataService(realmId, accessToken);
			queryResult = service.executeQuery(sql);
		} catch (FMSException e) {
			logger.error("error getting results : " + e.getMessage());
		}
		return queryResult.getEntities();
	}

	public String getPropertyValue(String proppertyName) {
		return env.getProperty(proppertyName);
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getRealmId() {
		return realmId;
	}

	public void setRealmId(String realmId) {
		this.realmId = realmId;
	}
}
