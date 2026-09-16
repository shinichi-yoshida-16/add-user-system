package com.fcuro.userreg.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * ログインフォームで入力されたスプレッドシートIDを、標準の認証情報(IPアドレス等)に
 * 加えて保持するための拡張。
 */
public class SheetAuthenticationDetails extends WebAuthenticationDetails {

    private final String spreadsheetId;
    private final String sheetName;

    public SheetAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.spreadsheetId = request.getParameter("spreadsheetId");
        this.sheetName = request.getParameter("sheetName");
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public String getSheetName() {
        return sheetName;
    }
}
