# Kafka Connect Http Connector

## Sink

The kafka connect http sink connector consumes records from the specified kafka topic into batches and, once the batch is complete, sends a http request to the provided url with a complete batch in the request body, where each record is converted into a string.

### Configuration

|`http.api.url`|http api url where the data will be sent|
|`http.request.method`|http Request Method|
|`http.headers`|http headers to be included in all requests separated by the header.separator|
|`headers.separator`|Separator character used in headers property|
|`batch.size`|the number of records accumulated in a batch before the HTTP API will be invoked|
|`batch.prefix`|prefix added to record batches that will be added at the beginning of the batch of records|
|`batch.suffix`|suffix added to record batches that will be applied once at the end of the batch of records|
|`batch.separator`|separator for records in a batch|
|`max.retries`|the maximum number of times to retry on errors before failing the task|
|`retry.backoff.ms`|the duration in milliseconds to wait after an error before a retry attempt is made|
|`regex.patterns`|character separated regex patterns to match for replacement in the destination messages|
|`regex.replacements`|character separated regex replacements to use with the patterns in regex.patterns|
|`regex.separator`|separator character used in regex.patterns and regex.replacements property.|
