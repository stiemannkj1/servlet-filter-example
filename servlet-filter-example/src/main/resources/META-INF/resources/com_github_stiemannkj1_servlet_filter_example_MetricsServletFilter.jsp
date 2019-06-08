<%@page contentType="text/html" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Metrics</title>
    </head>
    <body>
        <h1>Metrics</h1>
        <ul>
            <li><strong>Minimum Response Size:</strong> ${minimumResponseSize}</li>
            <li><strong>Maximum Response Size:</strong> ${maximumResponseSize}</li>
            <li><strong>Average Response Size:</strong> ${averageResponseSize}</li>
        </ul>
    </body>
</html>
