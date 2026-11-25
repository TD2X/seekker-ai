# s3-smoketest

Standalone check that an S3-compatible object storage (Neon Object Storage, Cloudflare R2, RustFS, ...)
behaves the way the seekker-ai design needs. It is **not** part of the application.

## Run

Create the bucket first (e.g. `seekker-pdfs-smoketest`), then export the credentials. Never commit them.

```bash
export AWS_ENDPOINT_URL_S3="https://<endpoint-from-provider>"
export AWS_ACCESS_KEY_ID="<access key>"
export AWS_SECRET_ACCESS_KEY="<secret key>"
export AWS_REGION="<region or us-east-1>"
export S3_BUCKET="<bucket name>"
# optional: S3_PATH_STYLE=false (virtual-hosted style), S3_TEST_SIZE_MB=10

cd tools/s3-smoketest
mvn -q compile exec:java
```

## What it checks

1. Put / Head / Get (streaming) of a ~10 MB PDF-like object, size and SHA-256 verified.
2. Missing key -> HTTP 404 `NoSuchKey` (permanent failure for the worker, vs transient errors).
3. Client timeouts really fire (verifies our client configuration, not the server).
4. Informational: `If-None-Match: *` on PutObject -> expect 412 on an existing key.

Exit code 0 only if checks 1-3 pass. Objects created under `smoketest/` are deleted at the end.
