<%--
The MIT License

Copyright 2019 Kyle Stiemann.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
--%>
<%@page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Metrics</title>
        <style>
            table {
                border-collapse: collapse;
            }

            table, th, td {
                border: 1px solid black;
            }
        </style>
    </head>
    <body>
        <h1>Metrics</h1>
        <ul>
            <li><strong>Minimum Servlet Response Size (in bytes):</strong> <span id="minimumResponseSize">${minimumResponseSize}</span></li>
            <li><strong>Maximum Servlet Response Size (in bytes):</strong> <span id="maximumResponseSize">${maximumResponseSize}</span></li>
            <li><strong>Average Servlet Response Size (in bytes):</strong> <span id="averageResponseSize">${averageResponseSize}</span></li>
        </ul>
        <ul>
            <li><strong>Minimum Servlet Response Time (in nanoseconds):</strong> <span id="minimumResponseTime">${minimumResponseTime}</span></li>
            <li><strong>Maximum Servlet Response Time (in nanoseconds):</strong> <span id="maximumResponseTime">${maximumResponseTime}</span></li>
            <li><strong>Average Servlet Response Time (in nanoseconds):</strong> <span id="averageResponseTime">${averageResponseTime}</span></li>
        </ul>
        <table>
            <caption>Historical Response Data</caption>
            <thead>
                <tr>
                    <th>Response Id</th>
                    <th>Servlet Response Time (in nanoseconds)</th>
                    <th>Response Size (in bytes)</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${responseMetrics}" var="responseInfo">
                    <tr>
                        <td align="right">${responseInfo.key}</td>
                        <td align="right">${responseInfo.value.get(0)}</td>
                        <td align="right">${responseInfo.value.get(1)}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </body>
</html>
