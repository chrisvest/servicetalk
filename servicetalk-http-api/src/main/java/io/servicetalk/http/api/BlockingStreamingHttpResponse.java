/*
 * Copyright © 2018, 2021 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.http.api;

import io.servicetalk.buffer.api.Buffer;
import io.servicetalk.concurrent.BlockingIterable;
import io.servicetalk.concurrent.api.Publisher;
import io.servicetalk.concurrent.api.Single;
import io.servicetalk.concurrent.api.internal.CloseableIteratorBufferAsInputStream;
import io.servicetalk.context.api.ContextMap;
import io.servicetalk.encoding.api.ContentCodec;

import java.io.InputStream;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The equivalent of {@link HttpResponse} but provides the payload as a {@link BlockingIterable}.
 */
public interface BlockingStreamingHttpResponse extends HttpResponseMetaData {
    /**
     * Gets the underlying payload as a {@link Publisher} of {@link Buffer}s.
     * @return The {@link Publisher} of {@link Buffer} representation of the underlying payload body.
     */
    BlockingIterable<Buffer> payloadBody();

    /**
     * Gets the underlying payload as a {@link InputStream}.
     * @return The {@link InputStream} representation of the underlying payload body.
     */
    default InputStream payloadBodyInputStream() {
        return new CloseableIteratorBufferAsInputStream(payloadBody().iterator());
    }

    /**
     * Gets and deserializes the payload body.
     * @param deserializer The function that deserializes the underlying {@link BlockingIterable}.
     * @param <T> The resulting type of the deserialization operation.
     * @return The results of the deserialization operation.
     * @deprecated Use {@link #payloadBody(HttpStreamingDeserializer)}.
     */
    @Deprecated
    default <T> BlockingIterable<T> payloadBody(HttpDeserializer<T> deserializer) {
        return deserializer.deserialize(headers(), payloadBody());
    }

    /**
     * Gets and deserializes the payload body.
     * @param deserializer The function that deserializes the underlying {@link BlockingIterable}.
     * @param <T> The resulting type of the deserialization operation.
     * @return The results of the deserialization operation.
     */
    <T> BlockingIterable<T> payloadBody(HttpStreamingDeserializer<T> deserializer);

    /**
     * Get the {@link HttpMessageBodyIterable} for this response.
     * @return the {@link HttpMessageBodyIterable} for this response.
     */
    HttpMessageBodyIterable<Buffer> messageBody();

    /**
     * Get the {@link HttpMessageBodyIterable} for this response and deserialize to type {@link T}.
     * @param deserializer The function that deserializes the underlying {@link BlockingIterable}.
     * @param <T> The resulting type of the deserialization operation.
     * @return the {@link HttpMessageBodyIterable} for this payloadBody.
     */
    <T> HttpMessageBodyIterable<T> messageBody(HttpStreamingDeserializer<T> deserializer);

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload set to {@code payloadBody}.
     * <p>
     * A best effort will be made to apply back pressure to the existing payload body which is being replaced. If this
     * default policy is not sufficient you can use {@link #transformPayloadBody(UnaryOperator)} for more fine grain
     * control.
     * <p>
     * This method reserves the right to delay completion/consumption of {@code payloadBody}. This may occur due to the
     * combination with the existing payload body that is being replaced.
     * @param payloadBody The new payload body.
     * @return {@code this}
     */
    BlockingStreamingHttpResponse payloadBody(Iterable<Buffer> payloadBody);

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload set to {@code payloadBody}.
     * <p>
     * A best effort will be made to apply back pressure to the existing payload body which is being replaced. If this
     * default policy is not sufficient you can use {@link #transformPayloadBody(UnaryOperator)} for more fine grain
     * control.
     * <p>
     * This method reserves the right to delay completion/consumption of {@code payloadBody}. This may occur due to the
     * combination with the existing payload body that is being replaced.
     * @param payloadBody The new payload body.
     * @return {@code this}
     */
    BlockingStreamingHttpResponse payloadBody(InputStream payloadBody);

    /**
     * Set the {@link HttpMessageBodyIterable} for this response.
     * @param messageBody The new message body.
     * @return {@code this}.
     */
    BlockingStreamingHttpResponse messageBody(HttpMessageBodyIterable<Buffer> messageBody);

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload set to the result of serialization.
     * <p>
     * A best effort will be made to apply back pressure to the existing payload body which is being replaced. If this
     * default policy is not sufficient you can use {@link #transformPayloadBody(Function, HttpSerializer)} for more
     * fine grain control.
     * <p>
     * This method reserves the right to delay completion/consumption of {@code payloadBody}. This may occur due to the
     * combination with the existing payload body that is being replaced.
     * @param payloadBody The new payload body, prior to serialization.
     * @param serializer Used to serialize the payload body.
     * @param <T> The type of objects to serialize.
     * @return {@code this}
     * @deprecated Use {@link #payloadBody(Iterable, HttpStreamingSerializer)}.
     */
    @Deprecated
    default <T> BlockingStreamingHttpResponse payloadBody(Iterable<T> payloadBody, HttpSerializer<T> serializer) {
        throw new UnsupportedOperationException("BlockingStreamingHttpResponse#payloadBody(Iterable, HttpSerializer) " +
                "is not supported by " + getClass() + ". This method is deprecated, consider migrating to " +
                "BlockingStreamingHttpResponse#payloadBody(Iterable, HttpStreamingSerializer) or implement this " +
                "method if it's required temporarily.");
    }

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload set to the result of serialization.
     * <p>
     * A best effort will be made to apply back pressure to the existing payload body which is being replaced. If this
     * default policy is not sufficient {@link #payloadBody()} can be used to drain with more fine grain control.
     * <p>
     * This method reserves the right to delay completion/consumption of {@code payloadBody}. This may occur due to the
     * combination with the existing payload body that is being replaced.
     * @param payloadBody The new payload body, prior to serialization.
     * @param serializer Used to serialize the payload body.
     * @param <T> The type of objects to serialize.
     * @return {@code this}
     */
    <T> BlockingStreamingHttpResponse payloadBody(Iterable<T> payloadBody, HttpStreamingSerializer<T> serializer);

    /**
     * Set the {@link HttpMessageBodyIterable} for this response.
     * @param messageBody The serialized message body.
     * @param serializer The function that serializes the underlying {@link BlockingIterable}.
     * @param <T> The type of the serialized objects.
     * @return {@code this}
     */
    <T> BlockingStreamingHttpResponse messageBody(HttpMessageBodyIterable<T> messageBody,
                                                  HttpStreamingSerializer<T> serializer);

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload transformed to the result of
     * serialization.
     * @param transformer A {@link Function} which take as a parameter the existing payload body
     * {@link BlockingIterable} and returns the new payload body {@link BlockingIterable} prior to serialization. It is
     * assumed the existing payload body {@link BlockingIterable} will be transformed/consumed or else no more responses
     * may be processed.
     * @param serializer Used to serialize the payload body.
     * @param <T> The type of objects to serialize.
     * @return {@code this}
     * @deprecated Use {@link #payloadBody(HttpStreamingDeserializer)} and
     * {@link #payloadBody(Iterable, HttpStreamingSerializer)}.
     */
    @Deprecated
    default <T> BlockingStreamingHttpResponse transformPayloadBody(
            Function<BlockingIterable<Buffer>, BlockingIterable<T>> transformer, HttpSerializer<T> serializer) {
        throw new UnsupportedOperationException(
                "BlockingStreamingHttpResponse#transformPayloadBody(Function, HttpSerializer) is not supported by " +
                        getClass() + ". This method is deprecated, consider migrating to alternative methods or " +
                        "implement this method if it's required temporarily.");
    }

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload transformed to the result of
     * serialization.
     * @param transformer A {@link Function} which take as a parameter the existing payload body
     * {@link BlockingIterable} and returns the new payload body {@link BlockingIterable} prior to serialization. It is
     * assumed the existing payload body {@link BlockingIterable} will be transformed/consumed or else no more requests
     * may be processed.
     * @param deserializer Used to deserialize the existing payload body.
     * @param serializer Used to serialize the payload body.
     * @param <T> The type of objects to deserialize.
     * @param <R> The type of objects to serialize.
     * @return {@code this}
     * @deprecated Use {@link #payloadBody(HttpStreamingDeserializer)} and
     * {@link #payloadBody(Iterable, HttpStreamingSerializer)}.
     */
    @Deprecated
    default <T, R> BlockingStreamingHttpResponse transformPayloadBody(
            Function<BlockingIterable<T>, BlockingIterable<R>> transformer, HttpDeserializer<T> deserializer,
            HttpSerializer<R> serializer) {
        return transformPayloadBody(buffers -> transformer.apply(payloadBody(deserializer)), serializer);
    }

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload transformed to {@link Buffer}s.
     * @param transformer A {@link Function} which take as a parameter the existing payload body
     * {@link BlockingIterable} and returns the new payload body {@link BlockingIterable}. It is assumed the existing
     * payload body {@link BlockingIterable} will be transformed/consumed or else no more responses may be processed.
     * @return {@code this}
     * @deprecated Use {@link #payloadBody()} and {@link #payloadBody(Iterable)}.
     */
    @Deprecated
    default BlockingStreamingHttpResponse transformPayloadBody(UnaryOperator<BlockingIterable<Buffer>> transformer) {
        throw new UnsupportedOperationException(
                "BlockingStreamingHttpResponse#transformPayloadBody(UnaryOperator) is not supported by " + getClass() +
                        ". This method is deprecated, consider migrating to alternative methods or implement this " +
                        "method if it's required temporarily.");
    }

    /**
     * Returns a {@link BlockingStreamingHttpResponse} with its underlying payload transformed to {@link Buffer}s,
     * with access to the <a href="https://tools.ietf.org/html/rfc7230#section-4.1.2">trailer</a>s.
     * @param trailersTransformer {@link TrailersTransformer} to use for this transform.
     * @param <T> The type of state used during the transformation.
     * @return {@code this}
     * @deprecated Use {@link #messageBody()} and {@link #messageBody(HttpMessageBodyIterable)}.
     */
    @Deprecated
    default <T> BlockingStreamingHttpResponse transform(TrailersTransformer<T, Buffer> trailersTransformer) {
        throw new UnsupportedOperationException(
                "BlockingStreamingHttpResponse#transform(TrailersTransformer) is not supported by " + getClass() +
                        ". This method is deprecated, consider migrating to alternative methods or implement this " +
                        "method if it's required temporarily.");
    }

    /**
     * Translates this {@link BlockingStreamingHttpResponse} to a {@link HttpResponse}.
     * @return a {@link Single} that completes with a {@link HttpResponse} representation of this
     * {@link BlockingStreamingHttpResponse}.
     */
    Single<HttpResponse> toResponse();

    /**
     * Translates this {@link BlockingStreamingHttpResponse} to a {@link StreamingHttpResponse}.
     * @return a {@link StreamingHttpResponse} representation of this {@link BlockingStreamingHttpResponse}.
     */
    StreamingHttpResponse toStreamingResponse();

    @Override
    BlockingStreamingHttpResponse version(HttpProtocolVersion version);

    @Override
    BlockingStreamingHttpResponse status(HttpResponseStatus status);

    @Deprecated
    @Override
    default BlockingStreamingHttpResponse encoding(ContentCodec encoding) {
        throw new UnsupportedOperationException("BlockingStreamingHttpResponse#encoding(ContentCodec) is not " +
                "supported by " + getClass() + ". This method is deprecated, consider migrating to provided " +
                "alternatives or implement this method if it's required temporarily.");
    }

    @Override
    default BlockingStreamingHttpResponse addHeader(final CharSequence name, final CharSequence value) {
        HttpResponseMetaData.super.addHeader(name, value);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse addHeaders(final HttpHeaders headers) {
        HttpResponseMetaData.super.addHeaders(headers);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse setHeader(final CharSequence name, final CharSequence value) {
        HttpResponseMetaData.super.setHeader(name, value);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse setHeaders(final HttpHeaders headers) {
        HttpResponseMetaData.super.setHeaders(headers);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse addCookie(final HttpCookiePair cookie) {
        HttpResponseMetaData.super.addCookie(cookie);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse addCookie(final CharSequence name, final CharSequence value) {
        HttpResponseMetaData.super.addCookie(name, value);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse addSetCookie(final HttpSetCookie cookie) {
        HttpResponseMetaData.super.addSetCookie(cookie);
        return this;
    }

    @Override
    default BlockingStreamingHttpResponse addSetCookie(final CharSequence name, final CharSequence value) {
        HttpResponseMetaData.super.addSetCookie(name, value);
        return this;
    }

    @Override
    BlockingStreamingHttpResponse context(ContextMap context);
}
