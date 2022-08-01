# webmention4j

> [Webmention](https://www.w3.org/TR/webmention/) implementation in Java.

## Usage

### Client

The `client` module contains an implementation of a Webmention client which can be used to notify a Webmention endpoint server.

### Server

The `server` module contains an implementation of a Webmention endpoint servlet which can be used to listen to Webmentions and process them.

### Examples

The `example` module contains example CLI applications that can be executed.

#### Client CLI Example

Webmention client CLI sending Webmention for `http://localhost:8080/somethingelse` being mentioned on `http://localhost:8080/blogpost`:

```shell
java -cp webmention4j-example-*.jar dev.rilling.webmention4j.example.WebmentionClientExample --source https://example.com/somethingelse --target https://example.org/blogpost
```

Instead of `--target`, the `--crawl` flag may be used to send Webmentions for all pages linked in the source page.

```shell
java -cp webmention4j-example-*.jar dev.rilling.webmention4j.example.WebmentionClientExample --source https://example.com/somethingelse --crawl
```

For all options, see the `--help` flag.

#### Server CLI Example

Webmention endpoint server CLI that logs incoming Webmentions:

```shell
java -cp webmention4j-example-*.jar dev.rilling.webmention4j.example.WebmentionEndpointServletExample
```

For all options, see the `--help` flag.
