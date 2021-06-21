# Kafka Connect Http Connector

## Sink

The kafka connect http sink connector consumes records from the specified kafka topic into batches and, once the batch is complete, sends a http request to the provided url with a complete batch in the request body, where each record is converted into a string.

### Configuration

|Property | Description |
|--- | --- |
|`http.api.url`| Http api url where the data will be sent|
|`http.request.method`| Http request method|
|`http.headers`| Http headers to be included in all requests separated by the header.separator|
|`headers.separator`| Separator character used in headers property|
|`batch.size`| The number of records accumulated in a batch before the HTTP API will be invoked|
|`batch.prefix`| Prefix added to record batches that will be added at the beginning of the batch of records|
|`batch.suffix`| Suffix added to record batches that will be applied once at the end of the batch of records|
|`batch.separator`| Separator for records in a batch|
|`max.retries`| The maximum number of times to retry on errors before failing the task|
|`retry.backoff.timeout.ms`| The maximum time in milliseconds before failing the task|
|`retry.backoff.ms`| The duration in milliseconds to wait after an error before a retry attempt is made|
|`retry.backoff.exponential`| Enables exponential retry with base `retry.backoff.ms` and maximum retry timeout `retry.timeout.max.ms` |
|`retry.timeout.max.ms`| Maximum retry timeout for a single retry. Applicable only when `retry.backoff.exponential` is set to `true`|
|`regex.patterns`| Character separated regex patterns to match for replacement in the destination messages|
|`regex.replacements`| Character separated regex replacements to use with the patterns in regex.patterns|
|`regex.separator`| Separator character used in regex.patterns and regex.replacements property|
|`request.read.timeout.ms`|HTTP client request read timeout in milliseconds|
|`request.read.timeout.ms`|HTTP client request connect timeout in milliseconds|
|`auth.type`| HTTP authentication type. current supported options are oauth2 |
|`auth.header.name`| HTTP authentication name to be used. Defaults to `Authorization` |
|`auth.oauth2.client.id`| Oauth2 client id |
|`auth.oauth2.client.secret`| Oauth2 client secret |
|`auth.oauth2.token.url`| The target endpoint for generating the access token |

### Error handling

Errors on network level (read, connect) and http errors (>=400) will be propagated with status code/text (if applicable).
All network and http errors will be retried. (At-least-once delivery)