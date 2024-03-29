/*
 * semanticcms-core-renderer-html - SemanticCMS pages rendered as HTML in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020, 2021, 2022  AO Industries, Inc.
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
 * along with semanticcms-core-renderer-html.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.core.renderer.html;

import com.aoapps.collections.AoCollections;
import com.semanticcms.core.controller.CapturePage;
import com.semanticcms.core.controller.PageUtils;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.pages.CaptureLevel;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilities for working with {@link HtmlRenderer}.
 */
public final class HtmlRendererUtils {

  /** Make no instances. */
  private HtmlRendererUtils() {
    throw new AssertionError();
  }

  /**
   * Gets all the parents of the given page that are not in missing books
   * and are applicable to the given view.
   *
   * @return  The filtered set of parents, in the order declared by the page.
   */
  public static Set<Page> getApplicableParents(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      View view,
      Page page
  ) throws ServletException, IOException {
    Collection<Page> parents = CapturePage.capturePages(
        servletContext,
        request,
        response,
        PageUtils.filterNotMissingBook(servletContext, page.getParentRefs()),
        CaptureLevel.META // TODO: View provide capture level required for isApplicable check, might be PAGE or (null for none) for some views.
    ).values();
    Set<Page> applicableParents = AoCollections.newLinkedHashSet(parents.size());
    for (Page parent : parents) {
      if (view.isApplicable(servletContext, request, response, parent)) {
        applicableParents.add(parent);
      }
    }
    return AoCollections.optimalUnmodifiableSet(applicableParents);
  }
}
