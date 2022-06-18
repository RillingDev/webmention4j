# webmention4j

> [Webmention](https://www.w3.org/TR/webmention/) implementation in Java.

## Usage

### Client

The `client` module contains an implementation of a Webmention client which can be used to notify a Webmention endpoint server.

### Server

The `server` module contains an implementation of a Webmention endpoint servlet which can be used to listen to Webmention notifications and process them.

### Examples

The `example` module contains example applications that can be executed.

#### Client Example

Webmention client sending Webmention for `http://localhost:8080/somethingelse` being mentioned on `http://localhost:8080/blogpost`:

```shell
java -cp webmention4j-example-*.jar dev.rilling.webmention4j.example.WebmentionClientExample http://localhost:8080/somethingelse http://localhost:8080/blogpost
```

#### Server Example

Webmention endpoint server that logs incoming Webmentions:

```shell
java -cp webmention4j-example-*.jar dev.rilling.webmention4j.example.WebmentionEndpointServletExample
```
