/*
 * ao-web-page-servlet - Java API for modeling web page content and relationships in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-page-servlet.
 *
 * ao-web-page-servlet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-page-servlet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-page-servlet.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.web.page.servlet;

import com.aoindustries.net.HttpParameters;
import com.aoindustries.servlet.http.NullHttpServletResponseWrapper;
import com.aoindustries.web.page.servlet.impl.LinkImpl;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

public class Link {

	private final ServletContext servletContext;
	private final HttpServletRequest request;
	private final HttpServletResponse response;

	private String book;
	private String page;
	private String element;
	private String view;
	private HttpParameters params;
	private String clazz;

	public Link(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		this.servletContext = servletContext;
		this.request = request;
		this.response = response;
	}

	public Link(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String page
	) {
		this(servletContext, request, response);
		this.page = page;
	}

	public Link(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String book,
		String page
	) {
		this(servletContext, request, response, page);
		this.book = book;
	}

	public Link(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String book,
		String page,
		String element
	) {
		this(servletContext, request, response, book, page);
		this.element = element;
	}

	/**
	 * Creates a new link in the current page context.
	 *
	 * @see  PageContext
	 */
	public Link() {
		this(
			PageContext.getServletContext(),
			PageContext.getRequest(),
			PageContext.getResponse()
		);
	}

	/**
	 * Creates a new link in the current page context.
	 *
	 * @see  PageContext
	 */
	public Link(String page) {
		this();
		this.page = page;
	}

	/**
	 * Creates a new link in the current page context.
	 *
	 * @see  PageContext
	 */
	public Link(String book, String page) {
		this(page);
		this.book = book;
	}

	/**
	 * Creates a new link in the current page context.
	 *
	 * @see  PageContext
	 */
	public Link(String book, String page, String element) {
		this(book, page);
		this.element = element;
	}

	public Link book(String book) {
		this.book = book;
		return this;
	}

	public Link page(String page) {
		this.page = page;
		return this;
	}

	public Link element(String element) {
		this.element = element;
		return this;
	}

	public Link view(String view) {
		this.view = view;
		return this;
	}

	public Link params(HttpParameters params) {
		this.params = params;
		return this;
	}

	public Link clazz(String clazz) {
		this.clazz = clazz;
		return this;
	}

	public static interface Body {
		void doBody(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, SkipPageException;
	}

	/**
	 * <p>
	 * Also establishes a new {@link PageContext}.
	 * </p>
	 *
	 * @see  PageContext
	 */
	public void invoke(Body body) throws ServletException, IOException, SkipPageException {
		LinkImpl.writeLinkImpl(
			servletContext,
			request,
			response,
			response.getWriter(),
			book,
			page,
			element,
			view,
			params,
			clazz,
			body == null
				? null
				// Lamdba version not working with generic exceptions:
				// discard -> body.doBody(request, discard ? new NullHttpServletResponseWrapper(response) : response)
				: new LinkImpl.LinkImplBody<ServletException>() {
					@Override
					public void doBody(boolean discard) throws ServletException, IOException, SkipPageException {
						if(discard) {
							HttpServletResponse newResponse = new NullHttpServletResponseWrapper(response);
							// Set PageContext
							PageContext.newPageContext(
								servletContext,
								request,
								newResponse,
								() -> body.doBody(request, newResponse)
							);
						} else {
							body.doBody(request, response);
						}
					}
				}
		);
	}

	/**
	 * @see  #invoke(com.aoindustries.web.page.servlet.Link.Body)
	 */
	public void invoke() throws ServletException, IOException, SkipPageException {
		invoke((Body)null);
	}

	public static interface PageContextBody {
		void doBody() throws ServletException, IOException, SkipPageException;
	}

	/**
	 * @see  #invoke(com.aoindustries.web.page.servlet.Link.Body)
	 */
	public void invoke(PageContextBody body) throws ServletException, IOException, SkipPageException {
		invoke(
			body == null
				? null
				: (req, resp) -> body.doBody()
		);
	}
}
