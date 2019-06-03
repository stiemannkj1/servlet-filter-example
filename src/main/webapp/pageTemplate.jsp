<%@page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>page${param.pageNumber}.jsp</title>
    </head>
    <body>
        <h1>page${param.pageNumber}.jsp</h1>
        <p>Hello World!</p>
        <jsp:useBean id="today" class="java.util.Date" scope="page" />
        <p>It is <fmt:formatDate value="${today}" pattern="yyyy-MM-dd HH:mm:ss"
            /></p>
        <a href="./index.jsp">Back to index.jsp.</a>
    </body>
</html>
