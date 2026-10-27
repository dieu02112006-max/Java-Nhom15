package com.example.demo.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String walletName) {
        super("Ví \"" + walletName + "\" không đủ số dư");
    }
}