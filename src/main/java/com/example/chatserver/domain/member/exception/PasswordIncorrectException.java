package com.example.chatserver.domain.member.exception;

import com.example.chatserver.global.common.error.BaseException;
import com.example.chatserver.global.common.error.ErrorCode;

public class PasswordIncorrectException extends BaseException {

	public PasswordIncorrectException() {
		super(ErrorCode.PASSWORD_INCORRECT);
	}
}
