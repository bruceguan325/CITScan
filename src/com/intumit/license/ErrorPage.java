package com.intumit.license;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ErrorPage {
    public static final String ERROR_PAGE = "/ShowMessage.jsp";
    public static final String MESSAGE_KEY = "message-key";
    public static final String MESSAGE = "message";
    public static final String EXCEPTION = "exception";

    public static void toDisplayException(Exception e, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setAttribute(EXCEPTION, e);
        request.getRequestDispatcher(ERROR_PAGE).forward(request, response);
    }

    public static void toDisplayMessage(String message, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setAttribute(MESSAGE, message);
        request.getRequestDispatcher(ERROR_PAGE).forward(request, response);
    }

    public static void toDisplayMessageKey(String messageKey, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setAttribute(MESSAGE_KEY, messageKey);
        request.getRequestDispatcher(ERROR_PAGE).forward(request, response);
    }

}
