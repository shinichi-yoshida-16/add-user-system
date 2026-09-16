package com.fcuro.userreg.repository;

/**
 * Google Sheets APIへのアクセス中に発生したエラーをラップする実行時例外。
 */
public class SheetAccessException extends RuntimeException {

    public SheetAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
