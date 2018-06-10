package com.philip.cssFilter;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CustomResponseWrapper extends HttpServletResponseWrapper {

	public CustomResponseWrapper(HttpServletResponse response) {
		super(response);
		// TODO Auto-generated constructor stub
		output = new CharArrayWriter();
	}

	private CharArrayWriter output;

	public String getResponseContent() {
		return output.toString();
	}

	public PrintWriter getWriter() {
		return new PrintWriter(output);
	}
}
