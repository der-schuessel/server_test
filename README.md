# server_test

Simple multithreaded Http server written in plain Java. Client can query files locally stored on the server. The server runs on localhost:8080.

The client specifies which file to download by giving an absolut path and a filename as parameter.

e.g. localhost:8080/file?path=/home/user/Documents&filename=simple_html.html

Handling of HTTP ETag, If-Match, If-Non-Match, If-Modified-Since headers is implemented.
