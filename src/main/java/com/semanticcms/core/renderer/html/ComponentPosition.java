/*
 * semanticcms-core-renderer-html - SemanticCMS pages rendered as HTML in a Servlet environment.
 * Copyright (C) 2016, 2017, 2022, 2023, 2024  AO Industries, Inc.
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

import com.aoapps.web.resources.registry.Script.Position;

/**
 * The set of allowed component locations.
 *
 * @see Position
 */
public enum ComponentPosition {

  /**
   * Components called just after the head opening tag.
   * These components are called in the order registered.
   *
   * <p>These are after any scripts in {@link Position#HEAD_START}.</p>
   */
  HEAD_START(Position.HEAD_START),

  /**
   * Components called just before the head closing tag.
   * These components are called in the reverse order registered.
   *
   * <p>These are before any scripts in {@link Position#HEAD_END}.</p>
   */
  HEAD_END(Position.HEAD_END),

  /**
   * Components called just after the body opening tag.
   * These components are called in the order registered.
   *
   * <p>These are after any scripts in {@link Position#BODY_START}.</p>
   */
  BODY_START(Position.BODY_START),

  /**
   * Components called just before the body closing tag.
   * These components are called in the reverse order registered.
   *
   * <p>These are before any scripts in {@link Position#BODY_END}.</p>
   */
  BODY_END(Position.BODY_END);

  private final Position scriptPosition;

  private ComponentPosition(Position scriptPosition) {
    this.scriptPosition = scriptPosition;
  }

  /**
   * Gets the related script position.
   */
  public Position getScriptPosition() {
    return scriptPosition;
  }
}
