/*
 * XPathUtil.java
 * This file is part of RunSimulator
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * RunSimulator is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * RunSimulator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RunSimulator. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ncl.giacomobergami.utils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;

public class XPathUtil {

    XPathFactory xpathfactory;
    XPath xpath;
    private static XPathUtil self = null;

    public XPathUtil() {
        xpathfactory = XPathFactory.newInstance();
        xpath = xpathfactory.newXPath();
    }

    public static XPathUtil getInstance() {
        if (self == null) {
            self = new XPathUtil();
        }
        return self;
    }

    public static String evaluate(Document doc, String xpath_expr) throws XPathExpressionException {
        XPathExpression expr = getInstance().xpath.compile(xpath_expr);
        return expr.evaluate(doc);
    }

    public static NodeList evaluateNodeList(Document doc, String xpath_expr) throws XPathExpressionException {
        XPathExpression expr = getInstance().xpath.compile(xpath_expr);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        return (NodeList) result;
    }

}
