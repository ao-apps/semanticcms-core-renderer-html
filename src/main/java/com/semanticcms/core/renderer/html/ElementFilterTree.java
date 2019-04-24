/*
 * semanticcms-core-renderer-html - SemanticCMS pages rendered as HTML in a Servlet environment.
 * Copyright (C) 2016, 2017  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-renderer-html.
 *
 * semanticcms-core-renderer-html is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-renderer-html is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-renderer-html.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.renderer.html;

import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import static com.aoindustries.encoding.TextInXhtmlEncoder.textInXhtmlEncoder;
import com.aoindustries.net.UrlUtils;
import com.semanticcms.core.controller.CapturePage;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.ChildRef;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.CurrentCaptureLevel;
import com.semanticcms.core.pages.local.CurrentNode;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

/**
 * Builds a tree, filtering for a specific element type.
 */
final public class ElementFilterTree {

	/**
	 * A filter to select elements by arbitrary conditions.
	 */
	public static interface ElementFilter {

		/**
		 * Checks if matches.
		 */
		boolean matches(Element e);
	}

	/**
	 * A filter to select non-hidden and by element class.
	 */
	public static class ClassFilter implements ElementFilter {

		private final Class<? extends Element> elementType;

		public ClassFilter(Class<? extends Element> elementType) {
			this.elementType = elementType;
		}

		@Override
		public boolean matches(Element e) {
			return !e.isHidden() && elementType.isInstance(e);			
		}
	}

	private static boolean findElements(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		SemanticCMS semanticCMS,
		ElementFilter elementFilter,
		Set<Node> nodesWithMatches,
		Node node,
		boolean includeElements
	) throws ServletException, IOException {
		List<Element> childElements = node.getChildElements();
		boolean hasMatch;
		// Add self if is the target type
		if((node instanceof Element) && elementFilter.matches((Element)node)) {
			hasMatch = true;
		} else {
			hasMatch = false;
			for(Element childElem : childElements) {
				if(elementFilter.matches(childElem)) {
					hasMatch = true;
					break;
				}
			}
		}
		if(includeElements) {
			for(Element childElem : childElements) {
				if(findElements(servletContext, request, response, semanticCMS, elementFilter, nodesWithMatches, childElem, includeElements)) {
					hasMatch = true;
				}
			}
		} else {
			assert (node instanceof Page);
			if(!hasMatch) {
				// Not including elements, so any match from an element must be considered a match from the page the element is on
				Page page = (Page)node;
				for(Element e : page.getElements()) {
					if(elementFilter.matches(e)) {
						hasMatch = true;
						break;
					}
				}
			}
		}
		if(node instanceof Page) {
			for(ChildRef childRef : ((Page)node).getChildRefs()) {
				PageRef childPageRef = childRef.getPageRef();
				// Child is in an accessible book
				if(semanticCMS.getBook(childPageRef.getBookRef()).isAccessible()) {
					Page child = CapturePage.capturePage(servletContext, request, response, childPageRef, CaptureLevel.META);
					if(findElements(servletContext, request, response, semanticCMS, elementFilter, nodesWithMatches, child, includeElements)) {
						hasMatch = true;
					}
				}
			}
		}
		if(hasMatch) {
			nodesWithMatches.add(node);
		}
		return hasMatch;
	}

	private static void writeNode(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Node currentNode,
		Set<Node> nodesWithMatches,
		PageIndex pageIndex,
		Writer out,
		Node node,
		boolean includeElements
	) throws ServletException, IOException, SkipPageException {
		final Page page;
		final Element element;
		if(node instanceof Page) {
			page = (Page)node;
			element = null;
		} else if(node instanceof Element) {
			assert includeElements;
			element = (Element)node;
			page = element.getPage();
		} else {
			throw new AssertionError();
		}
		final PageRef pageRef = page.getPageRef();
		if(currentNode != null) {
			// Add page links
			currentNode.addPageLink(pageRef);
		}
		final String servletPath;
		if(out == null) {
			// Will be unused
			servletPath = null;
		} else {
			if(element == null) {
				servletPath = pageRef.getBookRef().getPrefix() + pageRef.getPath();
			} else {
				String elemId = element.getId();
				assert elemId != null;
				servletPath = pageRef.getBookRef().getPrefix() + pageRef.getPath() + '#' + elemId;
			}
		}
		if(out != null) {
			out.write("<li");
			HtmlRenderer htmlRenderer = HtmlRenderer.getInstance(servletContext);
			String listItemCssClass = htmlRenderer.getListItemCssClass(node);
			if(listItemCssClass != null) {
				out.write(" class=\"");
				encodeTextInXhtmlAttribute(listItemCssClass, out);
				out.write('"');
			}
			out.write("><a href=\"");
			Integer index = pageIndex==null ? null : pageIndex.getPageIndex(pageRef);
			if(index != null) {
				out.write('#');
				PageIndex.appendIdInPage(
					index,
					element==null ? null : element.getId(),
					new MediaWriter(textInXhtmlAttributeEncoder, out)
				);
			} else {
				encodeTextInXhtmlAttribute(
					response.encodeURL(
						UrlUtils.encodeUrlPath(
							request.getContextPath() + servletPath,
							response.getCharacterEncoding()
						)
					),
					out
				);
			}
			out.write("\">");
			node.appendLabel(new MediaWriter(textInXhtmlEncoder, out));
			if(index != null) {
				out.write("<sup>[");
				encodeTextInXhtml(Integer.toString(index+1), out);
				out.write("]</sup>");
			}
			out.write("</a>");
		}
		List<Node> childNodes = NavigationTreeRenderer.getChildNodes(servletContext, request, response, includeElements, true, node);
		childNodes = NavigationTreeRenderer.filterNodes(childNodes, nodesWithMatches);
		if(!childNodes.isEmpty()) {
			if(out != null) {
				out.write('\n');
				out.write("<ul>\n");
			}
			for(Node childNode : childNodes) {
				writeNode(servletContext, request, response, currentNode, nodesWithMatches, pageIndex, out, childNode, includeElements);
			}
			if(out != null) out.write("</ul>\n");
		}
		if(out != null) out.write("</li>\n");
	}

	// Traversal-based implementation is proving too complicated due to needing to
	// look ahead to know which elements to show.
	// TODO: Caching?
	public static void writeElementFilterTreeImpl(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Writer out,
		ElementFilter elementFilter,
		Node root,
		boolean includeElements
	) throws ServletException, IOException, SkipPageException {
		// Get the current capture state
		final CaptureLevel captureLevel = CurrentCaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			final Node currentNode = CurrentNode.getCurrentNode(request);
			// Filter by has files
			final Set<Node> nodesWithMatches = new HashSet<Node>();
			findElements(
				servletContext,
				request,
				response,
				SemanticCMS.getInstance(servletContext),
				elementFilter,
				nodesWithMatches,
				root,
				includeElements
			);
			if(captureLevel == CaptureLevel.BODY) out.write("<ul>\n");
			writeNode(
				servletContext,
				request,
				response,
				currentNode,
				nodesWithMatches,
				PageIndex.getCurrentPageIndex(request),
				captureLevel == CaptureLevel.BODY ? out : null,
				root,
				includeElements
			);
			if(captureLevel == CaptureLevel.BODY) out.write("</ul>\n");
		}
	}

	public static void writeElementFilterTreeImpl(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Writer out,
		Class<? extends Element> elementType,
		Node root,
		boolean includeElements
	) throws ServletException, IOException, SkipPageException {
		writeElementFilterTreeImpl(
			servletContext,
			request,
			response,
			out,
			new ClassFilter(elementType),
			root,
			includeElements
		);
	}

	/**
	 * Make no instances.
	 */
	private ElementFilterTree() {
	}
}