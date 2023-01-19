package com.mariposa.QBO.resource;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.AccountBasedExpenseLineDetail;
import com.intuit.ipp.data.AccountClassificationEnum;
import com.intuit.ipp.data.AccountSubTypeEnum;
import com.intuit.ipp.data.AccountTypeEnum;
import com.intuit.ipp.data.Bill;
import com.intuit.ipp.data.BillPayment;
import com.intuit.ipp.data.BillPaymentCheck;
import com.intuit.ipp.data.BillPaymentTypeEnum;
import com.intuit.ipp.data.CheckPayment;
import com.intuit.ipp.data.EmailAddress;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.Line;
import com.intuit.ipp.data.LineDetailTypeEnum;
import com.intuit.ipp.data.LinkedTxn;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.data.TxnTypeEnum;
import com.intuit.ipp.data.Vendor;
import com.intuit.ipp.data.VendorCredit;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.mariposa.QBO.config.OAuth2PlatformClientFactory;

import jakarta.servlet.http.HttpSession;

@Controller
public class BillController {
	@Autowired
	OAuth2PlatformClientFactory factory;
	private static final Logger logger = LoggerFactory.getLogger(BillController.class);
	private static final String ACCOUNT_QUERY = "select * from Account where AccountType='%s' maxresults 1";
	private static final String VENDOR_QUERY = "select * from vendor where DisplayName='%s'";

	/**
	 * Sample QBO API call using OAuth2 tokens
	 * 
	 * @param session
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/bill/{bill_line_amount}")
	public String callBillingConcept(HttpSession session, @PathVariable("bill_line_amount") BigDecimal billLineAMount)
			throws FMSException, ParseException {
		session = factory.updateSession(session);
		String realmId = (String) session.getAttribute("realmId");
		if (StringUtils.isEmpty(realmId)) {
			return new JSONObject()
					.put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!")
					.toString();
		}
		String accessToken = (String) session.getAttribute("access_token");
		try {
			DataService service = factory.getDataService(realmId, accessToken);

			Vendor vendor = getVendorFields();
			Vendor vendorOut = service.add(vendor);

			Bill bill = getBillFields(service, vendorOut, billLineAMount);
			Bill billOut = service.add(bill);

			BillPayment billPayment = getBillPaymentFields(service, billOut);
			service.add(billPayment);

			VendorCredit vendorCredit = getVendorCreditFields(service, vendorOut);
			VendorCredit vendorCreditOut = service.add(vendorCredit);
			return createResponse(vendorCreditOut);
		} catch (FMSException e) {
			List<Error> list = e.getErrorList();
			list.forEach(error -> logger.error("Error while calling the API :: " + error.getMessage()));
			return new JSONObject().put("response", "Failed").toString();
		}
	}

	/**
	 * Prepare Vendor request
	 * 
	 * @return
	 */
	private Vendor getVendorFields() {
		Vendor vendor = new Vendor();
		// Mandatory Fields
		vendor.setDisplayName("SaurabhAffiliates");
		EmailAddress email = new EmailAddress();
		email.setAddress("saurabhmishra@qsstechnosoft.com");
		vendor.setPrimaryEmailAddr(email);
		return vendor;
	}

	/**
	 * Prepare Bill request
	 * 
	 * @param service
	 * @param vendor
	 * @return
	 * @throws FMSException
	 */
	private Bill getBillFields(DataService service, Vendor vendor, BigDecimal lineAmount) throws FMSException {

		Bill bill = new Bill();
		bill.setVendorRef(factory.createRef(vendor));

		Account liabilityAccount = getLiabilityBankAccount(service);
		bill.setAPAccountRef(factory.createRef(liabilityAccount));

		final LineDetailTypeEnum LINE_DETAIL_TYPE = LineDetailTypeEnum.ACCOUNT_BASED_EXPENSE_LINE_DETAIL;
		Line line1 = factory.createLine(lineAmount, LINE_DETAIL_TYPE);

		AccountBasedExpenseLineDetail detail = new AccountBasedExpenseLineDetail();
		Account account = getExpenseBankAccount(service);
		ReferenceType expenseAccountRef = factory.createRef(account);
		detail.setAccountRef(expenseAccountRef);
		line1.setAccountBasedExpenseLineDetail(detail);

		List<Line> lines1 = new ArrayList<Line>();
		lines1.add(line1);
		bill.setLine(lines1);

		bill.setTotalAmt(new BigDecimal("30.00"));

		return bill;
	}

	/**
	 * Prepare BillPayment request
	 * 
	 * @param service
	 * @param bill
	 * @return
	 * @throws FMSException
	 */
	private BillPayment getBillPaymentFields(DataService service, Bill bill) throws FMSException {
		BillPayment billPayment = new BillPayment();

		billPayment.setVendorRef(bill.getVendorRef());

		Line line1 = new Line();
		line1.setAmount(new BigDecimal("30"));
		List<LinkedTxn> linkedTxnList1 = new ArrayList<LinkedTxn>();
		LinkedTxn linkedTxn1 = new LinkedTxn();
		linkedTxn1.setTxnId(bill.getId());
		linkedTxn1.setTxnType(TxnTypeEnum.BILL.value());
		linkedTxnList1.add(linkedTxn1);
		line1.setLinkedTxn(linkedTxnList1);

		List<Line> lineList = new ArrayList<Line>();
		lineList.add(line1);
		billPayment.setLine(lineList);

		BillPaymentCheck billPaymentCheck = new BillPaymentCheck();
		Account bankAccount = getCheckBankAccount(service);
		billPaymentCheck.setBankAccountRef(factory.createRef(bankAccount));

		billPaymentCheck.setCheckDetail(getCheckPayment());

		billPayment.setCheckPayment(billPaymentCheck);
		billPayment.setPayType(BillPaymentTypeEnum.CHECK);
		billPayment.setTotalAmt(new BigDecimal("30"));
		return billPayment;
	}

	/**
	 * Prepare VendorCredit Request
	 * 
	 * @param service
	 * @param vendor
	 * @return
	 * @throws FMSException
	 */
	private VendorCredit getVendorCreditFields(DataService service, Vendor vendor) throws FMSException {

		VendorCredit vendorCredit = new VendorCredit();
		vendorCredit.setVendorRef(factory.createRef(vendor));

		Account account = getLiabilityBankAccount(service);
		vendorCredit.setAPAccountRef(factory.createRef(account));

		Line line1 = new Line();
		line1.setAmount(new BigDecimal("30.00"));
		line1.setDetailType(LineDetailTypeEnum.ACCOUNT_BASED_EXPENSE_LINE_DETAIL);
		AccountBasedExpenseLineDetail detail = new AccountBasedExpenseLineDetail();
		Account expenseAccount = getExpenseBankAccount(service);
		detail.setAccountRef(factory.createRef(expenseAccount));
		line1.setAccountBasedExpenseLineDetail(detail);

		List<Line> lines1 = new ArrayList<Line>();
		lines1.add(line1);
		vendorCredit.setLine(lines1);

		return vendorCredit;
	}

	/**
	 * Get Bank Account
	 * 
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account getCheckBankAccount(DataService service) throws FMSException {
		QueryResult queryResult = service.executeQuery(String.format(ACCOUNT_QUERY, AccountTypeEnum.BANK.value()));
		List<? extends IEntity> entities = queryResult.getEntities();
		if (!entities.isEmpty()) {
			return (Account) entities.get(0);
		}
		return createBankAccount(service);
	}

	/**
	 * Create Bank Account
	 * 
	 * @param service
	 * @return
	 * @throws FMSException
	 * @throws ParseException
	 */
	private Account createBankAccount(DataService service) throws FMSException {
		Account account = new Account();
		account.setName("Ba" + RandomStringUtils.randomAlphanumeric(7));
		account.setClassification(AccountClassificationEnum.ASSET);
		account.setAccountType(AccountTypeEnum.BANK);
		return service.add(account);
	}

	/**
	 * Get Expense Account
	 * 
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account getExpenseBankAccount(DataService service) throws FMSException {
		QueryResult queryResult = service.executeQuery(String.format(ACCOUNT_QUERY, AccountTypeEnum.EXPENSE.value()));
		List<? extends IEntity> entities = queryResult.getEntities();
		if (!entities.isEmpty()) {
			return (Account) entities.get(0);
		}
		return createExpenseBankAccount(service);
	}

	/**
	 * Create Expense Account
	 * 
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account createExpenseBankAccount(DataService service) throws FMSException {
		final String ACCOUNT_NAME = "Main_Expense_Account";
		final AccountClassificationEnum ACCOUNT_CLASSIFICATION = AccountClassificationEnum.EXPENSE;
		final AccountSubTypeEnum ACCOUNT_SUB_TYPE = AccountSubTypeEnum.PAYROLL_EXPENSES;
		Account account = factory.createAccount(ACCOUNT_NAME, ACCOUNT_SUB_TYPE, ACCOUNT_CLASSIFICATION);
		return service.add(account);
	}

	/**
	 * Get AP account
	 * 
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account getLiabilityBankAccount(DataService service) throws FMSException {

		QueryResult queryResult = service
				.executeQuery(String.format(ACCOUNT_QUERY, AccountTypeEnum.ACCOUNTS_PAYABLE.value()));
		List<? extends IEntity> entities = queryResult.getEntities();
		if (!entities.isEmpty()) {
			return (Account) entities.get(0);
		}
		return createLiabilityBankAccount(service);
	}

	/**
	 * Create AP account
	 * 
	 * @param service
	 * @return
	 * @throws FMSException
	 */
	private Account createLiabilityBankAccount(DataService service) throws FMSException {
		final String ACCOUNT_NAME = "Main_Account_Payable";
		final AccountClassificationEnum ACCOUNT_CLASSIFICATION = AccountClassificationEnum.LIABILITY;
		final AccountSubTypeEnum ACCOUNT_SUB_TYPE = AccountSubTypeEnum.ACCOUNTS_PAYABLE;
		Account account = factory.createAccount(ACCOUNT_NAME, ACCOUNT_SUB_TYPE, ACCOUNT_CLASSIFICATION);
		return service.add(account);
	}

	/**
	 * Prepare CheckPayment request
	 * 
	 * @return
	 * @throws FMSException
	 */
	private CheckPayment getCheckPayment() throws FMSException {
		String uuid = RandomStringUtils.randomAlphanumeric(8);

		CheckPayment checkPayment = new CheckPayment();
		checkPayment.setAcctNum("AccNum" + uuid);
		checkPayment.setBankName("BankName" + uuid);
		checkPayment.setCheckNum("CheckNum" + uuid);
		checkPayment.setNameOnAcct("Name" + uuid);
		checkPayment.setStatus("Status" + uuid);
		return checkPayment;
	}

	/**
	 * Map object to json string
	 * 
	 * @param entity
	 * @return
	 */
	private String createResponse(Object entity) {
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString;
		try {
			jsonInString = mapper.writeValueAsString(entity);
		} catch (JsonProcessingException e) {
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
		return jsonInString;
	}

	private String createErrorResponse(Exception e) {
		logger.error("Exception while calling QBO ", e);
		return new JSONObject().put("response", "Failed").toString();
	}

}
