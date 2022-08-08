# webmention4j

> [Webmention](https://www.w3.org/TR/webmention/) implementation in Java.

## Usage

### Client

The `client` module contains an implementation of a Webmention client which can be used to notify a Webmention endpoint server.

```java
import dev.rilling.webmention4j.client.WebmentionClient;
import dev.rilling.webmention4j.common.Webmention;

import java.io.IOException;
import java.net.URI;

public final class WebmentionClientExample {
	public static void main(String[] args) {
		URI source = URI.create("https://example.com/blog-item");
		URI target = URI.create("https://example.org/something-else");

		WebmentionClient webmentionClient = new WebmentionClient();
		try {
			if (!webmentionClient.supportsWebmention(target)) {
				System.out.println("No endpoint found for target URL.");
				return;
			}

			webmentionClient.sendWebmention(new Webmention(source, target));
			System.out.println("Success!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
```

### Server

The `server` module contains an implementation of a Webmention endpoint servlet which can be used to listen to Webmentions and process them.

```java
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.server.AbstractWebmentionEndpointServlet;

public static class LoggingWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {

	@Override
	protected void handleWebmention(Webmention webmention) {
		System.out.println("Received Webmention '" + webmention + "'.");
	}
}
```

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
